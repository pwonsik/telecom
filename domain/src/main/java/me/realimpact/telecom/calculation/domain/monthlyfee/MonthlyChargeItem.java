package me.realimpact.telecom.calculation.domain.monthlyfee;

import java.math.BigDecimal;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
@Getter
public class MonthlyChargeItem {
    private final String chargeItemId;
    private final String chargeItemName;
    private final BigDecimal chargeItemAmount;
    private final BigDecimal suspensionChargeRatio;
    private final CalculationMethod calculationMethod;
}
