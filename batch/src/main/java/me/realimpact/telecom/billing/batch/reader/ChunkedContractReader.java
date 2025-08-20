package me.realimpact.telecom.billing.batch.reader;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import me.realimpact.telecom.calculation.infrastructure.adapter.ContractQueryMapper;
import me.realimpact.telecom.calculation.infrastructure.dto.ContractDto;
import org.apache.ibatis.session.SqlSessionFactory;
import org.mybatis.spring.batch.MyBatisCursorItemReader;
import org.springframework.batch.item.ExecutionContext;
import org.springframework.batch.item.ItemStreamException;
import org.springframework.batch.item.ItemStreamReader;
import org.springframework.batch.item.support.ListItemReader;
import org.springframework.batch.core.configuration.annotation.StepScope;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@StepScope
@RequiredArgsConstructor
@Slf4j
public class ChunkedContractReader implements ItemStreamReader<ContractDto> {
    
    private final ContractQueryMapper contractQueryMapper;
    private final SqlSessionFactory sqlSessionFactory;
    
    @Value("#{jobParameters['billingStartDate']}")
    private String billingStartDateStr;
    
    @Value("#{jobParameters['billingEndDate']}")
    private String billingEndDateStr;
    
    @Value("#{jobParameters['contractId']}")
    private String contractIdStr;
    
    @Value("#{stepExecutionContext['chunkSize'] ?: 100}")
    private int chunkSize;
    
    private MyBatisCursorItemReader<Long> contractIdReader;
    private ListItemReader<ContractDto> currentChunkReader;
    private boolean initialized = false;
    
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
    public ContractDto read() throws Exception {
        if (currentChunkReader == null || currentChunkReader.read() == null) {
            loadNextChunk();
            if (currentChunkReader == null) {
                return null;
            }
        }
        
        return currentChunkReader.read();
    }
    
    private void initializeContractIdReader() {
        log.info("=== ChunkedContractReader 초기화 ===");
        log.info("chunkSize: {}", chunkSize);
        log.info("billingStartDateStr: [{}]", billingStartDateStr);
        log.info("billingEndDateStr: [{}]", billingEndDateStr);
        log.info("contractIdStr: [{}]", contractIdStr);

        Map<String, Object> parameterValues = getParameterValues();

        contractIdReader = new MyBatisCursorItemReader<>();
        contractIdReader.setSqlSessionFactory(sqlSessionFactory);
        contractIdReader.setQueryId("me.realimpact.telecom.calculation.infrastructure.adapter.ContractQueryMapper.findContractIds");
        contractIdReader.setParameterValues(parameterValues);
    }

    private Map<String, Object> getParameterValues() {
        if (billingStartDateStr == null || billingStartDateStr.trim().isEmpty() ||
            billingEndDateStr == null || billingEndDateStr.trim().isEmpty()) {
            throw new IllegalArgumentException("billingStartDate and billingEndDate are required job parameters");
        }

        LocalDate billingStartDate = LocalDate.parse(billingStartDateStr);
        LocalDate billingEndDate = LocalDate.parse(billingEndDateStr);

        Long contractId = (contractIdStr == null || contractIdStr.trim().isEmpty())
            ? null
            : Long.parseLong(contractIdStr.trim());

        Map<String, Object> parameterValues = new HashMap<>();
        parameterValues.put("contractId", contractId);
        parameterValues.put("billingStartDate", billingStartDate);
        parameterValues.put("billingEndDate", billingEndDate);
        return parameterValues;
    }

    private void loadNextChunk() throws Exception {
        log.debug("=== ChunkedContractReader loadNextChunk ===");
        
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
        
        log.info("읽어온 contractIds: {}", contractIds.size() <= 10 ? contractIds : contractIds.subList(0, 10) + "...");
        
        // contract ID들로 bulk 조회하여 ContractDto 리스트 생성
        List<ContractDto> contractDtos = fetchContractsByIds(contractIds);
        
        log.info("생성된 ContractDto 개수: {}", contractDtos.size());
        
        // ListItemReader로 감싸서 하나씩 반환할 수 있도록 설정
        currentChunkReader = new ListItemReader<>(contractDtos);
    }
    
    private List<ContractDto> fetchContractsByIds(List<Long> contractIds) {
        LocalDate billingStartDate = LocalDate.parse(billingStartDateStr);
        LocalDate billingEndDate = LocalDate.parse(billingEndDateStr);

        return contractQueryMapper.findContractsByIds(contractIds, billingStartDate, billingEndDate);
    }
}