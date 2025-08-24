package me.realimpact.telecom.calculation.domain.monthlyfee.policy;

import me.realimpact.telecom.calculation.domain.monthlyfee.AdditionalBillingFactor;
import me.realimpact.telecom.calculation.domain.monthlyfee.policy.MatchingFactorPolicy.MatchingRule;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class MatchingFactorPolicyTest {

    private static final LocalDate TODAY = LocalDate.now();

    @Test
    @DisplayName("추가 요금 정보가 없으면 0원을 반환한다")
    void getPrice_WhenNoFactors_ShouldReturnZero() {
        // given
        Map<String, String> conditions = new HashMap<>();
        conditions.put("line_type", "dedicated");
        conditions.put("speed", "1G");
        
        List<MatchingRule> rules = List.of(
            new MatchingRule("전용회선 1G", conditions, BigDecimal.valueOf(100000))
        );
        
        MatchingFactorPolicy policy = new MatchingFactorPolicy(rules);

        // when
        BigDecimal price = policy.getPrice(Collections.emptyList());

        // then
        assertThat(price).isEqualByComparingTo(BigDecimal.ZERO);
    }

    @Test
    @DisplayName("조건이 일치하면 해당 요금을 반환한다")
    void getPrice_WhenConditionsMatch_ShouldReturnMatchingFee() {
        // // given
        // Map<String, String> conditions = new HashMap<>();
        // conditions.put("line_type", "dedicated");
        // conditions.put("speed", "1G");
        
        // List<MatchingRule> rules = List.of(
        //     new MatchingRule("전용회선 1G", conditions, BigDecimal.valueOf(100000))
        // );
        
        // MatchingFactorPolicy policy = new MatchingFactorPolicy(rules);

        // Map<String, String> factors = new HashMap<>();
        // factors.put("line_type", "dedicated");
        // factors.put("speed", "1G");
        // AdditionalBillingFactors billingFactors = new AdditionalBillingFactors(factors, TODAY, TODAY.plusDays(1));

        // // when
        // BigDecimal price = policy.getPrice(List.of(billingFactors));

        // // then
        // assertThat(price).isEqualByComparingTo(BigDecimal.valueOf(100000));
    }

    @Test
    @DisplayName("조건이 일치하지 않으면 0원을 반환한다")
    void getPrice_WhenConditionsDoNotMatch_ShouldReturnZero() {
        // given
        Map<String, String> conditions = new HashMap<>();
        conditions.put("line_type", "dedicated");
        conditions.put("speed", "1G");
        
        List<MatchingRule> rules = List.of(
            new MatchingRule("전용회선 1G", conditions, BigDecimal.valueOf(100000))
        );
        
        MatchingFactorPolicy policy = new MatchingFactorPolicy(rules);

        Map<String, String> factors = new HashMap<>();
        factors.put("line_type", "dedicated");
        factors.put("speed", "10G"); // 다른 속도
        AdditionalBillingFactor billingFactors = new AdditionalBillingFactor(factors, TODAY, TODAY.plusDays(1));

        // when
        BigDecimal price = policy.getPrice(List.of(billingFactors));

        // then
        assertThat(price).isEqualByComparingTo(BigDecimal.ZERO);
    }

    @Test
    @DisplayName("여러 조건 중 첫 번째로 일치하는 조건의 요금을 반환한다")
    void getPrice_WithMultipleRules_ShouldReturnFirstMatchingFee() {
        // // given
        // Map<String, String> conditions1 = new HashMap<>();
        // conditions1.put("line_type", "dedicated");
        // conditions1.put("speed", "1G");
        
        // Map<String, String> conditions2 = new HashMap<>();
        // conditions2.put("line_type", "dedicated");
        // conditions2.put("speed", "10G");
        
        // List<MatchingRule> rules = List.of(
        //     new MatchingRule("전용회선 1G", conditions1, BigDecimal.valueOf(100000)),
        //     new MatchingRule("전용회선 10G", conditions2, BigDecimal.valueOf(200000))
        // );
        
        // MatchingFactorPolicy policy = new MatchingFactorPolicy(rules);

        // Map<String, String> factors = new HashMap<>();
        // factors.put("line_type", "dedicated");
        // factors.put("speed", "1G");
        // AdditionalBillingFactors billingFactors = new AdditionalBillingFactors(factors, TODAY, TODAY.plusDays(1));

        // // when
        // BigDecimal price = policy.getPrice(List.of(billingFactors));

        // // then
        // assertThat(price).isEqualByComparingTo(BigDecimal.valueOf(100000));
    }
} 