package me.realimpact.telecom.calculation.domain.monthlyfee.policy;

import java.math.BigDecimal;
import java.util.Optional;

import me.realimpact.telecom.calculation.domain.monthlyfee.MonthlyChargingPolicy;
import me.realimpact.telecom.calculation.domain.monthlyfee.MonthlyFeeCalculationResult;
import me.realimpact.telecom.calculation.domain.monthlyfee.ProratedPeriod;

/* 상품에 정의된 월정액료를 가져와서 일할계산합니다. 가장 기본적인 계산방식입니다. */
public class FlatRatePolicy implements MonthlyChargingPolicy {
    @Override
    public Optional<MonthlyFeeCalculationResult> calculate(ProratedPeriod proratedPeriod) {
        BigDecimal productOfferingMonthlyFee = proratedPeriod.getMonthlyChargeItem().getChargeItemAmount();
        BigDecimal proratedFee = proratedPeriod.getProratedFee(productOfferingMonthlyFee);

        return Optional.of(new MonthlyFeeCalculationResult(proratedPeriod, proratedFee));
    }
}
