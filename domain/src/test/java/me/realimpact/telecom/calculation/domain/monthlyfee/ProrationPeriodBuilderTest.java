package me.realimpact.telecom.calculation.domain.monthlyfee;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

class ProrationPeriodBuilderTest {

    private static class TestFixture {
        static final LocalDate BILLING_START_DATE = LocalDate.of(2025, 5, 1);
        static final LocalDate BILLING_END_DATE = LocalDate.of(2025, 6, 1);

        static ProductOffering createDefaultProductOffering() {
            MonthlyChargeItem monthlyChargeItem = new MonthlyChargeItem(
                "CHARGE_001",
                "기본료",
                BigDecimal.valueOf(10000),
                BigDecimal.valueOf(0.5),
                CalculationMethod.FLAT_RATE
            );
            
            return new ProductOffering(
                "PO_001",
                "기본상품",
                List.of(monthlyChargeItem)
            );
        }

        static Period createBillingPeriod() {
            return Period.of(BILLING_START_DATE, BILLING_END_DATE);
        }

        static Product createProduct(LocalDateTime effectiveStartDateTime, LocalDateTime effectiveEndDateTime, LocalDate subscribedAt, Temporal billingPeriod) {
            return new Product(
                1L,
                createDefaultProductOffering(),
                effectiveStartDateTime,
                effectiveEndDateTime,
                subscribedAt,
                Optional.empty(),
                Optional.empty(),
                billingPeriod
            );
        }
    }

    @DisplayName("5월 중간에 가입한 상품의 일할 계산 구간을 정확히 생성한다")
    @Test
    void buildWithMidMonthSubscription() {
        // given
        Period billingPeriod = TestFixture.createBillingPeriod();
        
        LocalDate subscriptionDate = TestFixture.BILLING_START_DATE.plusDays(14); // 5월 15일 가입
        LocalDateTime subscriptionDateTime = subscriptionDate.atStartOfDay();
        
        Contract contract = new Contract(
            1L,
            subscriptionDate,
            subscriptionDate,
            Optional.empty(),
            Optional.empty(),
            billingPeriod
        );
        
        Product product = TestFixture.createProduct(
            subscriptionDateTime,
            TestFixture.BILLING_END_DATE.plusMonths(1).atStartOfDay(),
            subscriptionDate,
            billingPeriod
        );

        ProrationPeriodBuilder builder = new ProrationPeriodBuilder(
            contract,
            List.of(product),
            List.of(),
            List.of(),
            billingPeriod
        );

        // when
        List<ProrationPeriod> periods = builder.build();

        // then
        assertThat(periods).hasSize(1);
        assertThat(periods.get(0).getCalculationStartDate()).isEqualTo(subscriptionDate);
        assertThat(periods.get(0).getCalculationEndDate()).isEqualTo(TestFixture.BILLING_END_DATE.minusDays(1));
    }

    @DisplayName("5월 중간에 정지되고 해제되는 경우의 구간을 정확히 생성한다")
    @Test
    void buildWithMidMonthSuspension() {
        // given
        Period billingPeriod = TestFixture.createBillingPeriod();
        
        // 4월 1일 가입
        Contract contract = new Contract(
            1L,
            TestFixture.BILLING_START_DATE.minusMonths(1),
            TestFixture.BILLING_START_DATE.minusMonths(1),
            Optional.empty(),
            Optional.empty(),
            billingPeriod
        );
        
        // 4월 1일 가입, 6월 30일 해지
        Product product = TestFixture.createProduct(
            TestFixture.BILLING_START_DATE.minusMonths(1).atStartOfDay(),
            TestFixture.BILLING_END_DATE.plusMonths(1).atStartOfDay(),
            TestFixture.BILLING_START_DATE.minusMonths(1),
            billingPeriod
        );
        
        // 5월 10일부터 5월 20일까지 일시정지
        Suspension suspension = new Suspension(
            TestFixture.BILLING_START_DATE.plusDays(9).atStartOfDay(),
            TestFixture.BILLING_START_DATE.plusDays(19).atStartOfDay(),
            Suspension.SuspensionType.TEMPORARY_SUSPENSION,
            billingPeriod
        );

        ProrationPeriodBuilder builder = new ProrationPeriodBuilder(
            contract,
            List.of(product),
            List.of(suspension),
            List.of(),
            billingPeriod
        );

        // when
        List<ProrationPeriod> periods = builder.build();

        // then (5/1 ~ 5/9, 5/10 ~ 5/19, 5/20 ~ 5/31)
        assertThat(periods).hasSize(3); // 정지 전, 정지 중, 정지 후
        
        assertThat(periods.get(0).getCalculationStartDate()).isEqualTo(TestFixture.BILLING_START_DATE);
        assertThat(periods.get(0).getCalculationEndDate()).isEqualTo(TestFixture.BILLING_START_DATE.plusDays(8));
        
        assertThat(periods.get(1).getCalculationStartDate()).isEqualTo(TestFixture.BILLING_START_DATE.plusDays(9));
        assertThat(periods.get(1).getCalculationEndDate()).isEqualTo(TestFixture.BILLING_START_DATE.plusDays(18));
        assertThat(periods.get(1).getSuspension()).isPresent();
        
        assertThat(periods.get(2).getCalculationStartDate()).isEqualTo(TestFixture.BILLING_START_DATE.plusDays(19));
        assertThat(periods.get(2).getCalculationEndDate()).isEqualTo(TestFixture.BILLING_END_DATE.minusDays(1));
    }

