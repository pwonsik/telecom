package me.realimpact.telecom.billing.batch.reader;

import lombok.extern.slf4j.Slf4j;
import me.realimpact.telecom.billing.batch.CalculationParameters;
import me.realimpact.telecom.calculation.application.CalculationTarget;
import me.realimpact.telecom.calculation.application.CalculationTargetLoader;
import me.realimpact.telecom.calculation.domain.CalculationContext;
import org.apache.ibatis.session.SqlSessionFactory;
import org.mybatis.spring.batch.MyBatisCursorItemReader;
import org.springframework.batch.item.ExecutionContext;
import org.springframework.batch.item.ItemStreamException;
import org.springframework.batch.item.ItemStreamReader;
import org.springframework.batch.item.support.ListItemReader;

import java.util.*;

import static me.realimpact.telecom.billing.batch.config.BatchConstants.CHUNK_SIZE;

/**
 * 파티션 기반으로 계약 데이터를 읽어오는 ItemStreamReader 구현체.
 * 각 파티션은 독립적인 MyBatisCursorItemReader 인스턴스를 사용하여 계약 ID를 읽어온다.
 */
@Slf4j
public class PartitionedContractReader implements ItemStreamReader<CalculationTarget> {

    private final CalculationTargetLoader calculationTargetLoader;
    private final SqlSessionFactory sqlSessionFactory;
    private final CalculationParameters calculationParameters;
    private final Integer partitionKey;
    private final Integer partitionCount;

    private static final int chunkSize = CHUNK_SIZE;

    private MyBatisCursorItemReader<Long> contractIdReader;
    private ListItemReader<CalculationTarget> currentChunkReader;
    private boolean initialized = false;

    /**
     * 생성자.
     * @param calculationTargetLoader CalculationTarget을 로드하는 로더
     * @param sqlSessionFactory MyBatis 연동을 위한 SqlSessionFactory
     * @param calculationParameters 배치 계산 파라미터
     * @param partitionKey 현재 파티션의 키
     * @param partitionCount 전체 파티션 개수
     */
    public PartitionedContractReader(
            CalculationTargetLoader calculationTargetLoader,
            SqlSessionFactory sqlSessionFactory,
            CalculationParameters calculationParameters,
            Integer partitionKey,
            Integer partitionCount) {
        this.calculationTargetLoader = calculationTargetLoader;
        this.sqlSessionFactory = sqlSessionFactory;
        this.calculationParameters = calculationParameters;
        this.partitionKey = partitionKey;
        this.partitionCount = partitionCount;

        log.info("=== PartitionedContractReader 생성 (파티션 {}) ===", partitionKey);
        log.info("Partition Key: {}, Partition Count: {}", partitionKey, partitionCount);
    }

    /**
     * Reader를 열고 초기화한다.
     * @param executionContext 실행 컨텍스트
     * @throws ItemStreamException
     */
    @Override
    public void open(ExecutionContext executionContext) throws ItemStreamException {
        if (!initialized) {
            log.info("=== PartitionedContractReader open() 시작 (파티션 {}) ===", partitionKey);
            initializePartitionedContractIdReader(executionContext);
            initialized = true;
            log.info("=== PartitionedContractReader open() 완료 (파티션 {}) ===", partitionKey);
        }
    }

    /**
     * 실행 컨텍스트를 업데이트한다.
     * @param executionContext 실행 컨텍스트
     * @throws ItemStreamException
     */
    @Override
    public void update(ExecutionContext executionContext) throws ItemStreamException {
        if (contractIdReader != null) {
            contractIdReader.update(executionContext);
        }
    }

    /**
     * Reader를 닫는다.
     * @throws ItemStreamException
     */
    @Override
    public void close() throws ItemStreamException {
        if (contractIdReader != null) {
            contractIdReader.close();
            log.info("=== MyBatisCursorItemReader close() 완료 (파티션 {}) ===", partitionKey);
        }
    }

