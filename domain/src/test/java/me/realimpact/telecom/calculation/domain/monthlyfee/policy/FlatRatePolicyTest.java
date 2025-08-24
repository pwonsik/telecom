package me.realimpact.telecom.calculation.domain.monthlyfee.policy;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.Collections;

import static org.assertj.core.api.Assertions.assertThat;

class FlatRatePolicyTest {

    @Test
    @DisplayName("정액제 요금은 추가 요금 정보와 관계없이 항상 동일한 금액을 반환한다")
    void getPrice_ShouldReturnFixedAmount() {
        // given
        BigDecimal fixedAmount = BigDecimal.valueOf(10000);
        FlatRatePolicy policy = new FlatRatePolicy(fixedAmount);

        // when
        BigDecimal price = policy.getPrice(Collections.emptyList());

        // then
        assertThat(price).isEqualByComparingTo(fixedAmount);
    }
} 