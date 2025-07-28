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

    public record RangeRule(String factorName,long from, long to, long amountToCharge, boolean includeUpperValue) {
        public long findMatch(long billingFactor) {
            if (billingFactor >= from && (includeUpperValue ? billingFactor <= to : billingFactor < to)) {
                return this.amountToCharge;
            }
            return 0L;
        }
    }
    private final List<RangeRule> rules;

    @Override
    public Optional<MonthlyFeeCalculationResult> calculate(ProrationPeriod prorationPeriod) {
        for (RangeRule rule : rules) {
            Long billingFactor = prorationPeriod.getAdditionalBillingFactor(rule.factorName(), Long.class)
                .orElseThrow(() -> new IllegalArgumentException("Billing factor not found: " + rule.factorName()));
            long amountToCharge = rule.findMatch(billingFactor);
            BigDecimal proratedFee = prorationPeriod.getProratedAmount(BigDecimal.valueOf(amountToCharge));
            return Optional.of(new MonthlyFeeCalculationResult(prorationPeriod, proratedFee));
        }
        return Optional.empty();
    }

}
