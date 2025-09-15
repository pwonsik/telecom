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
     * 생성자에서 DataLoader List를 Map으로 변환
     */
    public ChunkedContractReader(
            CalculationCommandService calculationCommandService,
            SqlSessionFactory sqlSessionFactory,
            CalculationParameters calculationParameters) {
        
        this.calculationCommandService = calculationCommandService;
        this.sqlSessionFactory = sqlSessionFactory;
        this.calculationParameters = calculationParameters;
    }

    
    @Override
    public void open(ExecutionContext executionContext) throws ItemStreamException {
        if (!initialized) {
            initializeContractIdReader();
            contractIdReader.open(executionContext);
            initialized = true;
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
    
    private void initializeContractIdReader() {
        log.info("=== ChunkedContractReader 초기화 ===");
        log.info("chunkSize: {}", chunkSize);

        contractIdReader = new MyBatisCursorItemReader<>();
        contractIdReader.setSqlSessionFactory(sqlSessionFactory);
        contractIdReader.setQueryId("me.realimpact.telecom.calculation.infrastructure.adapter.mybatis.ContractQueryMapper.findContractIds");
        contractIdReader.setParameterValues(getParameterValues());
    }

    private Map<String, Object> getParameterValues() {
        Map<String, Object> parameterValues = new HashMap<>();
        parameterValues.put("billingStartDate", calculationParameters.getBillingStartDate());
        parameterValues.put("billingEndDate", calculationParameters.getBillingEndDate());
        parameterValues.put("contractIds", calculationParameters.getContractIds());
        return parameterValues;
    }

    private void loadNextChunk() throws Exception {
        //log.debug("=== ChunkedContractReader loadNextChunk ===");
        
        List<Long> contractIds = new ArrayList<>();
        
        // chunk size만큼 contract ID를 읽어오기
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