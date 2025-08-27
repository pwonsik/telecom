package me.realimpact.telecom.calculation.domain.monthlyfee;

import me.realimpact.telecom.calculation.domain.monthlyfee.policy.FlatRatePolicy;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Contract의 buildProratedPeriods 메서드를 테스트한다.
 * 이전 ProratedPeriodBuilder의 역할을 Contract가 담당하게 되었음.
 */
class ContractWithProductsAndSuspensionsProratedPeriodsTest {

    private static class TestFixture {
        static final LocalDate BILLING_START_DATE = LocalDate.of(2025, 5, 1);
        static final LocalDate BILLING_END_DATE = LocalDate.of(2025, 6, 1);

        static ProductOffering createDefaultProductOffering() {
            ChargeItem chargeItem = new ChargeItem(
                "CHARGE_001",
                "기본료",
                "REVENUE_001",
                BigDecimal.valueOf(0.5),
                CalculationMethod.FLAT_RATE,
                new FlatRatePolicy(BigDecimal.valueOf(10000))
              );
            
            return new ProductOffering(
                "PO_001",
                "기본상품",
                List.of(chargeItem)
            );
        }

        static DefaultPeriod createBillingPeriod() {
            return DefaultPeriod.of(BILLING_START_DATE, BILLING_END_DATE);
        }

        static Product createProduct(LocalDateTime effectiveStartDateTime, LocalDateTime effectiveEndDateTime, LocalDate subscribedAt) {
            return new Product(
                1L,
                createDefaultProductOffering(),
                effectiveStartDateTime,
                effectiveEndDateTime,
                subscribedAt,
                Optional.empty(),
                Optional.empty()
            );
        }
    }

    @DisplayName("5월 중간에 가입한 상품의 일할 계산 구간을 정확히 생성한다")
    @Test
    void buildWithMidMonthSubscription() {
        // given
        DefaultPeriod billingPeriod = TestFixture.createBillingPeriod();
        
        LocalDate subscriptionDate = TestFixture.BILLING_START_DATE.plusDays(14); // 5월 15일 가입
        LocalDateTime subscriptionDateTime = subscriptionDate.atStartOfDay();
        
        Product product = TestFixture.createProduct(
            subscriptionDateTime,
            TestFixture.BILLING_END_DATE.plusMonths(1).atStartOfDay(),
            subscriptionDate
        );
        
        ContractWithProductsAndSuspensions contractWithProductsAndSuspensions = new ContractWithProductsAndSuspensions(
            1L,
            subscriptionDate,
            subscriptionDate,
            Optional.empty(),
            Optional.empty(),
            billingPeriod.getStartDate(),
            billingPeriod.getEndDate(),
            List.of(product), // products
            List.of(),        // suspensions
            List.of()         // additionalBillingFactors
        );

        // when - Contract가 직접 구간을 생성
        List<ProratedPeriod> periods = contractWithProductsAndSuspensions.buildProratedPeriods();

        // then
        assertThat(periods).hasSize(1);
        assertThat(periods.get(0).getEffectiveCalculationStartDate(billingPeriod)).isEqualTo(subscriptionDate);
        assertThat(periods.get(0).getEffectiveCalculationEndDate(billingPeriod)).isEqualTo(TestFixture.BILLING_END_DATE.minusDays(1));
    }

    @DisplayName("5월 중간에 정지되고 해제되는 경우의 구간을 정확히 생성한다")
    @Test
    void buildWithMidMonthSuspension() {
        // given
        DefaultPeriod billingPeriod = TestFixture.createBillingPeriod();
        
        // 4월 1일 가입, 6월 30일 해지
        Product product = TestFixture.createProduct(
            TestFixture.BILLING_START_DATE.minusMonths(1).atStartOfDay(),
            TestFixture.BILLING_END_DATE.plusMonths(1).atStartOfDay(),
            TestFixture.BILLING_START_DATE.minusMonths(1)
        );
        
        // 5월 10일부터 5월 20일까지 일시정지
        Suspension suspension = new Suspension(
            TestFixture.BILLING_START_DATE.plusDays(9).atStartOfDay(),
            TestFixture.BILLING_START_DATE.plusDays(19).atStartOfDay(),
            Suspension.SuspensionType.TEMPORARY_SUSPENSION
        );
        
        // 4월 1일 가입
        ContractWithProductsAndSuspensions contractWithProductsAndSuspensions = new ContractWithProductsAndSuspensions(
            1L,
            TestFixture.BILLING_START_DATE.minusMonths(1),
            TestFixture.BILLING_START_DATE.minusMonths(1),
            Optional.empty(),
            Optional.empty(),
            billingPeriod.getStartDate(),
            billingPeriod.getEndDate(),
            List.of(product),    // products
            List.of(suspension), // suspensions
            List.of()            // additionalBillingFactors
        );

        // when - Contract가 직접 구간을 생성
        List<ProratedPeriod> periods = contractWithProductsAndSuspensions.buildProratedPeriods();

        // then (5/1 ~ 5/9, 5/10 ~ 5/19, 5/20 ~ 5/31)
        assertThat(periods).hasSize(3); // 정지 전, 정지 중, 정지 후
        
        assertThat(periods.get(0).getEffectiveCalculationStartDate(billingPeriod)).isEqualTo(TestFixture.BILLING_START_DATE);
        assertThat(periods.get(0).getEffectiveCalculationEndDate(billingPeriod)).isEqualTo(TestFixture.BILLING_START_DATE.plusDays(8));
        
        assertThat(periods.get(1).getEffectiveCalculationStartDate(billingPeriod)).isEqualTo(TestFixture.BILLING_START_DATE.plusDays(9));
        assertThat(periods.get(1).getEffectiveCalculationEndDate(billingPeriod)).isEqualTo(TestFixture.BILLING_START_DATE.plusDays(18));
        assertThat(periods.get(1).getSuspension()).isPresent();
        
        assertThat(periods.get(2).getEffectiveCalculationStartDate(billingPeriod)).isEqualTo(TestFixture.BILLING_START_DATE.plusDays(19));
        assertThat(periods.get(2).getEffectiveCalculationEndDate(billingPeriod)).isEqualTo(TestFixture.BILLING_END_DATE.minusDays(1));
    }

