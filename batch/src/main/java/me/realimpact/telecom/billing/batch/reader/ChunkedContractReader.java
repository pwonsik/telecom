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
 * 청크 기반으로 계약 데이터를 읽어오는 ItemStreamReader 구현체.
 * 계약 ID를 청크 단위로 읽어와 CalculationTarget 객체를 생성한다.
 */
@Slf4j
public class ChunkedContractReader implements ItemStreamReader<CalculationTarget> {

    //-- 인터페이스 주입이 아닌 구현체 주입.. 배치는 read, process, write를 나눠서 호출해야 해서 어쩔 수 없이.
    private final CalculationCommandService calculationCommandService;

    private final SqlSessionFactory sqlSessionFactory;
    private final CalculationParameters calculationParameters;

    private static final int chunkSize = CHUNK_SIZE;

    private MyBatisCursorItemReader<Long> contractIdReader;
    private ListItemReader<CalculationTarget> currentChunkReader;
    private boolean initialized = false;

    /**
     * 생성자
     */
    public ChunkedContractReader(
            CalculationCommandService calculationCommandService,
            SqlSessionFactory sqlSessionFactory,
            CalculationParameters calculationParameters) {

        this.calculationCommandService = calculationCommandService;
        this.sqlSessionFactory = sqlSessionFactory;
        this.calculationParameters = calculationParameters;

        log.info("=== ChunkedContractReader 생성 ===");
    }

    @Override
    public void open(ExecutionContext executionContext) throws ItemStreamException {
        if (!initialized) {
            log.info("=== ChunkedContractReader open() 시작 ===");
            initializeContractIdReader(executionContext);
            initialized = true;
            log.info("=== ChunkedContractReader open() 완료 ===");
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
            log.info("=== MyBatisCursorItemReader close() 완료 ===");
        }
    }
    
    @Override
    public CalculationTarget read() throws Exception {
        log.debug("=== ChunkedContractReader.read() 호출 ===");

        // contractIdReader null 체크 (방어적 프로그래밍)
        if (contractIdReader == null) {
            log.error("contractIdReader가 null입니다. 초기화에 실패했습니다");
            return null;
        }

        // 현재 청크에서 아이템을 읽기 시도
        if (currentChunkReader != null) {
            CalculationTarget item = currentChunkReader.read();
            if (item != null) {
                log.debug("현재 청크에서 아이템 반환");
                return item;
            } else {
                log.info("현재 청크 완료 - 다음 청크 로드 시도");
            }
        } else {
            log.info("currentChunkReader가 null - 첫 번째 청크 로드 시도");
        }

        // 현재 청크가 끝났으면 다음 청크 로드
        loadNextChunk();
        if (currentChunkReader == null) {
            log.info("loadNextChunk() 결과: currentChunkReader가 null - 더 이상 읽을 데이터 없음");
            return null; // 더 이상 읽을 데이터가 없음
        }

        // 새로운 청크에서 첫 번째 아이템 반환
        CalculationTarget item = currentChunkReader.read();
        if (item != null) {
            log.info("새 청크에서 첫 번째 아이템 반환");
        } else {
            log.warn("새 청크가 생성되었지만 첫 번째 아이템이 null");
        }
        return item;
    }
    
    private void initializeContractIdReader(ExecutionContext executionContext) {
        log.info("=== ChunkedContractReader 초기화 ===");
        log.info("chunkSize: {}", chunkSize);

        contractIdReader = new MyBatisCursorItemReader<>();
        contractIdReader.setSqlSessionFactory(sqlSessionFactory);
        contractIdReader.setQueryId("me.realimpact.telecom.calculation.infrastructure.adapter.mybatis.ContractQueryMapper.findContractIds");
        contractIdReader.setParameterValues(getParameterValues());
        contractIdReader.open(executionContext);    // ItemStreamReader 기반이므로 반드시 호출해야함
    }

    private Map<String, Object> getParameterValues() {
        Map<String, Object> parameterValues = new HashMap<>();
        parameterValues.put("billingStartDate", calculationParameters.getBillingStartDate());
        parameterValues.put("billingEndDate", calculationParameters.getBillingEndDate());
        parameterValues.put("contractIds", calculationParameters.getContractIds());
        return parameterValues;
    }

    private void loadNextChunk() throws Exception {
        log.info("=== ChunkedContractReader loadNextChunk 시작 ===");

        List<Long> contractIds = new ArrayList<>();

        // chunk size만큼 contract ID를 읽어오기
        for (int i = 0; i < chunkSize; i++) {
            Long contractId = contractIdReader.read();
            log.debug("contractIdReader.read() 결과 [{}]: {}", i, contractId);

            if (contractId == null) {
                log.info("contractIdReader.read()가 null 반환 - 더 이상 읽을 데이터 없음 (읽은 개수: {})", i);
                break; // 더 이상 읽을 데이터가 없음
            }
            contractIds.add(contractId);
        }

        log.info("loadNextChunk에서 수집된 Contract IDs 개수: {}", contractIds.size());

        if (contractIds.isEmpty()) {
            log.info("수집된 Contract IDs가 없음 - currentChunkReader를 null로 설정");
            currentChunkReader = null;
            return;
        }

        // CalculationTarget 생성
        List<CalculationTarget> calculationTargets = getCalculationTargets(contractIds);
        log.info("생성된 CalculationTarget 개수: {}", calculationTargets.size());

        // ListItemReader로 감싸서 하나씩 반환할 수 있도록 설정
        currentChunkReader = new ListItemReader<>(calculationTargets);
        log.info("=== ChunkedContractReader loadNextChunk 완료 ===");
    }

    private List<CalculationTarget> getCalculationTargets(List<Long> contractIds) {
        CalculationContext ctx = calculationParameters.toCalculationContext();
        return calculationCommandService.loadCalculationTargets(contractIds, ctx);
    }
}