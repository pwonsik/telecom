package me.realimpact.telecom.calculation.domain.monthlyfee.policy;

import lombok.RequiredArgsConstructor;
import me.realimpact.telecom.calculation.domain.monthlyfee.AdditionalBillingFactor;
import me.realimpact.telecom.calculation.domain.monthlyfee.Pricing;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

/*
 * 범위 계산 정책
 * 범위 내에 존재하는 팩터면 그 범위 구간에 매핑된 가격 리턴       
 */
@RequiredArgsConstructor
public class RangeFactorPolicy implements Pricing {
    private final String factorKey;
    private final List<RangeRule> rules;

    @Override
    public BigDecimal getPrice(List<AdditionalBillingFactor> additionalBillingFactors) {
        Long billingFactor = additionalBillingFactors.stream()
            .map(factor -> factor.getFactorValue(factorKey, Long.class))
            .filter(Optional::isPresent)
            .map(opt -> opt.orElse(0L))
            .findFirst()
            .orElse(0L);

        return rules.stream()
            .filter(rule -> rule.isInRange(billingFactor))
            .map(RangeRule::getFee)
            .findFirst()
            .orElse(BigDecimal.ZERO);
    }
}