    @DisplayName("5월 중간에 추가 과금 요소가 변경되는 경우의 구간을 정확히 생성한다")
    @Test
    void buildWithMidMonthAdditionalBillingFactorsChange() {
        // given
        Period billingPeriod = TestFixture.createBillingPeriod();
        
        // 4월 1일 가입
        Contract contract = new Contract(
            1L,
            TestFixture.BILLING_START_DATE.minusMonths(1),
            TestFixture.BILLING_START_DATE.minusMonths(1),
            Optional.empty(),
            Optional.empty(),
            billingPeriod
        );
        
        // 4월 1일 가입, 6월 30일 해지
        Product product = TestFixture.createProduct(
            TestFixture.BILLING_START_DATE.minusMonths(1).atStartOfDay(),
            TestFixture.BILLING_END_DATE.plusMonths(1).atStartOfDay(),
            TestFixture.BILLING_START_DATE.minusMonths(1),
            billingPeriod
        );
        
        // 5월 5일부터 5월 25일까지 추가 과금 요소 적용
        Map<String, String> factors = new HashMap<>();
        factors.put("ContractAmount", "20000");
        
        AdditionalBillingFactors additionalFactor = new AdditionalBillingFactors(
            factors,
            TestFixture.BILLING_START_DATE.plusDays(4).atStartOfDay(),
            TestFixture.BILLING_START_DATE.plusDays(24).atStartOfDay(),
            Optional.empty(),
            billingPeriod
        );

        ProrationPeriodBuilder builder = new ProrationPeriodBuilder(
            contract,
            List.of(product),
            List.of(),
            List.of(additionalFactor),
            billingPeriod
        );

        // when
        List<ProrationPeriod> periods = builder.build();

        // then (5/1 ~ 5/4. 5/5 ~ 5/24. 5/25 ~ 5/31)
        assertThat(periods).hasSize(3); // 추가요소 전, 추가요소 적용 중, 추가요소 후
        
        assertThat(periods.get(0).getCalculationStartDate()).isEqualTo(TestFixture.BILLING_START_DATE);
        assertThat(periods.get(0).getCalculationEndDate()).isEqualTo(TestFixture.BILLING_START_DATE.plusDays(3));
        
        assertThat(periods.get(1).getCalculationStartDate()).isEqualTo(TestFixture.BILLING_START_DATE.plusDays(4));
        assertThat(periods.get(1).getCalculationEndDate()).isEqualTo(TestFixture.BILLING_START_DATE.plusDays(23));
        assertThat(periods.get(1).getAdditionalBillingFactors()).isNotEmpty();
        
        assertThat(periods.get(2).getCalculationStartDate()).isEqualTo(TestFixture.BILLING_START_DATE.plusDays(24));
        assertThat(periods.get(2).getCalculationEndDate()).isEqualTo(TestFixture.BILLING_END_DATE.minusDays(1));
    }

    @DisplayName("5월 중간에 가입하고 정지되는 복합 케이스의 구간을 정확히 생성한다")
    @Test
    void buildWithMidMonthSubscriptionAndSuspension() {
        // given
        Period billingPeriod = TestFixture.createBillingPeriod();
        
        LocalDate subscriptionDate = TestFixture.BILLING_START_DATE.plusDays(4); // 5월 5일 가입
        LocalDateTime subscriptionDateTime = subscriptionDate.atStartOfDay();
        
        Contract contract = new Contract(
            1L,
            subscriptionDate,
            subscriptionDate,
            Optional.empty(),
            Optional.empty(),
            billingPeriod
        );
        
        Product product = TestFixture.createProduct(
            subscriptionDateTime,
            TestFixture.BILLING_END_DATE.plusMonths(1).atStartOfDay(),
            subscriptionDate,
            billingPeriod
        );
        
        // 5월 15일부터 5월 25일까지 정지
        Suspension suspension = new Suspension(
            TestFixture.BILLING_START_DATE.plusDays(14).atStartOfDay(),
            TestFixture.BILLING_START_DATE.plusDays(24).atStartOfDay(),
            Suspension.SuspensionType.TEMPORARY_SUSPENSION,
            billingPeriod
        );

        ProrationPeriodBuilder builder = new ProrationPeriodBuilder(
            contract,
            List.of(product),
            List.of(suspension),
            List.of(),
            billingPeriod
        );

        // when
        List<ProrationPeriod> periods = builder.build();

        // then (5/5 ~ 5/14, 5/15 ~ 5/24, 5/25 ~ 5/31)
        assertThat(periods).hasSize(3);
        
        // 가입일(5/5)부터 정지 시작일(5/15)까지
        assertThat(periods.get(0).getCalculationStartDate()).isEqualTo(subscriptionDate);
        assertThat(periods.get(0).getCalculationEndDate()).isEqualTo(TestFixture.BILLING_START_DATE.plusDays(13));
        
        // 정지 기간(5/15 ~ 5/25)
        assertThat(periods.get(1).getCalculationStartDate()).isEqualTo(TestFixture.BILLING_START_DATE.plusDays(14));
        assertThat(periods.get(1).getCalculationEndDate()).isEqualTo(TestFixture.BILLING_START_DATE.plusDays(23));
        assertThat(periods.get(1).getSuspension()).isPresent();
        
        // 정지 해제일(5/25)부터 월말(5/31)까지
        assertThat(periods.get(2).getCalculationStartDate()).isEqualTo(TestFixture.BILLING_START_DATE.plusDays(24));
        assertThat(periods.get(2).getCalculationEndDate()).isEqualTo(TestFixture.BILLING_END_DATE.minusDays(1));
    }
} 