package me.realimpact.telecom.billing.batch.reader;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import me.realimpact.telecom.billing.batch.CalculationParameters;
import me.realimpact.telecom.calculation.infrastructure.adapter.mybatis.ContractQueryMapper;
import me.realimpact.telecom.calculation.infrastructure.adapter.mybatis.DeviceInstallmentMapper;
import me.realimpact.telecom.calculation.infrastructure.adapter.mybatis.InstallationHistoryMapper;
import me.realimpact.telecom.calculation.infrastructure.dto.ContractDto;
import org.apache.ibatis.session.SqlSessionFactory;
import org.mybatis.spring.batch.MyBatisCursorItemReader;
import org.springframework.batch.core.configuration.annotation.StepScope;
import org.springframework.batch.item.ExecutionContext;
import org.springframework.batch.item.ItemStreamException;
import org.springframework.batch.item.ItemStreamReader;
import org.springframework.batch.item.support.ListItemReader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static me.realimpact.telecom.billing.batch.config.BatchConstants.CHUNK_SIZE;

@StepScope
@RequiredArgsConstructor
@Slf4j
public class ChunkedContractReader implements ItemStreamReader<ContractDto> {
    
    private final ContractQueryMapper contractQueryMapper;
    private final InstallationHistoryMapper installationHistoryMapper;
    private final DeviceInstallmentMapper deviceInstallmentMapper;
    private final SqlSessionFactory sqlSessionFactory;
    private final CalculationParameters calculationParameters;
    
    private static final int chunkSize = CHUNK_SIZE;
    
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
        // 현재 청크에서 아이템을 읽기 시도
        if (currentChunkReader != null) {
            ContractDto item = currentChunkReader.read();
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
        parameterValues.put("contractId", calculationParameters.contractIds());
        parameterValues.put("billingStartDate", calculationParameters.billingStartDate());
        parameterValues.put("billingEndDate", calculationParameters.billingEndDate());
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
        
        //log.info("읽어온 contractIds: {}", contractIds.size() <= 10 ? contractIds : contractIds.subList(0, 10) + "...");
        
        // contract ID들로 bulk 조회하여 ContractDto 리스트 생성
        List<ContractDto> contractDtos =
            contractQueryMapper.findContractsAndProductInventoriesByContractIds(
                contractIds, calculationParameters.billingStartDate(), calculationParameters.billingEndDate());

        contractDtos.forEach(contractDto -> {
            contractDto.setInstallationHistories(
                installationHistoryMapper.findInstallationsByContractIds(
                    contractIds, calculationParameters.billingEndDate()
                )
            );
            contractDto.setDeviceInstallments(
                deviceInstallmentMapper.findInstallmentsByContractIds(
                    contractIds, calculationParameters.billingEndDate()
                )
            );
        });

        // todo - 여기에 각종 요금항목을 계산하기 위한 기초 데이터를 load하는 로직 넣는다.
        
        log.info("생성된 ContractDto 개수: {}", contractDtos.size());
        
        // ListItemReader로 감싸서 하나씩 반환할 수 있도록 설정
        currentChunkReader = new ListItemReader<>(contractDtos);
    }

}