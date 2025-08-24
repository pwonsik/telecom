package me.realimpact.telecom.calculation.domain.monthlyfee.policy;

import lombok.RequiredArgsConstructor;
import me.realimpact.telecom.calculation.domain.monthlyfee.AdditionalBillingFactor;
import me.realimpact.telecom.calculation.domain.monthlyfee.Pricing;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;


/**
 * 전체 값이 속하는 구간의 요금을 모든 값에 적용하는 정책
 * 예: 1~5회선은 1000원, 6~10회선은 800원, 11회선 이상은 600원일 때
 * 15회선인 경우: 15회선 * 600원 = 9000원
 */
@RequiredArgsConstructor
public class TierFactorPolicy implements Pricing {
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

        BigDecimal price = rules.stream()
                .filter(rule -> rule.isInRange(billingFactor))
                .findFirst()
                .map(RangeRule::getFee)
                .orElse(BigDecimal.ZERO);

        return price.multiply(BigDecimal.valueOf(billingFactor));
    }
} 