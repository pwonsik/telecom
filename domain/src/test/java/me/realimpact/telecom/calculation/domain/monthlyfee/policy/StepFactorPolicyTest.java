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

class StepFactorPolicyTest {

    private static final String FACTOR_KEY = "line_count";
    private static final LocalDate TODAY = LocalDate.now();

    @Test
    @DisplayName("추가 요금 정보가 없으면 0원을 반환한다")
    void getPrice_WhenNoFactors_ShouldReturnZero() {
        // given
        List<RangeRule> rules = List.of(
            new RangeRule(1, 5, BigDecimal.valueOf(1000)),
            new RangeRule(6, 10, BigDecimal.valueOf(800)),
            new RangeRule(11, 15, BigDecimal.valueOf(600))
        );
        StepFactorPolicy policy = new StepFactorPolicy(FACTOR_KEY, rules);

        // when
        BigDecimal price = policy.getPrice(Collections.emptyList());

        // then
        assertThat(price).isEqualByComparingTo(BigDecimal.ZERO);
    }

    @Test
    @DisplayName("첫 번째 구간 내의 수량에 대해 올바른 가격을 계산한다")
    void getPrice_WithinFirstRange_ShouldCalculateCorrectPrice() {
        // given
        List<RangeRule> rules = List.of(
            new RangeRule(1, 5, BigDecimal.valueOf(1000)),
            new RangeRule(6, 10, BigDecimal.valueOf(800)),
            new RangeRule(11, 15, BigDecimal.valueOf(600))
        );
        StepFactorPolicy policy = new StepFactorPolicy(FACTOR_KEY, rules);

        Map<String, String> factors = new HashMap<>();
        factors.put(FACTOR_KEY, "3");
        AdditionalBillingFactor billingFactors = new AdditionalBillingFactor(factors, TODAY, TODAY.plusDays(1));

        // when
        BigDecimal price = policy.getPrice(List.of(billingFactors));

        // then
        // 3회선 * 1000원 = 3000원
        assertThat(price).isEqualByComparingTo(BigDecimal.valueOf(3000));
    }

    @Test
    @DisplayName("여러 구간에 걸친 수량에 대해 올바른 가격을 계산한다")
    void getPrice_AcrossMultipleRanges_ShouldCalculateCorrectPrice() {
        // given
        List<RangeRule> rules = List.of(
            new RangeRule(1, 5, BigDecimal.valueOf(1000)),
            new RangeRule(6, 10, BigDecimal.valueOf(800)),
            new RangeRule(11, 15, BigDecimal.valueOf(600))
        );
        StepFactorPolicy policy = new StepFactorPolicy(FACTOR_KEY, rules);

        Map<String, String> factors = new HashMap<>();
        factors.put(FACTOR_KEY, "15");
        AdditionalBillingFactor billingFactors = new AdditionalBillingFactor(factors, TODAY, TODAY.plusDays(1));

        // when
        BigDecimal price = policy.getPrice(List.of(billingFactors));

        // then
        // (5회선 * 1000원) + (5회선 * 800원) + (5회선 * 600원) = 12000원
        assertThat(price).isEqualByComparingTo(BigDecimal.valueOf(12000));
    }

    @Test
    @DisplayName("구간의 경계값에서도 올바른 가격을 계산한다")
    void getPrice_AtRangeBoundary_ShouldCalculateCorrectPrice() {
        // given
        List<RangeRule> rules = List.of(
            new RangeRule(1, 5, BigDecimal.valueOf(1000)),
            new RangeRule(6, 10, BigDecimal.valueOf(800)),
            new RangeRule(11, 15, BigDecimal.valueOf(600))
        );
        StepFactorPolicy policy = new StepFactorPolicy(FACTOR_KEY, rules);

        Map<String, String> factors = new HashMap<>();
        factors.put(FACTOR_KEY, "10");
        AdditionalBillingFactor billingFactors = new AdditionalBillingFactor(factors, TODAY, TODAY.plusDays(1));

        // when
        BigDecimal price = policy.getPrice(List.of(billingFactors));

        // then
        // (5회선 * 1000원) + (5회선 * 800원) = 9000원
        assertThat(price).isEqualByComparingTo(BigDecimal.valueOf(9000));
    }

    @Test
    @DisplayName("여러 개의 요금 정보가 있을 때 첫 번째 값을 사용한다")
    void getPrice_WithMultipleFactors_ShouldUseFirstValue() {
        // given
        List<RangeRule> rules = List.of(
            new RangeRule(1, 5, BigDecimal.valueOf(1000)),
            new RangeRule(6, 10, BigDecimal.valueOf(800)),
            new RangeRule(11, 15, BigDecimal.valueOf(600))
        );
        StepFactorPolicy policy = new StepFactorPolicy(FACTOR_KEY, rules);

        Map<String, String> factors1 = new HashMap<>();
        factors1.put(FACTOR_KEY, "7");
        AdditionalBillingFactor billingFactors1 = new AdditionalBillingFactor(factors1, TODAY, TODAY.plusDays(1));

        Map<String, String> factors2 = new HashMap<>();
        factors2.put(FACTOR_KEY, "15");
        AdditionalBillingFactor billingFactors2 = new AdditionalBillingFactor(factors2, TODAY, TODAY.plusDays(1));

        // when
        BigDecimal price = policy.getPrice(List.of(billingFactors1, billingFactors2));

        // then
        // (5회선 * 1000원) + (2회선 * 800원) = 6600원
        assertThat(price).isEqualByComparingTo(BigDecimal.valueOf(6600));
    }
} 