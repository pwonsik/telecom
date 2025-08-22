package me.realimpact.telecom.calculation.infrastructure.converter;

import me.realimpact.telecom.calculation.domain.CalculationContext;
import me.realimpact.telecom.calculation.domain.CalculationResult;
import me.realimpact.telecom.calculation.infrastructure.dto.FlatCalculationResultDto;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

/**
 * 계산 결과를 평면화된 구조로 변환하는 유틸리티
 * MonthlyFeeCalculationResult와 OneTimeChargeCalculationResult를 FlatCalculationResultDto로 변환
 */
@Component
public class CalculationResultFlattener {

    /**
     * MonthlyFeeCalculationResult 목록을 평면화된 DTO 목록으로 변환
     * @param results 변환할 계산 결과 목록
     * @return 평면화된 DTO 목록
     */
    public List<FlatCalculationResultDto> flattenResults(CalculationContext ctx, List<? extends CalculationResult> results) {
        List<FlatCalculationResultDto> flatDtos = new ArrayList<>();
        
        for (CalculationResult result : results) {
            // 각 ResultItem을 개별 DTO로 변환
            for (CalculationResult item : result.items()) {
                FlatCalculationResultDto flatDto = new FlatCalculationResultDto();
                
                // Contract 정보 (중복 저장)
                flatDto.setContractId(result.contractId());
                flatDto.setBillingStartDate(ctx.billingStartDate());
                flatDto.setBillingEndDate(ctx.billingEndDate());
                
                // Item 정보
                flatDto.setProductOfferingId(item.productOfferingId());
                flatDto.setMonthlyChargeItemId(item.chargeItemId());
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