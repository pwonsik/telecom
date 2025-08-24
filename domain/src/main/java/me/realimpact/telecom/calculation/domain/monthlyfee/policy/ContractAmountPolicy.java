package me.realimpact.telecom.calculation.domain.monthlyfee.policy;

import me.realimpact.telecom.calculation.domain.monthlyfee.AdditionalBillingFactor;
import me.realimpact.telecom.calculation.domain.monthlyfee.Pricing;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;


/* 추가과금요소의 계약금액을 가져와서 일할계산합니다. */
public class ContractAmountPolicy implements Pricing {
    @Override
    public BigDecimal getPrice(List<AdditionalBillingFactor> additionalBillingFactors) {
        return additionalBillingFactors.stream()
            .map(factor -> factor.getFactorValue("ContractAmount", Long.class))
            .filter(Optional::isPresent)
            .map(opt -> BigDecimal.valueOf(opt.orElse(0L)))
            .findFirst()
            .orElse(BigDecimal.ZERO);
    }
}
