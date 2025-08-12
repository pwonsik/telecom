package me.realimpact.telecom.calculation.infrastructure.converter;

import me.realimpact.telecom.calculation.domain.monthlyfee.MonthlyFeeCalculationResult;
import me.realimpact.telecom.calculation.domain.monthlyfee.MonthlyFeeCalculationResultItem;
import me.realimpact.telecom.calculation.infrastructure.dto.FlatCalculationResultDto;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

/**
 * MonthlyFeeCalculationResult를 평면화된 구조로 변환하는 유틸리티
 * 하나의 MonthlyFeeCalculationResult가 여러 개의 FlatCalculationResultDto로 변환됨
 */
@Component
public class CalculationResultFlattener {

    /**
     * MonthlyFeeCalculationResult 목록을 평면화된 DTO 목록으로 변환
     * @param results 변환할 계산 결과 목록
     * @return 평면화된 DTO 목록
     */
    public List<FlatCalculationResultDto> flattenResults(List<? extends MonthlyFeeCalculationResult> results) {
        List<FlatCalculationResultDto> flatDtos = new ArrayList<>();
        
        for (MonthlyFeeCalculationResult result : results) {
            // 각 ResultItem을 개별 DTO로 변환
            for (MonthlyFeeCalculationResultItem item : result.items()) {
                FlatCalculationResultDto flatDto = new FlatCalculationResultDto();
                
                // Contract 정보 (중복 저장)
                flatDto.setContractId(result.contractId());
                flatDto.setBillingStartDate(result.billingStartDate());
                flatDto.setBillingEndDate(result.billingEndDate());
                
                // Item 정보
                flatDto.setProductOfferingId(item.productOfferingId());
                flatDto.setMonthlyChargeItemId(item.monthlyChargeItemId());
                flatDto.setEffectiveStartDate(item.effectiveStartDate());
                flatDto.setEffectiveEndDate(item.effectiveEndDate());
                flatDto.setSuspensionType(item.suspensionType());  // Enum -> String 변환
                flatDto.setFee(item.fee());
                
                flatDtos.add(flatDto);
            }
        }
        
        return flatDtos;
    }
}