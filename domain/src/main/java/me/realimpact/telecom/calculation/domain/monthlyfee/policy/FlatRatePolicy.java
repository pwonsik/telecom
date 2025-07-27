package me.realimpact.telecom.calculation.domain.monthlyfee.policy;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import java.util.Optional;

import me.realimpact.telecom.calculation.domain.monthlyfee.MonthlyFeeCalculationResult;
import me.realimpact.telecom.calculation.domain.monthlyfee.ProrationPeriod;

public class FlatRatePolicy implements MonthlyChargingPolicy {
    @Override
    public MonthlyFeeCalculationResult calculate(ProrationPeriod calculationPeriod) {
        BigDecimal productOfferingMonthlyFee = calculationPeriod.getMonthlyChargeItem().getChargeItemAmount();
        BigDecimal proratedFee = calculationPeriod.getProratedAmount(productOfferingMonthlyFee);

        return new MonthlyFeeCalculationResult(calculationPeriod, proratedFee);
    }
}
