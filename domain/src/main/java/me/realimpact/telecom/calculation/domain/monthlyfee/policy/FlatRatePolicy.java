package me.realimpact.telecom.calculation.domain.monthlyfee.policy;

import me.realimpact.telecom.calculation.domain.monthlyfee.AdditionalBillingFactor;
import me.realimpact.telecom.calculation.domain.monthlyfee.Pricing;

import java.math.BigDecimal;
import java.util.List;


/* 상품에 정의된 월정액료를 가져와서 일할계산합니다. 가장 기본적인 계산방식입니다. */
public class FlatRatePolicy implements Pricing {
    private final BigDecimal fee;

    public FlatRatePolicy(BigDecimal fee) {
        this.fee = fee;
    }

    @Override
    public BigDecimal getPrice(List<AdditionalBillingFactor> additionalBillingFactors) {
        return this.fee;
    }
}
