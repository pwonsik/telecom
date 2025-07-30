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
import me.realimpact.telecom.calculation.domain.monthlyfee.ProratedPeriod;

/*
 * 단가 * 건수 계산 정책
 */
@RequiredArgsConstructor
public class UnitPriceFactorPolicy implements MonthlyChargingPolicy {

    private final String factorName;

    @Override
    public Optional<MonthlyFeeCalculationResult> calculate(ProratedPeriod proratedPeriod) {
        // 건수 조회
        Long count = proratedPeriod.getAdditionalBillingFactor(factorName, Long.class)
            .orElseThrow(() -> new IllegalArgumentException("Billing factor not found: " + factorName));
        BigDecimal unitPrice = proratedPeriod.getMonthlyChargeItem().getChargeItemAmount();

        // 단가 * 건수
        BigDecimal proratedFee = proratedPeriod.getProratedFee(unitPrice).multiply(BigDecimal.valueOf(count));

        return Optional.of(new MonthlyFeeCalculationResult(proratedPeriod, proratedFee));        
    }
}
