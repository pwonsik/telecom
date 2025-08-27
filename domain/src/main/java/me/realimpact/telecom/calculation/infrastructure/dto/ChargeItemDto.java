package me.realimpact.telecom.calculation.infrastructure.dto;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;

@Getter
@Setter
@NoArgsConstructor
public class ChargeItemDto {
    private String productOfferingId;
    private String chargeItemId;
    private String chargeItemName;
    private String revenueItemId;
    private BigDecimal suspensionChargeRatio;
    private String calculationMethodCode;
    private String calculationMethodName;
    
    // Pricing 관련 필드들 (strategy pattern을 위해 추가)
    private BigDecimal flatRateAmount;
    private String pricingType;
}