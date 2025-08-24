package me.realimpact.telecom.calculation.infrastructure.dto;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * CalculationResult 테이블과 매핑되는 DTO
 */
@Getter
@Setter
@Builder
public class CalculationResultDto {
    
    // Primary Key
    private Long calculationResultId;
    
    // Contract 정보
    private Long contractId;
    
    // Product 정보
    private Long productContractId;
    private String productOfferingId;
    
    // MonthlyChargeItem 정보
    private String chargeItemId;
    
    // Suspension 정보 (Optional)
    private String suspensionTypeCode;
    
    // Period 정보
    private LocalDate periodStartDate;
    private LocalDate periodEndDate;
    private Integer usageDays;
    private Integer daysOfMonth;
    
    // 계산 결과
    private BigDecimal calculatedFee;

    // 메타데이터
    private LocalDate billingStartDate;
    private LocalDate billingEndDate;
}