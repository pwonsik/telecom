package me.realimpact.telecom.calculation.domain.monthlyfee;

import java.math.BigDecimal;
import java.util.List;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
@Getter
public class MonthlyChargeItem {
    private final String chargeItemId;
    private final String chargeItemName;
    private final BigDecimal suspensionChargeRatio;
    private final CalculationMethod calculationMethod;
    private final Pricing pricing;

    public BigDecimal getPrice(List<AdditionalBillingFactor> additionalBillingFactors) {
        return pricing.getPrice(additionalBillingFactors);
    }
}
