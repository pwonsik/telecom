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
    public CalculationResultDto convertToCalculationResultDto(
            MonthlyFeeCalculationResult result,
            LocalDate billingStartDate,
            LocalDate billingEndDate) {
        
        var proratedPeriod = result.getProratedPeriod();
        var contract = proratedPeriod.getContract();
        var product = proratedPeriod.getProduct();
        var productOffering = proratedPeriod.getProductOffering();
        var chargeItem = proratedPeriod.getMonthlyChargeItem();
        var suspension = proratedPeriod.getSuspension();
        
        return CalculationResultDto.builder()
                // Contract 정보
                .contractId(contract.getContractId())

                // Product 정보
                .productContractId(product.getContractId())
                .productOfferingId(productOffering.getProductOfferingId())

                // MonthlyChargeItem 정보
                .chargeItemId(chargeItem.getChargeItemId())

                // Suspension 정보 (Optional)
                .suspensionTypeCode(suspension.map(s -> s.getSuspensionType().getCode()).orElse(null))

                // Period 정보
                .periodStartDate(proratedPeriod.getStartDate())
                .periodEndDate(proratedPeriod.getEndDate())
                .usageDays((int) proratedPeriod.getUsageDays())
                .daysOfMonth((int) proratedPeriod.getDayOfMonth())
                
                // 계산 결과
                .calculatedFee(result.getFee())

                // 메타데이터
                .billingStartDate(billingStartDate)
                .billingEndDate(billingEndDate)
                .build();
    }

    /**
     * 여러 MonthlyFeeCalculationResult를 CalculationResultDto 리스트로 변환
     */
    public List<CalculationResultDto> convertToCalculationResultDtos(
            List<MonthlyFeeCalculationResult> results,
            LocalDate billingStartDate,
            LocalDate billingEndDate) {
        
        return results.stream()
                .map(result -> convertToCalculationResultDto(result, billingStartDate, billingEndDate))
                .collect(Collectors.toList());
    }

}