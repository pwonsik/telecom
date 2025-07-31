package me.realimpact.telecom.calculation.domain.monthlyfee;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import me.realimpact.telecom.calculation.api.BillingCalculationPeriod;
import me.realimpact.telecom.calculation.api.BillingCalculationType;
import me.realimpact.telecom.calculation.api.CalculationRequest;
import me.realimpact.telecom.calculation.application.monthlyfee.AdditionalBillingFactorFactory;
import me.realimpact.telecom.calculation.application.monthlyfee.MonthlyFeeCalculator;
import me.realimpact.telecom.calculation.domain.monthlyfee.policy.FlatRatePolicy;
import me.realimpact.telecom.calculation.domain.monthlyfee.policy.RangeFactorPolicy;
import me.realimpact.telecom.calculation.domain.monthlyfee.policy.RangeRule;
import me.realimpact.telecom.calculation.domain.monthlyfee.policy.StepFactorPolicy;
import me.realimpact.telecom.calculation.domain.monthlyfee.policy.TierFactorPolicy;
import me.realimpact.telecom.calculation.port.out.ContractQueryPort;
import me.realimpact.telecom.calculation.port.out.ProductQueryPort;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;


@ExtendWith(MockitoExtension.class)
class MonthlyFeeCalculationIntegrationTest {

    @Mock
    private ContractQueryPort contractQueryPort;

    @Mock
    private ProductQueryPort productQueryPort;

    @Mock
    private AdditionalBillingFactorFactory additionalBillingFactorFactory;

    private MonthlyFeeCalculator calculator;

    private static final LocalDate BILLING_START_DATE = LocalDate.of(2024, 3, 1);
    private static final LocalDate BILLING_END_DATE = LocalDate.of(2024, 3, 31);
    private static final LocalDate MAX_END_DATE = LocalDate.of(9999, 12, 31);
    private static final Long CONTRACT_ID = 1L;

    private static final ProductOffering FLAT_RATE_OFFERING = new ProductOffering(
        "FLAT-001",
        "정액제 상품",
        List.of(new MonthlyChargeItem(
            "FLAT-001-01",
            "기본료",
            BigDecimal.ONE,
            CalculationMethod.FLAT_RATE,
            new FlatRatePolicy(BigDecimal.valueOf(30000))
        ))
    );

    private static final ProductOffering STEP_RATE_OFFERING_NO_CHARGE_WHEN_SUSPENSION = new ProductOffering(
        "STEP-001",
        "구간별 요금제 상품",
        List.of(new MonthlyChargeItem(
            "STEP-001-01",
            "회선수 기반 요금",
            BigDecimal.ZERO,
            CalculationMethod.STEP_FACTOR,
            new StepFactorPolicy("line_count", List.of(
                new RangeRule(1, 5, BigDecimal.valueOf(12000)),
                new RangeRule(6, 10, BigDecimal.valueOf(10000)),
                new RangeRule(11, 99999999, BigDecimal.valueOf(8000))
            ))
        ))
    );

    private static final ProductOffering RANGE_RATE_OFFERING = new ProductOffering(
        "RANGE-001",
        "범위별 구간 요금제 상품",
        List.of(
            new MonthlyChargeItem(
                "RANGE-001-01",
                "1G 속도 요금",
                BigDecimal.ZERO,
                CalculationMethod.RANGE_FACTOR,
                new RangeFactorPolicy("speed", List.of(
                    new RangeRule(1, 1, BigDecimal.valueOf(10000)),
                    new RangeRule(2, 2, BigDecimal.valueOf(20000))
                ))
            )
        )
    );

    private static final ProductOffering COMPLEX_RATE_OFFERING = new ProductOffering(
        "COMPLEX-001",
        "복합 요금제 상품",
        List.of(
            new MonthlyChargeItem(
                "COMPLEX-001-01",
                "기본료",
                BigDecimal.ZERO,
                CalculationMethod.FLAT_RATE,
                new FlatRatePolicy(BigDecimal.valueOf(10000))
            ),
            new MonthlyChargeItem(
                "COMPLEX-001-02",
                "5회선 요금",
                BigDecimal.ZERO,
                CalculationMethod.STEP_FACTOR,
                new StepFactorPolicy("line_count", List.of(
                    new RangeRule(1, 5, BigDecimal.valueOf(5000)),
                    new RangeRule(6, 10, BigDecimal.valueOf(8000))
                ))
            )
        )
    );