    @DisplayName("5월 중간에 추가 과금 요소가 변경되는 경우의 구간을 정확히 생성한다")
    @Test
    void buildWithMidMonthAdditionalBillingFactorsChange() {
        // given
        DefaultPeriod billingPeriod = TestFixture.createBillingPeriod();
        
        // 4월 1일 가입, 6월 30일 해지
        Product product = TestFixture.createProduct(
            TestFixture.BILLING_START_DATE.minusMonths(1).atStartOfDay(),
            TestFixture.BILLING_END_DATE.plusMonths(1).atStartOfDay(),
            TestFixture.BILLING_START_DATE.minusMonths(1)
        );
        
        // 4월 1일 가입
        ContractWithProductsAndSuspensions contractWithProductsAndSuspensions = new ContractWithProductsAndSuspensions(
            1L,
            TestFixture.BILLING_START_DATE.minusMonths(1),
            TestFixture.BILLING_START_DATE.minusMonths(1),
            Optional.empty(),
            Optional.empty(),
            billingPeriod.getStartDate(),
            billingPeriod.getEndDate(),
            List.of(product), // products
            List.of(),        // suspensions
            List.of()         // additionalBillingFactors
        );
        
        // TODO: AdditionalBillingFactors 처리 로직이 구현되면 테스트 추가
        // 현재는 기본적인 구간 분리만 테스트

        // when - Contract가 직접 구간을 생성
        List<ProratedPeriod> periods = contractWithProductsAndSuspensions.buildProratedPeriods();

        // then - 추가 과금 요소가 없으므로 전체 기간이 하나의 구간
        assertThat(periods).hasSize(1);
        
        assertThat(periods.get(0).getEffectiveCalculationStartDate(billingPeriod)).isEqualTo(TestFixture.BILLING_START_DATE);
        assertThat(periods.get(0).getEffectiveCalculationEndDate(billingPeriod)).isEqualTo(TestFixture.BILLING_END_DATE.minusDays(1));
    }

    @DisplayName("5월 중간에 가입하고 정지되는 복합 케이스의 구간을 정확히 생성한다")
    @Test
    void buildWithMidMonthSubscriptionAndSuspension() {
        // given
        DefaultPeriod billingPeriod = TestFixture.createBillingPeriod();
        
        LocalDate subscriptionDate = TestFixture.BILLING_START_DATE.plusDays(4); // 5월 5일 가입
        LocalDateTime subscriptionDateTime = subscriptionDate.atStartOfDay();
        
        Product product = TestFixture.createProduct(
            subscriptionDateTime,
            TestFixture.BILLING_END_DATE.plusMonths(1).atStartOfDay(),
            subscriptionDate
        );
        
        // 5월 15일부터 5월 25일까지 정지
        Suspension suspension = new Suspension(
            TestFixture.BILLING_START_DATE.plusDays(14).atStartOfDay(),
            TestFixture.BILLING_START_DATE.plusDays(24).atStartOfDay(),
            Suspension.SuspensionType.TEMPORARY_SUSPENSION
        );
        
        ContractWithProductsAndSuspensions contractWithProductsAndSuspensions = new ContractWithProductsAndSuspensions(
            1L,
            subscriptionDate,
            subscriptionDate,
            Optional.empty(),
            Optional.empty(),
            billingPeriod.getStartDate(),
            billingPeriod.getEndDate(),
            List.of(product),    // products
            List.of(suspension), // suspensions
            List.of()            // additionalBillingFactors
        );

        // when - Contract가 직접 구간을 생성
        List<ProratedPeriod> periods = contractWithProductsAndSuspensions.buildProratedPeriods();

        // then (5/5 ~ 5/14, 5/15 ~ 5/24, 5/25 ~ 5/31)
        assertThat(periods).hasSize(3);
        
        // 가입일(5/5)부터 정지 시작일(5/15)까지
        assertThat(periods.get(0).getEffectiveCalculationStartDate(billingPeriod)).isEqualTo(subscriptionDate);
        assertThat(periods.get(0).getEffectiveCalculationEndDate(billingPeriod)).isEqualTo(TestFixture.BILLING_START_DATE.plusDays(13));
        
        // 정지 기간(5/15 ~ 5/25)
        assertThat(periods.get(1).getEffectiveCalculationStartDate(billingPeriod)).isEqualTo(TestFixture.BILLING_START_DATE.plusDays(14));
        assertThat(periods.get(1).getEffectiveCalculationEndDate(billingPeriod)).isEqualTo(TestFixture.BILLING_START_DATE.plusDays(23));
        assertThat(periods.get(1).getSuspension()).isPresent();
        
        // 정지 해제일(5/25)부터 월말(5/31)까지
        assertThat(periods.get(2).getEffectiveCalculationStartDate(billingPeriod)).isEqualTo(TestFixture.BILLING_START_DATE.plusDays(24));
        assertThat(periods.get(2).getEffectiveCalculationEndDate(billingPeriod)).isEqualTo(TestFixture.BILLING_END_DATE.minusDays(1));
    }
}