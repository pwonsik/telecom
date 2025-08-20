package me.realimpact.telecom.calculation.infrastructure.converter;

import me.realimpact.telecom.calculation.application.onetimecharge.OneTimeChargeCalculationResult;
import me.realimpact.telecom.calculation.application.onetimecharge.OneTimeChargeCalculationResultItem;
import me.realimpact.telecom.calculation.domain.monthlyfee.MonthlyFeeCalculationResult;
import me.realimpact.telecom.calculation.domain.monthlyfee.MonthlyFeeCalculationResultItem;
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

    /**
     * OneTimeChargeCalculationResult 목록을 평면화된 DTO 목록으로 변환
     * @param results 변환할 일회성 과금 계산 결과 목록
     * @return 평면화된 DTO 목록
     */
    public List<FlatCalculationResultDto> flattenOneTimeChargeResults(List<OneTimeChargeCalculationResult> results) {
        List<FlatCalculationResultDto> flatDtos = new ArrayList<>();
        
        for (OneTimeChargeCalculationResult result : results) {
            // 각 ResultItem을 개별 DTO로 변환
            for (OneTimeChargeCalculationResultItem item : result.items()) {
                FlatCalculationResultDto flatDto = new FlatCalculationResultDto();
                
                // Contract 정보
                flatDto.setContractId(result.contractId());
                flatDto.setBillingStartDate(result.billingStartDate());
                flatDto.setBillingEndDate(result.billingEndDate());
                
                // OneTimeCharge Item 정보
                // productOfferingId는 null (일회성 과금은 상품과 직접 연관되지 않음)
                flatDto.setProductOfferingId(null);
                // chargeItemCode를 monthlyChargeItemId에 저장 (기존 스키마 활용)
                flatDto.setMonthlyChargeItemId(item.chargeItemCode());
                flatDto.setEffectiveStartDate(result.billingStartDate());
                flatDto.setEffectiveEndDate(result.billingEndDate());
                flatDto.setSuspensionType(null); // 일회성 과금은 정지 타입 없음
                flatDto.setFee(item.fee());
                
                flatDtos.add(flatDto);
            }
        }
        
        return flatDtos;
    }
}