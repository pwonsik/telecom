package me.realimpact.telecom.calculation.infrastructure.adapter;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import me.realimpact.telecom.calculation.domain.monthlyfee.DefaultPeriod;
import me.realimpact.telecom.calculation.domain.monthlyfee.MonthlyFeeCalculationResult;
import me.realimpact.telecom.calculation.infrastructure.converter.DomainToDtoConverter;
import me.realimpact.telecom.calculation.infrastructure.dto.CalculationResultDto;
import me.realimpact.telecom.calculation.port.out.CalculationResultSavePort;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;

/**
 * 계산 결과 저장을 위한 Repository 구현체
 * 헥사고날 아키텍처의 outbound adapter
 */
@Repository
@RequiredArgsConstructor
@Slf4j
public class CalculationResultRepository implements CalculationResultSavePort {

    private final CalculationResultMapper calculationResultMapper;
    private final DomainToDtoConverter domainToDtoConverter;

    /**
     * 대용량 배치 저장 (Batch Insert)
     * 성능을 위해 여러 개의 결과를 한번에 저장
     */
    @Override
    @Transactional
    public void batchSaveCalculationResults(List<MonthlyFeeCalculationResult> results, DefaultPeriod billingPeriod) {
        if (results == null || results.isEmpty()) {
            log.warn("No calculation results to save");
            return;
        }

        // 대용량 처리를 위해 청크 단위로 나누어 처리
        final int chunkSize = 1000; // 1000개씩 나누어 처리
        
        List<CalculationResultDto> dtos = domainToDtoConverter.convertToCalculationResultDtos(results, billingPeriod.getStartDate(), billingPeriod.getEndDate());
        
        int totalInserted = 0;
        int chunkCount = (dtos.size() + chunkSize - 1) / chunkSize; // 올림 계산
        
        for (int i = 0; i < chunkCount; i++) {
            int start = i * chunkSize;
            int end = Math.min(start + chunkSize, dtos.size());
            
            List<CalculationResultDto> chunk = dtos.subList(start, end);
            
            try {
                int insertCount = calculationResultMapper.batchInsertCalculationResults(chunk);
                totalInserted += insertCount;
                
                log.debug("Batch insert chunk {}/{}: {} records inserted", i + 1, chunkCount, insertCount);
                
                if (insertCount != chunk.size()) {
                    log.warn("Expected to insert {} records in chunk {}, but inserted {} records", 
                            chunk.size(), i + 1, insertCount);
                }
            } catch (Exception e) {
                log.error("Failed to insert chunk {} with {} records", i + 1, chunk.size(), e);
                throw new RuntimeException("Batch insert failed at chunk " + (i + 1), e);
            }
        }
        
        log.info("Successfully batch inserted {} calculation results (expected: {})", totalInserted, results.size());
        
        if (totalInserted != results.size()) {
            throw new RuntimeException("Batch insert incomplete. Expected: " + results.size() + ", Actual: " + totalInserted);
        }
    }

}