    private static final ProductOffering TIER_RATE_OFFERING = new ProductOffering(
        "TIER-001",
        "구간별 누진 요금제 상품",
        List.of(
            new MonthlyChargeItem(
                "TIER-001-01",
                "데이터 사용량 기반 요금",
                BigDecimal.ZERO,
                CalculationMethod.TIER_FACTOR,
                new TierFactorPolicy("data_usage", List.of(
                    new RangeRule(0, 5, BigDecimal.valueOf(5000)),  // 0~5GB: 5,000원/GB
                    new RangeRule(6, 10, BigDecimal.valueOf(8000)), // 6~10GB: 8,000원/GB
                    new RangeRule(11, 99999999, BigDecimal.valueOf(10000)) // 11GB 이상: 10,000원/GB
                ))
            )
        )
    );

    @BeforeEach
    void setUp() {
        calculator = new MonthlyFeeCalculator(contractQueryPort, productQueryPort, additionalBillingFactorFactory);
    }

    @Test
    @DisplayName("정액제 상품의 중간 가입 시나리오")
    void calculate_FlatRate_MidMonthSubscription() {
        // given
        LocalDate subscriptionDate = LocalDate.of(2024, 3, 15);
        Contract contract = new Contract(
            CONTRACT_ID,
            subscriptionDate,
            subscriptionDate,
            Optional.empty(),
            Optional.empty()
        );

        Product product = new Product(
            CONTRACT_ID,
            FLAT_RATE_OFFERING,
            LocalDateTime.of(2024, 3, 15, 0, 0),
            LocalDateTime.of(9999, 12, 31, 23, 59),
            subscriptionDate,
            Optional.of(subscriptionDate),
            Optional.empty()
        );

        when(contractQueryPort.findByContractId(CONTRACT_ID)).thenReturn(contract);
        when(contractQueryPort.findSuspensionHistory(any())).thenReturn(List.of());
        when(additionalBillingFactorFactory.create(any())).thenReturn(List.of());
        when(productQueryPort.findByContractId(any())).thenReturn(List.of(product));

        CalculationRequest request = createCalculationRequest(BillingCalculationType.REVENUE_CONFIRMATION, BillingCalculationPeriod.POST_BILLING_CURRENT_MONTH);

        // when
        List<MonthlyFeeCalculationResult> results = calculator.calculate(request);

        // then
        assertThat(results).hasSize(1);
        MonthlyFeeCalculationResult result = results.get(0);
        // 3/15 ~ 3/31 (17일) => 30000 * (17/31)
        assertThat(result.getFee().setScale(0, RoundingMode.FLOOR))
            .isEqualByComparingTo(BigDecimal.valueOf((long)(30000 * (17.0/31))));
    }

    @Test
    @DisplayName("회선수 구간별 요금제 상품의 정지 포함 시나리오")
    void calculate_StepRate_WithSuspension() {
        // given : 3/1
        LocalDate subscriptionDate = BILLING_START_DATE;
        Contract contract = new Contract(
            CONTRACT_ID,
            subscriptionDate,
            subscriptionDate,
            Optional.empty(),
            Optional.empty()
        );

        // 정지 기간: 3/10 ~ 3/20
        Suspension suspension = new Suspension(
            LocalDateTime.of(2024, 3, 10, 0, 0),
            LocalDateTime.of(2024, 3, 20, 23, 59),
            Suspension.SuspensionType.TEMPORARY_SUSPENSION
        );

        // 회선수에 따른 구간별 요금
        Map<String, String> factors = new HashMap<>();
        factors.put("line_count", "15");  // 15회선
        AdditionalBillingFactors billingFactors = new AdditionalBillingFactors(
            factors, BILLING_START_DATE, MAX_END_DATE);

        Product product = new Product(
            CONTRACT_ID,
            STEP_RATE_OFFERING_NO_CHARGE_WHEN_SUSPENSION,
            LocalDateTime.of(2024, 3, 1, 0, 0),
            LocalDateTime.of(9999, 12, 31, 23, 59),
            subscriptionDate,
            Optional.of(subscriptionDate),
            Optional.empty()
        );

        when(contractQueryPort.findByContractId(CONTRACT_ID)).thenReturn(contract);
        when(contractQueryPort.findSuspensionHistory(any())).thenReturn(List.of(suspension));
        when(additionalBillingFactorFactory.create(any())).thenReturn(List.of(billingFactors));
        when(productQueryPort.findByContractId(any())).thenReturn(List.of(product));

        CalculationRequest request = createCalculationRequest(BillingCalculationType.REVENUE_CONFIRMATION, BillingCalculationPeriod.POST_BILLING_CURRENT_MONTH);

        // when
        List<MonthlyFeeCalculationResult> results = calculator.calculate(request);

        // then
        assertThat(results).hasSize(3);

        /* 
         * 1~5 : 12000
         * 6~10 : 10000
         * 11~ : 8000
         */
         
        // 3/1 ~ 3/9 (9일)
        assertThat(results.get(0).getFee().setScale(0, RoundingMode.FLOOR))
            .isEqualByComparingTo(BigDecimal.valueOf((long)((12000 * 5 + 10000 * 5 + 8000 * 5) * (9.0/31))));  
        
        // 3/10 ~ 3/19 (10일) - 정지 기간
        assertThat(results.get(1).getFee().setScale(0, RoundingMode.FLOOR))
            .isEqualByComparingTo(BigDecimal.ZERO);
        
        // 3/20 ~ 3/31 (12일)
        assertThat(results.get(2).getFee().setScale(0, RoundingMode.FLOOR))
            .isEqualByComparingTo(BigDecimal.valueOf((long)((12000 * 5 + 10000 * 5 + 8000 * 5) * (12.0/31))));  // 150000 * 12/31
    }

