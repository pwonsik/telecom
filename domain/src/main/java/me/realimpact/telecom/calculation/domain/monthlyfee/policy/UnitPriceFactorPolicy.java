package me.realimpact.telecom.calculation.domain.monthlyfee.policy;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

import lombok.RequiredArgsConstructor;
import me.realimpact.telecom.calculation.domain.monthlyfee.Pricing;
import me.realimpact.telecom.calculation.domain.monthlyfee.AdditionalBillingFactors;
import me.realimpact.telecom.calculation.domain.monthlyfee.MonthlyFeeCalculationResult;
import me.realimpact.telecom.calculation.domain.monthlyfee.ProratedPeriod;

/*
 * 단가 * 건수 계산 정책
 */
@RequiredArgsConstructor
public class UnitPriceFactorPolicy implements Pricing {

    private final String factorKey;
    private final BigDecimal unitPrice;

    @Override
    public BigDecimal getPrice(List<AdditionalBillingFactors> additionalBillingFactors) {
        // 건수 조회
        Long count = additionalBillingFactors.stream()
            .map(factor -> factor.getFactorValue(factorKey, Long.class))
            .filter(Optional::isPresent)
            .map(opt -> opt.orElse(0L))
            .findFirst()
            .orElse(0L);

        return unitPrice.multiply(BigDecimal.valueOf(count));
    }
}
