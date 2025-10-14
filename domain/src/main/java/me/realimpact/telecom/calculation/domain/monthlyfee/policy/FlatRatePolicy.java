package me.realimpact.telecom.calculation.domain.monthlyfee.policy;

import me.realimpact.telecom.calculation.domain.monthlyfee.AdditionalBillingFactor;
import me.realimpact.telecom.calculation.domain.monthlyfee.Pricing;

import java.math.BigDecimal;
import java.util.List;

/**
 * 정액 요금 계산 정책을 구현하는 클래스.
 * 상품에 정의된 고정된 월정액 요금을 반환한다.
 */
public class FlatRatePolicy implements Pricing {
    private final BigDecimal fee;

    /**
     * 정액 요금 정책 생성자.
     * @param fee 고정 요금
     */
    public FlatRatePolicy(BigDecimal fee) {
        this.fee = fee;
    }

    /**
     * 추가 과금 요소를 고려하지 않고, 설정된 고정 요금을 그대로 반환한다.
     * @param additionalBillingFactors 추가 과금 요소 목록 (이 정책에서는 사용되지 않음)
     * @return 고정 요금
     */
    @Override
    public BigDecimal getPrice(List<AdditionalBillingFactor> additionalBillingFactors) {
        return this.fee;
    }
}