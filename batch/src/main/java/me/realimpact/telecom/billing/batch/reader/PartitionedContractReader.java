package me.realimpact.telecom.billing.batch.reader;

import lombok.extern.slf4j.Slf4j;
import me.realimpact.telecom.billing.batch.CalculationParameters;
import me.realimpact.telecom.calculation.application.CalculationCommandService;
import me.realimpact.telecom.calculation.application.CalculationTarget;
import me.realimpact.telecom.calculation.domain.CalculationContext;
import org.apache.ibatis.session.SqlSessionFactory;
import org.mybatis.spring.batch.MyBatisCursorItemReader;
import org.springframework.batch.core.configuration.annotation.StepScope;
import org.springframework.batch.item.ExecutionContext;
import org.springframework.batch.item.ItemStreamException;
import org.springframework.batch.item.ItemStreamReader;
import org.springframework.batch.item.support.ListItemReader;

import java.util.*;

import static me.realimpact.telecom.billing.batch.config.BatchConstants.CHUNK_SIZE;

/**
 * 파티션 기반 계약 Reader
 * 파티션별로 독립적인 MyBatisCursorItemReader 인스턴스 생성
 */
@Slf4j
public class PartitionedContractReader implements ItemStreamReader<CalculationTarget> {

    private final CalculationCommandService calculationCommandService;
    private final SqlSessionFactory sqlSessionFactory;
    private final CalculationParameters calculationParameters;
    private final Integer partitionKey;
    private final Integer partitionCount;

    private static final int chunkSize = CHUNK_SIZE;

    private MyBatisCursorItemReader<Long> contractIdReader;
    private ListItemReader<CalculationTarget> currentChunkReader;
    private boolean initialized = false;

    public PartitionedContractReader(
            CalculationCommandService calculationCommandService,
            SqlSessionFactory sqlSessionFactory,
            CalculationParameters calculationParameters,
            Integer partitionKey,
            Integer partitionCount) {
        this.calculationCommandService = calculationCommandService;
        this.sqlSessionFactory = sqlSessionFactory;
        this.calculationParameters = calculationParameters;
        this.partitionKey = partitionKey;
        this.partitionCount = partitionCount;
    }

    @Override
    public void open(ExecutionContext executionContext) throws ItemStreamException {
        if (!initialized) {
            log.info("=== 파티션별 Reader 초기화 (파티션 {}) ===", partitionKey);
            log.info("Partition Key: {}, Partition Count: {}", partitionKey, partitionCount);

            initializePartitionedContractIdReader();
            contractIdReader.open(executionContext);
            initialized = true;
        }
    }

    @Override
    public CalculationTarget read() throws Exception {
        // 현재 청크에서 아이템을 읽기 시도
        if (currentChunkReader != null) {
            CalculationTarget item = currentChunkReader.read();
            if (item != null) {
                return item;
            }
        }
        
        // 현재 청크가 끝났으면 다음 청크 로드
        loadNextChunk();
        if (currentChunkReader == null) {
            return null; // 더 이상 읽을 데이터가 없음
        }
        
        // 새로운 청크에서 첫 번째 아이템 반환
        return currentChunkReader.read();
    }

    @Override
    public void update(ExecutionContext executionContext) throws ItemStreamException {
        if (contractIdReader != null) {
            contractIdReader.update(executionContext);
        }
    }

    @Override
    public void close() throws ItemStreamException {
        if (contractIdReader != null) {
            contractIdReader.close();
        }
    }


    /**
     * 파티션 조건이 적용된 Contract ID Reader 초기화
     */
    private void initializePartitionedContractIdReader() {
        contractIdReader = new MyBatisCursorItemReader<>();
        contractIdReader.setSqlSessionFactory(sqlSessionFactory);

        // 파티션 조건이 포함된 쿼리 사용
        if (calculationParameters.getContractIds().isEmpty()) {
            // 전체 계약 대상 (파티션 조건 적용)
            contractIdReader.setQueryId("me.realimpact.telecom.calculation.infrastructure.adapter.mybatis.ContractQueryMapper.findContractIdsWithPartition");

            Map<String, Object> parameterValues = new HashMap<>();
            parameterValues.put("partitionKey", partitionKey);
            parameterValues.put("partitionCount", partitionCount);
            parameterValues.put("billingStartDate", calculationParameters.getBillingStartDate());
            parameterValues.put("billingEndDate", calculationParameters.getBillingEndDate());
            contractIdReader.setParameterValues(parameterValues);

            log.info("전체 계약 조회 (파티션 조건 적용): contractId % {} = {}", partitionCount, partitionKey);
        } else {
            // 특정 계약 대상 (파티션 조건 적용)
            List<Long> filteredContractIds = calculationParameters.getContractIds().stream()
                    .filter(contractId -> contractId % partitionCount == partitionKey)
                    .toList();

            if (filteredContractIds.isEmpty()) {
                log.info("파티션 {}에 해당하는 계약이 없습니다.", partitionKey);
                // 빈 결과를 반환하도록 설정
                contractIdReader.setQueryId("me.realimpact.telecom.calculation.infrastructure.adapter.mybatis.ContractQueryMapper.findEmptyContractIds");
                contractIdReader.setParameterValues(new HashMap<>());
            } else {
                contractIdReader.setQueryId("me.realimpact.telecom.calculation.infrastructure.adapter.mybatis.ContractQueryMapper.findSpecificContractIds");

                Map<String, Object> parameterValues = new HashMap<>();
                parameterValues.put("contractIds", filteredContractIds);
                contractIdReader.setParameterValues(parameterValues);

                log.info("특정 계약 조회 (파티션 필터링 적용): {} 건", filteredContractIds.size());
            }
        }
    }

    /**
     * 다음 청크 로드 (ChunkedContractReader 로직과 동일)
     */
    private void loadNextChunk() throws Exception {
        
        List<Long> contractIds = new ArrayList<>();
        
        // chunkSize만큼 Contract ID 수집
        for (int i = 0; i < chunkSize; i++) {
            Long contractId = contractIdReader.read();
            if (contractId == null) {
                break; // 더 이상 읽을 데이터가 없음
            }
            contractIds.add(contractId);
        }
        
        if (contractIds.isEmpty()) {
            currentChunkReader = null;
            return;
        }

        // ListItemReader로 감싸서 하나씩 반환할 수 있도록 설정
        currentChunkReader = new ListItemReader<>(getCalculationTargets(contractIds));
    }

    private List<CalculationTarget> getCalculationTargets(List<Long> contractIds) {
        CalculationContext ctx = calculationParameters.toCalculationContext();
        return calculationCommandService.loadCalculationTargets(contractIds, ctx);
    }
}