package me.realimpact.telecom.calculation.domain.monthlyfee;

import java.math.BigDecimal;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import me.realimpact.telecom.calculation.domain.monthlyfee.policy.CalculationMethod;
import me.realimpact.telecom.calculation.domain.monthlyfee.policy.MonthlyChargingPolicy;

@RequiredArgsConstructor
@Getter
public class MonthlyChargeItem {
    private final String chargeItemId;
    private final String chargeItemName;
    private final MonthlyChargingPolicy chargingPolicy;
    private final BigDecimal chargeItemAmount;
    private final BigDecimal suspensionChargeRatio;
    private final CalculationMethod calculationMethod;
}
