package me.realimpact.telecom.calculation.domain.monthlyfee.policy;

import me.realimpact.telecom.calculation.domain.monthlyfee.ProductOffering;

public record MonthlyChargingPolicyKey(ProductOffering productOffering, CalculationMethod calculationMethod) {

}
