package me.realimpact.telecom.calculation.domain.monthlyfee.policy;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

import lombok.RequiredArgsConstructor;
import me.realimpact.telecom.calculation.domain.monthlyfee.MonthlyChargingPolicy;
import me.realimpact.telecom.calculation.domain.monthlyfee.MonthlyFeeCalculationResult;
import me.realimpact.telecom.calculation.domain.monthlyfee.ProrationPeriod;

/*
 * 단가 * 건수 계산 정책
 */
@RequiredArgsConstructor
public class UnitPriceFactorPolicy implements MonthlyChargingPolicy {

    private final String factorName;

    @Override
    public Optional<MonthlyFeeCalculationResult> calculate(ProrationPeriod prorationPeriod) {
        // 건수 조회
        Long count = prorationPeriod.getAdditionalBillingFactor(factorName, Long.class)
            .orElseThrow(() -> new IllegalArgumentException("Billing factor not found: " + factorName));
        BigDecimal unitPrice = prorationPeriod.getMonthlyChargeItem().getChargeItemAmount();

        // 단가 * 건수
        BigDecimal proratedFee = prorationPeriod.getProratedFee(unitPrice).multiply(BigDecimal.valueOf(count));

        return Optional.of(new MonthlyFeeCalculationResult(prorationPeriod, proratedFee));        
    }
}
