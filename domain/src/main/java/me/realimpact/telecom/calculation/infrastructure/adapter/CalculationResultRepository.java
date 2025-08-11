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
    public void batchSaveCalculationResults(List<MonthlyFeeCalculationResult> results) {
        if (results == null || results.isEmpty()) {
            log.warn("No calculation results to save");
            return;
        }
        List<CalculationResultDto> dtos = domainToDtoConverter.convertToCalculationResultDtos(results);
        calculationResultMapper.batchInsertCalculationResults(dtos);
    }

}