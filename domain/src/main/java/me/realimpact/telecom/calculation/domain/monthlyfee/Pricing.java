package me.realimpact.telecom.calculation.domain.monthlyfee;

import java.math.BigDecimal;
import java.util.List;

/* 하나의 상세 구간/상품/과금항목에 대한 계산로직을 책임진다. 이의 구현체는 policy 패키지에 개발한다.*/
public interface Pricing {
    BigDecimal getPrice(List<AdditionalBillingFactor> additionalBillingFactors);
}
