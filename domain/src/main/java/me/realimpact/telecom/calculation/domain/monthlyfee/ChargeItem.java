package me.realimpact.telecom.calculation.domain.monthlyfee;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

import java.math.BigDecimal;
import java.util.List;

@RequiredArgsConstructor
@Getter
public class ChargeItem {
    private final String chargeItemId;
    private final String chargeItemName;
    private final String revenueItemId;
    private final BigDecimal suspensionChargeRatio;
    private final CalculationMethod calculationMethod;
    private final Pricing pricing;

    public BigDecimal getPrice(List<AdditionalBillingFactor> additionalBillingFactors) {
        return pricing.getPrice(additionalBillingFactors);
    }

    @Override
    public String toString() {
        return "ChargeItem{" +
                "chargeItemId='" + chargeItemId + '\'' +
                ", chargeItemName='" + chargeItemName + '\'' +
                ", revenueItemId='" + revenueItemId + '\'' +
                ", suspensionChargeRatio=" + suspensionChargeRatio +
                ", calculationMethod=" + calculationMethod +
                '}';
    }
}