package me.realimpact.telecom.calculation.domain.monthlyfee.policy;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import lombok.RequiredArgsConstructor;
import me.realimpact.telecom.calculation.domain.monthlyfee.MonthlyChargingPolicy;
import me.realimpact.telecom.calculation.domain.monthlyfee.MonthlyFeeCalculationResult;
import me.realimpact.telecom.calculation.domain.monthlyfee.ProratedPeriod;

/*
 * 범위 계산 정책
 * 범위 내에 존재하는 팩터면 그 범위 구간에 매핑된 가격 리턴       
 */
@RequiredArgsConstructor
public class RangeFactorPolicy implements MonthlyChargingPolicy {
    private final String factorKey;
    private final List<RangeRule> rules;

    @Override
    public Optional<MonthlyFeeCalculationResult> calculate(ProratedPeriod proratedPeriod) {
        for (RangeRule rule : rules) {
            Long billingFactor = proratedPeriod.getAdditionalBillingFactor(factorKey, Long.class)
                .orElseThrow(() -> new IllegalArgumentException("Billing factor not found: " + factorKey));
            if (rule.isInRange(billingFactor)) {
                BigDecimal proratedFee = proratedPeriod.getProratedFee(rule.getAmount());
                return Optional.of(new MonthlyFeeCalculationResult(proratedPeriod, proratedFee));
            }
        }
        return Optional.empty();
    }

}
