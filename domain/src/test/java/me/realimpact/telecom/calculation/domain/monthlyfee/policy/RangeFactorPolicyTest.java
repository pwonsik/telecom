package me.realimpact.telecom.calculation.domain.monthlyfee.policy;

import me.realimpact.telecom.calculation.domain.monthlyfee.AdditionalBillingFactor;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class RangeFactorPolicyTest {

    private static final String FACTOR_KEY = "line_speed";
    private static final LocalDate TODAY = LocalDate.now();

    @Test
    @DisplayName("추가 요금 정보가 없으면 0원을 반환한다")
    void getPrice_WhenNoFactors_ShouldReturnZero() {
        // given
        List<RangeRule> rules = List.of(
            new RangeRule(1, 100, BigDecimal.valueOf(10000)),
            new RangeRule(101, 1000, BigDecimal.valueOf(20000))
        );
        RangeFactorPolicy policy = new RangeFactorPolicy(FACTOR_KEY, rules);

        // when
        BigDecimal price = policy.getPrice(Collections.emptyList());

        // then
        assertThat(price).isEqualByComparingTo(BigDecimal.ZERO);
    }

    @Test
    @DisplayName("값이 범위 안에 있으면 해당 범위의 요금을 반환한다")
    void getPrice_WhenValueInRange_ShouldReturnRangeFee() {
        // given
        List<RangeRule> rules = List.of(
            new RangeRule(1, 100, BigDecimal.valueOf(10000)),
            new RangeRule(101, 1000, BigDecimal.valueOf(20000))
        );
        RangeFactorPolicy policy = new RangeFactorPolicy(FACTOR_KEY, rules);

        Map<String, String> factors = new HashMap<>();
        factors.put(FACTOR_KEY, "50");
        AdditionalBillingFactor billingFactors = new AdditionalBillingFactor(factors, TODAY, TODAY.plusDays(1));

        // when
        BigDecimal price = policy.getPrice(List.of(billingFactors));

        // then
        assertThat(price).isEqualByComparingTo(BigDecimal.valueOf(10000));
    }

    @Test
    @DisplayName("값이 어떤 범위에도 속하지 않으면 0원을 반환한다")
    void getPrice_WhenValueOutOfRange_ShouldReturnZero() {
        // given
        List<RangeRule> rules = List.of(
            new RangeRule(1, 100, BigDecimal.valueOf(10000)),
            new RangeRule(101, 1000, BigDecimal.valueOf(20000))
        );
        RangeFactorPolicy policy = new RangeFactorPolicy(FACTOR_KEY, rules);

        Map<String, String> factors = new HashMap<>();
        factors.put(FACTOR_KEY, "2000");
        AdditionalBillingFactor billingFactors = new AdditionalBillingFactor(factors, TODAY, TODAY.plusDays(1));

        // when
        BigDecimal price = policy.getPrice(List.of(billingFactors));

        // then
        assertThat(price).isEqualByComparingTo(BigDecimal.ZERO);
    }

    @Test
    @DisplayName("여러 개의 요금 정보가 있을 때 첫 번째 값을 사용한다")
    void getPrice_WithMultipleFactors_ShouldUseFirstValue() {
        // given
        List<RangeRule> rules = List.of(
            new RangeRule(1, 100, BigDecimal.valueOf(10000)),
            new RangeRule(101, 1000, BigDecimal.valueOf(20000))
        );
        RangeFactorPolicy policy = new RangeFactorPolicy(FACTOR_KEY, rules);

        Map<String, String> factors1 = new HashMap<>();
        factors1.put(FACTOR_KEY, "50");
        AdditionalBillingFactor billingFactors1 = new AdditionalBillingFactor(factors1, TODAY, TODAY.plusDays(1));

        Map<String, String> factors2 = new HashMap<>();
        factors2.put(FACTOR_KEY, "500");
        AdditionalBillingFactor billingFactors2 = new AdditionalBillingFactor(factors2, TODAY, TODAY.plusDays(1));

        // when
        BigDecimal price = policy.getPrice(List.of(billingFactors1, billingFactors2));

        // then
        assertThat(price).isEqualByComparingTo(BigDecimal.valueOf(10000));
    }
} 