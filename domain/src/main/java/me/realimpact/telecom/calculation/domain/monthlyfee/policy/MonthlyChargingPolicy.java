package me.realimpact.telecom.calculation.domain.monthlyfee.policy;

import java.util.List;

import me.realimpact.telecom.calculation.domain.monthlyfee.MonthlyFeeCalculationResult;
import me.realimpact.telecom.calculation.domain.monthlyfee.ProrationPeriod;

public interface MonthlyChargingPolicy {
    MonthlyFeeCalculationResult calculate(ProrationPeriod calculationPeriod);
}
