package me.realimpact.telecom.calculation.domain.monthlyfee.policy;

import java.math.BigDecimal;
import java.util.Optional;

import me.realimpact.telecom.calculation.domain.monthlyfee.MonthlyChargingPolicy;
import me.realimpact.telecom.calculation.domain.monthlyfee.MonthlyFeeCalculationResult;
import me.realimpact.telecom.calculation.domain.monthlyfee.ProratedPeriod;

/* 추가과금요소의 계약금액을 가져와서 일할계산합니다. */
public class ContractAmountPolicy implements MonthlyChargingPolicy {
    @Override
    public Optional<MonthlyFeeCalculationResult> calculate(ProratedPeriod proratedPeriod) {
        BigDecimal contractAmount = BigDecimal.valueOf(
            proratedPeriod.getAdditionalBillingFactor("ContractAmount", Long.class).orElse(0L)
        );
        BigDecimal proratedFee = proratedPeriod.getProratedFee(contractAmount);
        return Optional.of(new MonthlyFeeCalculationResult(proratedPeriod, proratedFee));
    }
}
