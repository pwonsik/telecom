package me.realimpact.telecom.billing.batch.reader;

import static me.realimpact.telecom.billing.batch.config.BatchConstants.CHUNK_SIZE;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.ibatis.session.SqlSessionFactory;
import org.mybatis.spring.batch.MyBatisCursorItemReader;
import org.springframework.batch.item.ExecutionContext;
import org.springframework.batch.item.ItemStreamException;
import org.springframework.batch.item.ItemStreamReader;
import org.springframework.batch.item.support.ListItemReader;

import lombok.extern.slf4j.Slf4j;
import me.realimpact.telecom.billing.batch.CalculationParameters;
import me.realimpact.telecom.calculation.application.CalculationCommandService;
import me.realimpact.telecom.calculation.application.CalculationTarget;
import me.realimpact.telecom.calculation.domain.CalculationContext;

/**
 * 파티션 기반으로 계약 데이터를 읽어오는 ItemStreamReader 구현체.
 * 각 파티션은 독립적인 MyBatisCursorItemReader 인스턴스를 사용하여 계약 ID를 읽어온다.
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

        log.info("=== PartitionedContractReader 생성 (파티션 {}) ===", partitionKey);
        log.info("Partition Key: {}, Partition Count: {}", partitionKey, partitionCount);
    }

    @Override
    public void open(ExecutionContext executionContext) throws ItemStreamException {
        if (!initialized) {
            log.info("=== PartitionedContractReader open() 시작 (파티션 {}) ===", partitionKey);
            initializePartitionedContractIdReader(executionContext);
            initialized = true;
            log.info("=== PartitionedContractReader open() 완료 (파티션 {}) ===", partitionKey);
        }
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
            log.info("=== MyBatisCursorItemReader close() 완료 (파티션 {}) ===", partitionKey);
        }
    }

    @Override
    public CalculationTarget read() throws Exception {
        log.debug("=== PartitionedContractReader.read() 호출 (파티션 {}) ===", partitionKey);

        // contractIdReader null 체크 (방어적 프로그래밍)
        if (contractIdReader == null) {
            log.error("contractIdReader가 null입니다. 초기화에 실패했습니다 (파티션 {})", partitionKey);
            return null;
        }

        // 현재 청크에서 아이템을 읽기 시도
        if (currentChunkReader != null) {
            CalculationTarget item = currentChunkReader.read();
            if (item != null) {
                log.debug("청크에서 아이템 반환 (파티션 {})", partitionKey);
                return item;
            }
        }

        // 현재 청크가 끝났으면 다음 청크 로드
        loadNextChunk();
        if (currentChunkReader == null) {
            log.debug("더 이상 읽을 데이터 없음 (파티션 {})", partitionKey);
            return null; // 더 이상 읽을 데이터가 없음
        }

        // 새로운 청크에서 첫 번째 아이템 반환
        CalculationTarget item = currentChunkReader.read();
        log.debug("새 청크에서 아이템 반환 (파티션 {})", partitionKey);
        return item;
    }

    /**
     * 파티션 조건이 적용된 Contract ID Reader 초기화
     */
    private void initializePartitionedContractIdReader(ExecutionContext executionContext) {
        log.info("=== MyBatisCursorItemReader 생성 시작 (파티션 {}) ===", partitionKey);

        try {
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
                contractIdReader.open(executionContext);    // ItemStreamReader 기반이므로 반드시 호출해야함

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
                    contractIdReader.open(executionContext);    // ItemStreamReader 기반이므로 반드시 호출해야함

                    log.info("특정 계약 조회 (파티션 필터링 적용): {} 건", filteredContractIds.size());
                }
            }

            log.info("=== MyBatisCursorItemReader 생성 완료 (파티션 {}) ===", partitionKey); 

        } catch (Exception e) {
            log.error("MyBatisCursorItemReader 초기화 실패 (파티션 {})", partitionKey, e);
            contractIdReader = null;
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