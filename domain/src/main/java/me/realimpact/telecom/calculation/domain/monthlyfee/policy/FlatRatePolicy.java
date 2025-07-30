package me.realimpact.telecom.calculation.domain.monthlyfee.policy;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import me.realimpact.telecom.calculation.domain.monthlyfee.Pricing;
import me.realimpact.telecom.calculation.domain.monthlyfee.AdditionalBillingFactors;


/* 상품에 정의된 월정액료를 가져와서 일할계산합니다. 가장 기본적인 계산방식입니다. */
public class FlatRatePolicy implements Pricing {
    private final BigDecimal fee;

    public FlatRatePolicy(BigDecimal fee) {
        this.fee = fee;
    }

    @Override
    public BigDecimal getPrice(List<AdditionalBillingFactors> additionalBillingFactors) {
        return this.fee;
    }
}
