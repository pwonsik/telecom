package me.realimpact.telecom.calculation.infrastructure.adapter;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import me.realimpact.telecom.calculation.domain.monthlyfee.MonthlyFeeCalculationResult;
import me.realimpact.telecom.calculation.infrastructure.converter.CalculationResultFlattener;
import me.realimpact.telecom.calculation.infrastructure.dto.FlatCalculationResultDto;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * 계산 결과 저장을 위한 Repository 구현체
 * 헥사고날 아키텍처의 outbound adapter
 */
@Repository
@RequiredArgsConstructor
@Slf4j
public class CalculationResultRepository  {

    private final CalculationResultMapper calculationResultMapper;
    private final CalculationResultFlattener calculationResultFlattener;

    /**
     * 대용량 배치 저장
     * MonthlyFeeCalculationResult를 평면화된 단일 테이블 구조로 저장
     */
    @Transactional
    public void batchSaveCalculationResults(List<MonthlyFeeCalculationResult> results) {
        if (results == null || results.isEmpty()) {
            log.warn("No calculation results to save");
            return;
        }

        log.info("Starting batch save for {} calculation results", results.size());

        try {
            // MonthlyFeeCalculationResult를 평면화된 DTO로 변환
            List<FlatCalculationResultDto> flatResults = calculationResultFlattener.flattenResults(results);
            
            log.info("Flattened {} results into {} records for batch insert", 
                    results.size(), flatResults.size());

            // 단일 배치 Insert 실행
            int insertedRows = calculationResultMapper.batchInsertCalculationResults(flatResults);
            
            log.info("Successfully inserted {} records", insertedRows);
            
        } catch (Exception e) {
            log.error("Failed to batch save calculation results", e);
            throw e;
        }
    }

}