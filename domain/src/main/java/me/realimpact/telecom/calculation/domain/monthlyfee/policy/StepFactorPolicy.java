package me.realimpact.telecom.calculation.domain.monthlyfee.policy;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import lombok.RequiredArgsConstructor;
import me.realimpact.telecom.calculation.domain.monthlyfee.MonthlyChargingPolicy;
import me.realimpact.telecom.calculation.domain.monthlyfee.MonthlyFeeCalculationResult;
import me.realimpact.telecom.calculation.domain.monthlyfee.ProratedPeriod;

/**
 * 구간별로 다른 요금을 적용하고, 각 구간의 합을 계산하는 정책
 * 예: 1~5회선은 1000원, 6~10회선은 800원, 11회선 이상은 600원일 때
 * 15회선인 경우: (5회선 * 1000원) + (5회선 * 800원) + (5회선 * 600원) = 12000원
 */
@RequiredArgsConstructor
public class StepFactorPolicy implements MonthlyChargingPolicy {
    private final String factorKey;
    private final List<RangeRule> rules;

    @Override
    public Optional<MonthlyFeeCalculationResult> calculate(ProratedPeriod proratedPeriod) {
        Optional<Long> valueOpt = proratedPeriod.getAdditionalBillingFactor(factorKey, Long.class);
        if (valueOpt.isEmpty()) {
            return Optional.empty();
        }

        long value = valueOpt.get();
        BigDecimal totalAmount = BigDecimal.ZERO;
        long remainingValue = value;
        
        for (RangeRule rule : rules) {
            if (remainingValue <= 0) break;
            
            long valueInThisRange;
            if (remainingValue > (rule.getTo() - rule.getFrom() + 1)) {
                valueInThisRange = rule.getTo() - rule.getFrom() + 1;
            } else {
                valueInThisRange = remainingValue;
            }
            
            totalAmount = totalAmount.add(rule.getAmount().multiply(BigDecimal.valueOf(valueInThisRange)));
            remainingValue -= valueInThisRange;
        }

        return Optional.of(new MonthlyFeeCalculationResult(proratedPeriod, totalAmount));
    }
} 