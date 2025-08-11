package me.realimpact.telecom.calculation.infrastructure.converter;

import me.realimpact.telecom.calculation.domain.monthlyfee.AdditionalBillingFactor;
import me.realimpact.telecom.calculation.domain.monthlyfee.MonthlyFeeCalculationResult;
import me.realimpact.telecom.calculation.infrastructure.dto.CalculationResultDto;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 도메인 객체를 DTO로 변환하는 컨버터
 */
@Component
public class DomainToDtoConverter {


    /**
     * MonthlyFeeCalculationResult를 CalculationResultDto로 변환
     */
    public List<CalculationResultDto> convertToCalculationResultDto(
            MonthlyFeeCalculationResult result) {
        return result.items().stream()
            .map(i -> CalculationResultDto.builder()
                .contractId(result.contractId())
                .productOfferingId(i.productOfferingId())
                .chargeItemId(i.monthlyChargeItemId())
                .billingStartDate(result.billingStartDate())
                .billingEndDate(result.billingEndDate())
                .calculatedFee(i.fee())
                .periodStartDate(i.effectiveStartDate())
                .periodEndDate(i.effectiveEndDate())
                .build()
            )
            .toList();
    }

    /**
     * 여러 MonthlyFeeCalculationResult를 CalculationResultDto 리스트로 변환
     */
    public List<CalculationResultDto> convertToCalculationResultDtos(
            List<MonthlyFeeCalculationResult> results) {
        
        return results.stream()
            .flatMap(result -> convertToCalculationResultDto(result).stream())
            .toList();
    }

}