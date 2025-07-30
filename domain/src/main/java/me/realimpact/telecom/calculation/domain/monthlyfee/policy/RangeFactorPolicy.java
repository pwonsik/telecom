package me.realimpact.telecom.calculation.domain.monthlyfee.policy;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import lombok.RequiredArgsConstructor;
import me.realimpact.telecom.calculation.domain.monthlyfee.MonthlyChargingPolicy;
import me.realimpact.telecom.calculation.domain.monthlyfee.MonthlyFeeCalculationResult;
import me.realimpact.telecom.calculation.domain.monthlyfee.ProrationPeriod;

/*
 * 범위 계산 정책
 *        
 */
@RequiredArgsConstructor
public class RangeFactorPolicy implements MonthlyChargingPolicy {
    private final String factorKey;
    private final List<RangeRule> rules;

    @Override
    public Optional<MonthlyFeeCalculationResult> calculate(ProrationPeriod prorationPeriod) {
        for (RangeRule rule : rules) {
            Long billingFactor = prorationPeriod.getAdditionalBillingFactor(factorKey, Long.class)
                .orElseThrow(() -> new IllegalArgumentException("Billing factor not found: " + factorKey));
            if (rule.isInRange(billingFactor)) {
                BigDecimal proratedFee = prorationPeriod.getProratedFee(rule.getAmount());
                return Optional.of(new MonthlyFeeCalculationResult(prorationPeriod, proratedFee));
            }
        }
        return Optional.empty();
    }

}
