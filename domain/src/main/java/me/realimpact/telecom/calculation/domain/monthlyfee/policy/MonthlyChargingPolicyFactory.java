package me.realimpact.telecom.calculation.domain.monthlyfee.policy;

import me.realimpact.telecom.calculation.domain.monthlyfee.CalculationMethod;
import me.realimpact.telecom.calculation.domain.monthlyfee.MonthlyChargingPolicy;

public interface MonthlyChargingPolicyFactory {
    MonthlyChargingPolicy getPolicy(CalculationMethod calculationMethod);
}
