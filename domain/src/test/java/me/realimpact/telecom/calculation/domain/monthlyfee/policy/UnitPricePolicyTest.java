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

class UnitPricePolicyTest {

    private static final String FACTOR_KEY = "usage_count";
    private static final BigDecimal UNIT_PRICE = BigDecimal.valueOf(1000);
    private static final LocalDate TODAY = LocalDate.now();

    @Test
    @DisplayName("추가 요금 정보가 없으면 0원을 반환한다")
    void getPrice_WhenNoFactors_ShouldReturnZero() {
        // given
        UnitPriceFactorPolicy policy = new UnitPriceFactorPolicy(FACTOR_KEY, UNIT_PRICE);

        // when
        BigDecimal price = policy.getPrice(Collections.emptyList());

        // then
        assertThat(price).isEqualByComparingTo(BigDecimal.ZERO);
    }

    @Test
    @DisplayName("단가와 사용량을 곱한 금액을 반환한다")
    void getPrice_ShouldMultiplyUnitPriceByCount() {
        // given
        UnitPriceFactorPolicy policy = new UnitPriceFactorPolicy(FACTOR_KEY, UNIT_PRICE);
        Map<String, String> factors = new HashMap<>();
        factors.put(FACTOR_KEY, "5");
        AdditionalBillingFactor billingFactors = new AdditionalBillingFactor(factors, TODAY, TODAY.plusDays(1));

        // when
        BigDecimal price = policy.getPrice(List.of(billingFactors));

        // then
        assertThat(price).isEqualByComparingTo(BigDecimal.valueOf(5000));
    }

    @Test
    @DisplayName("요금 정보가 여러 개일 때 첫 번째 값을 사용한다")
    void getPrice_WithMultipleFactors_ShouldUseFirstValue() {
        // given
        UnitPriceFactorPolicy policy = new UnitPriceFactorPolicy(FACTOR_KEY, UNIT_PRICE);
        
        Map<String, String> factors1 = new HashMap<>();
        factors1.put(FACTOR_KEY, "3");
        AdditionalBillingFactor billingFactors1 = new AdditionalBillingFactor(factors1, TODAY, TODAY.plusDays(1));
        
        Map<String, String> factors2 = new HashMap<>();
        factors2.put(FACTOR_KEY, "5");
        AdditionalBillingFactor billingFactors2 = new AdditionalBillingFactor(factors2, TODAY, TODAY.plusDays(1));

        // when
        BigDecimal price = policy.getPrice(List.of(billingFactors1, billingFactors2));

        // then
        assertThat(price).isEqualByComparingTo(BigDecimal.valueOf(3000));
    }
} 