    @Test
    @DisplayName("속도별 구간 요금제 상품의 속도 변경 시나리오")
    void calculate_RangeRate_WithSpeedChange() {
        // given - 3/1 가입
        LocalDate subscriptionDate = BILLING_START_DATE;
        Contract contract = new Contract(
            CONTRACT_ID,
            subscriptionDate,
            subscriptionDate,
            Optional.empty(),
            Optional.empty()
        );

        // 3/1 ~ 3/15: 1G
        Map<String, String> factors1 = new HashMap<>();
        factors1.put("speed", "1");
        AdditionalBillingFactors billingFactors1 = new AdditionalBillingFactors(
            factors1, BILLING_START_DATE, LocalDate.of(2024, 3, 16));

        // 3/16 ~ 3/31: 10G
        Map<String, String> factors2 = new HashMap<>();
        factors2.put("speed", "2");
        AdditionalBillingFactors billingFactors2 = new AdditionalBillingFactors(
            factors2, LocalDate.of(2024, 3, 16), MAX_END_DATE);

        Product product = new Product(
            CONTRACT_ID,
            RANGE_RATE_OFFERING,
            LocalDateTime.of(2024, 3, 1, 0, 0),
            LocalDateTime.of(9999, 12, 31, 23, 59),
            subscriptionDate,
            Optional.of(subscriptionDate),
            Optional.empty()
        );

        when(contractQueryPort.findByContractId(CONTRACT_ID)).thenReturn(contract);
        when(contractQueryPort.findSuspensionHistory(any())).thenReturn(List.of());
        when(additionalBillingFactorFactory.create(any()))
            .thenReturn(List.of(billingFactors1, billingFactors2));
        when(productQueryPort.findByContractId(any())).thenReturn(List.of(product));

        CalculationRequest request = createCalculationRequest(BillingCalculationType.REVENUE_CONFIRMATION, BillingCalculationPeriod.POST_BILLING_CURRENT_MONTH);

        // when
        List<MonthlyFeeCalculationResult> results = calculator.calculate(request);

        // then
        assertThat(results).hasSize(2);
        
        // 3/1 ~ 3/15 (15일) - 1G
        assertThat(results.get(0).getFee().setScale(0, RoundingMode.FLOOR))
            .isEqualByComparingTo(BigDecimal.valueOf((long)(10000 * (15.0/31))));  
        
        // 3/16 ~ 3/31 (16일) - 10G
        assertThat(results.get(1).getFee().setScale(0, RoundingMode.FLOOR))
            .isEqualByComparingTo(BigDecimal.valueOf((long)(20000 * (16.0/31))));  
    }

