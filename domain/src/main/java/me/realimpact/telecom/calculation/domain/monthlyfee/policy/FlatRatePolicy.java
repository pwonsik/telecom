package me.realimpact.telecom.calculation.domain.monthlyfee.policy;

import java.math.BigDecimal;
import java.util.Optional;

import me.realimpact.telecom.calculation.domain.monthlyfee.MonthlyChargingPolicy;
import me.realimpact.telecom.calculation.domain.monthlyfee.MonthlyFeeCalculationResult;
import me.realimpact.telecom.calculation.domain.monthlyfee.ProrationPeriod;

public class FlatRatePolicy implements MonthlyChargingPolicy {
    @Override
    public Optional<MonthlyFeeCalculationResult> calculate(ProrationPeriod prorationPeriod) {
        BigDecimal productOfferingMonthlyFee = prorationPeriod.getMonthlyChargeItem().getChargeItemAmount();
        BigDecimal proratedFee = prorationPeriod.getProratedAmount(productOfferingMonthlyFee);

        return Optional.of(new MonthlyFeeCalculationResult(prorationPeriod, proratedFee));
    }
}
