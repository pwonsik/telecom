package me.realimpact.telecom.calculation.infrastructure.dto;

import java.math.BigDecimal;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class MonthlyChargeItemDto {
    private String productOfferingId;
    private String chargeItemId;
    private String chargeItemName;
    private BigDecimal suspensionChargeRatio;
    private String calculationMethodCode;
    private String calculationMethodName;
    
    // Pricing 관련 필드들 (strategy pattern을 위해 추가)
    private BigDecimal flatRateAmount;
    private String pricingType;
}