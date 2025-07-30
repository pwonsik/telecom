package me.realimpact.telecom.calculation.domain.monthlyfee.policy;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import lombok.RequiredArgsConstructor;
import me.realimpact.telecom.calculation.domain.monthlyfee.MonthlyChargingPolicy;
import me.realimpact.telecom.calculation.domain.monthlyfee.MonthlyFeeCalculationResult;
import me.realimpact.telecom.calculation.domain.monthlyfee.ProratedPeriod;

/**
 * 전체 값이 속하는 구간의 요금을 모든 값에 적용하는 정책
 * 예: 1~5회선은 1000원, 6~10회선은 800원, 11회선 이상은 600원일 때
 * 15회선인 경우: 15회선 * 600원 = 9000원
 */
@RequiredArgsConstructor
public class TierFactorPolicy implements MonthlyChargingPolicy {
    private final String factorKey;
    private final List<RangeRule> rules;

    @Override
    public Optional<MonthlyFeeCalculationResult> calculate(ProratedPeriod proratedPeriod) {
        Optional<Long> valueOpt = proratedPeriod.getAdditionalBillingFactor(factorKey, Long.class);
        if (valueOpt.isEmpty()) {
            return Optional.empty();
        }

        long value = valueOpt.get();
        Optional<RangeRule> applicableRule = rules.stream()
                .filter(rule -> rule.isInRange(value))
                .findFirst();

        if (applicableRule.isEmpty()) {
            return Optional.empty();
        }

        BigDecimal totalAmount = applicableRule.get().getAmount()
                .multiply(BigDecimal.valueOf(value));

        return Optional.of(new MonthlyFeeCalculationResult(proratedPeriod, totalAmount));
    }
} 