    @Test
    @DisplayName("복합 요금제(기본료 + 회선수 구간별 요금) 상품의 회선수 변경 시나리오")
    void calculate_ComplexRate_WithLineCountChange() {
        // given
        LocalDate subscriptionDate = BILLING_START_DATE;
        Contract contract = new Contract(
            CONTRACT_ID,
            subscriptionDate,
            subscriptionDate,
            Optional.empty(),
            Optional.empty()
        );

        // 3/1 ~ 3/20: 5회선
        Map<String, String> factors1 = new HashMap<>();
        factors1.put("line_count", "5");
        AdditionalBillingFactors billingFactors1 = new AdditionalBillingFactors(
            factors1, BILLING_START_DATE, LocalDate.of(2024, 3, 21));

        // 3/21 ~ 3/31: 8회선
        Map<String, String> factors2 = new HashMap<>();
        factors2.put("line_count", "8");
        AdditionalBillingFactors billingFactors2 = new AdditionalBillingFactors(
            factors2, LocalDate.of(2024, 3, 21), MAX_END_DATE);

        Product product = new Product(
            CONTRACT_ID,
            COMPLEX_RATE_OFFERING,
            LocalDateTime.of(2024, 3, 1, 0, 0),
            LocalDateTime.of(9999, 12, 31, 23, 59),
            subscriptionDate,
            Optional.of(subscriptionDate),
            Optional.empty()
        );

        when(contractQueryPort.findByContractId(CONTRACT_ID)).thenReturn(contract);
        when(contractQueryPort.findSuspensionHistory(any())).thenReturn(List.of());
        when(additionalBillingFactorFactory.create(any()))
            .thenReturn(List.of(billingFactors1, billingFactors2));
        when(productQueryPort.findByContractId(any())).thenReturn(List.of(product));

        CalculationRequest request = createCalculationRequest(BillingCalculationType.REVENUE_CONFIRMATION, BillingCalculationPeriod.POST_BILLING_CURRENT_MONTH);

        // when
        List<MonthlyFeeCalculationResult> results = calculator.calculate(request);

        // then
        assertThat(results).hasSize(4);
        
        // 기본료 계산 - 20일
        assertThat(results.get(0).getFee().setScale(0, RoundingMode.FLOOR))
            .isEqualByComparingTo(BigDecimal.valueOf((long)(10000 * (20.0/31))));

        // 기본료 계산 - 11일    
        assertThat(results.get(2).getFee().setScale(0, RoundingMode.FLOOR))
            .isEqualByComparingTo(BigDecimal.valueOf((long)(10000 * (11.0/31))));
        
        // 3/1 ~ 3/20 (20일) - 5회선
        assertThat(results.get(1).getFee().setScale(0, RoundingMode.FLOOR))
            .isEqualByComparingTo(BigDecimal.valueOf((long)(5000 * 5 * (20.0/31))));
        
        // 3/21 ~ 3/31 (11일) - 8회선
        assertThat(results.get(3).getFee().setScale(0, RoundingMode.FLOOR))
            .isEqualByComparingTo(BigDecimal.valueOf((long)((5000 * 5 + 8000 * 3)* (11.0/31))));
    }

    @Test
    @DisplayName("데이터 사용량 기반 구간별 누진 요금제 상품의 사용량 변경 시나리오")
    void calculate_TierRate_WithDataUsageChange() {
        // given
        LocalDate subscriptionDate = BILLING_START_DATE;
        Contract contract = new Contract(
            CONTRACT_ID,
            subscriptionDate,
            subscriptionDate,
            Optional.empty(),
            Optional.empty()
        );

        // 3/1 ~ 3/15: 8GB 사용
        Map<String, String> factors1 = new HashMap<>();
        factors1.put("data_usage", "8");
        AdditionalBillingFactors billingFactors1 = new AdditionalBillingFactors(
            factors1, BILLING_START_DATE, LocalDate.of(2024, 3, 16));

        // 3/16 ~ 3/31: 15GB 사용
        Map<String, String> factors2 = new HashMap<>();
        factors2.put("data_usage", "15");
        AdditionalBillingFactors billingFactors2 = new AdditionalBillingFactors(
            factors2, LocalDate.of(2024, 3, 16), MAX_END_DATE);

        Product product = new Product(
            CONTRACT_ID,
            TIER_RATE_OFFERING,
            LocalDateTime.of(2024, 3, 1, 0, 0),
            LocalDateTime.of(9999, 12, 31, 23, 59),
            subscriptionDate,
            Optional.of(subscriptionDate),
            Optional.empty()
        );

        when(contractQueryPort.findByContractId(CONTRACT_ID)).thenReturn(contract);
        when(contractQueryPort.findSuspensionHistory(any())).thenReturn(List.of());
        when(additionalBillingFactorFactory.create(any()))
            .thenReturn(List.of(billingFactors1, billingFactors2));
        when(productQueryPort.findByContractId(any())).thenReturn(List.of(product));

        CalculationRequest request = createCalculationRequest(BillingCalculationType.REVENUE_CONFIRMATION, BillingCalculationPeriod.POST_BILLING_CURRENT_MONTH);

        // when
        List<MonthlyFeeCalculationResult> results = calculator.calculate(request);

        // then
        assertThat(results).hasSize(2);
        
        // 3/1 ~ 3/15 (15일) - 8GB 사용
        assertThat(results.get(0).getFee().setScale(0, RoundingMode.FLOOR))
            .isEqualByComparingTo(BigDecimal.valueOf((long)((8 * 8000) * (15.0/31))));  
        
        // 3/16 ~ 3/31 (16일) - 15GB 사용
        assertThat(results.get(1).getFee().setScale(0, RoundingMode.FLOOR))
            .isEqualByComparingTo(BigDecimal.valueOf((long)((15 * 10000) * (16.0/31))));
    }

    private CalculationRequest createCalculationRequest(BillingCalculationType billingCalculationType, BillingCalculationPeriod billingCalculationPeriod) {
        return new CalculationRequest(
            CONTRACT_ID,
            BILLING_START_DATE,
            BILLING_END_DATE,
            billingCalculationType,
            billingCalculationPeriod
        );
    }
} 