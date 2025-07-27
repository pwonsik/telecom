package me.realimpact.telecom.calculation.domain.monthlyfee.policy;

public interface MonthlyChargingPolicyFactory {
    MonthlyChargingPolicy getPolicy(MonthlyChargingPolicyKey calculationMethod);
}