    /**
     * 다음 CalculationTarget을 읽어온다.
     * @return CalculationTarget 객체, 더 이상 읽을 데이터가 없으면 null
     * @throws Exception
     */
    @Override
    public CalculationTarget read() throws Exception {
        log.debug("=== PartitionedContractReader.read() 호출 (파티션 {}) ===", partitionKey);

        if (contractIdReader == null) {
            log.error("contractIdReader가 null입니다. 초기화에 실패했습니다 (파티션 {})", partitionKey);
            return null;
        }

        if (currentChunkReader != null) {
            CalculationTarget item = currentChunkReader.read();
            if (item != null) {
                log.debug("청크에서 아이템 반환 (파티션 {})", partitionKey);
                return item;
            }
        }

        loadNextChunk();
        if (currentChunkReader == null) {
            log.debug("더 이상 읽을 데이터 없음 (파티션 {})", partitionKey);
            return null;
        }

        CalculationTarget item = currentChunkReader.read();
        log.debug("새 청크에서 아이템 반환 (파티션 {})", partitionKey);
        return item;
    }

    /**
     * 파티션 조건에 맞는 계약 ID를 읽어오는 MyBatisCursorItemReader를 초기화한다.
     * @param executionContext 실행 컨텍스트
     */
    private void initializePartitionedContractIdReader(ExecutionContext executionContext) {
        log.info("=== MyBatisCursorItemReader 생성 시작 (파티션 {}) ===", partitionKey);

        try {
            contractIdReader = new MyBatisCursorItemReader<>();
            contractIdReader.setSqlSessionFactory(sqlSessionFactory);

            log.info("##################################### 1");

            if (calculationParameters.getContractIds().isEmpty()) {

                log.info("##################################### 2");
                // 전체 계약 대상 (파티션 조건 적용)
                contractIdReader.setQueryId("me.realimpact.telecom.calculation.infrastructure.adapter.mybatis.ContractQueryMapper.findContractIdsWithPartition");

                Map<String, Object> parameterValues = new HashMap<>();
                parameterValues.put("partitionKey", partitionKey);
                parameterValues.put("partitionCount", partitionCount);
                parameterValues.put("billingStartDate", calculationParameters.getBillingStartDate());
                parameterValues.put("billingEndDate", calculationParameters.getBillingEndDate());
                contractIdReader.setParameterValues(parameterValues);
                contractIdReader.open(executionContext);

                log.info("전체 계약 조회 (파티션 조건 적용): contractId % {} = {}", partitionCount, partitionKey);
            } else {
                log.info("##################################### 3");
                // 특정 계약 대상 (파티션 조건 적용)
                List<Long> filteredContractIds = calculationParameters.getContractIds().stream()
                        .filter(contractId -> contractId % partitionCount == partitionKey)
                        .toList();

                if (filteredContractIds.isEmpty()) {
                    log.info("##################################### 4");
                    log.info("파티션 {}에 해당하는 계약이 없습니다.", partitionKey);
                    // 빈 결과를 반환하도록 설정
                    contractIdReader.setQueryId("me.realimpact.telecom.calculation.infrastructure.adapter.mybatis.ContractQueryMapper.findEmptyContractIds");
                    contractIdReader.setParameterValues(new HashMap<>());
                } else {
                    log.info("##################################### 5");
                    contractIdReader.setQueryId("me.realimpact.telecom.calculation.infrastructure.adapter.mybatis.ContractQueryMapper.findSpecificContractIds");

                    Map<String, Object> parameterValues = new HashMap<>();
                    parameterValues.put("contractIds", filteredContractIds);
                    contractIdReader.setParameterValues(parameterValues);
                    contractIdReader.open(executionContext);

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
     * 다음 청크를 로드한다.
     * @throws Exception
     */
    private void loadNextChunk() throws Exception {
        
        List<Long> contractIds = new ArrayList<>();
        
        // chunkSize만큼 Contract ID 수집
        for (int i = 0; i < chunkSize; i++) {
            Long contractId = contractIdReader.read();
            if (contractId == null) {
                break;
            }
            contractIds.add(contractId);
        }
        
        if (contractIds.isEmpty()) {
            currentChunkReader = null;
            return;
        }

        currentChunkReader = new ListItemReader<>(getCalculationTargets(contractIds));
    }

    /**
     * 계약 ID 목록을 사용하여 CalculationTarget 목록을 가져온다.
     * @param contractIds 계약 ID 목록
     * @return CalculationTarget 목록
     */
    private List<CalculationTarget> getCalculationTargets(List<Long> contractIds) {
        CalculationContext ctx = calculationParameters.toCalculationContext();
        return calculationTargetLoader.load(contractIds, ctx);
    }
}