package me.realimpact.telecom.calculation.infrastructure.adapter;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import me.realimpact.telecom.calculation.domain.CalculationContext;
import me.realimpact.telecom.calculation.domain.CalculationResult;
import me.realimpact.telecom.calculation.infrastructure.adapter.mybatis.CalculationResultMapper;
import me.realimpact.telecom.calculation.port.out.CalculationResultSavePort;
import org.springframework.stereotype.Repository;

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

    @Override
    public void save(CalculationContext ctx, List<CalculationResult<?>> results) {
        if (results == null || results.isEmpty()) {
            log.warn("No calculation results to save");
            return;
        }

        //log.info("Starting batch save for {} calculation results", results.size());

        try {
            int insertedRows = calculationResultMapper.batchInsertCalculationResults(results);
            log.info("Successfully inserted {} records", insertedRows);
            
        } catch (Exception e) {
            log.error("Failed to batch save calculation results", e);
            throw e;
        }
    }

}