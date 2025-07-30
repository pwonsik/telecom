package me.realimpact.telecom.calculation.domain.monthlyfee;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;

import lombok.RequiredArgsConstructor;

import java.util.stream.IntStream;

/*
 * 계약정보, 상품가입정보, 정지정보, 빌링과금을 위한 추가 요소, 청구기간 등 여러가지 기간 정보를 기반으로
 * 중첩된 구간 목록을 생성한다.
 */
@RequiredArgsConstructor
public class ProratedPeriodBuilder {
    private final Contract contract;
    private final List<Product> products;
    private final List<Suspension> suspensions;
    private final List<AdditionalBillingFactors> additionalBillingFactors;
    private final DefaultPeriod billingPeriod;

    public List<ProratedPeriod> build() {
        Stream<LocalDate> contractDates = Stream.of(contract.getEffectiveCalculationStartDate(billingPeriod), contract.getEffectiveCalculationEndDate(billingPeriod));
        Stream<LocalDate> productsDates = products.stream().flatMap(s -> Stream.of(s.getEffectiveCalculationStartDate(billingPeriod), s.getEffectiveCalculationEndDate(billingPeriod)));
        Stream<LocalDate> suspensionDates = suspensions.stream().flatMap(s -> Stream.of(s.getEffectiveCalculationStartDate(billingPeriod), s.getEffectiveCalculationEndDate(billingPeriod)));
        Stream<LocalDate> billingFactorDates = additionalBillingFactors.stream().flatMap(bf -> Stream.of(bf.getEffectiveCalculationStartDate(billingPeriod), bf.getEffectiveCalculationEndDate(billingPeriod)));

        List<LocalDate> datePoints = Stream.of(contractDates, productsDates, suspensionDates, billingFactorDates)
                .flatMap(datePoint -> datePoint)
                .distinct()
                .sorted()
                .toList();

        // 각 구간별로 ProrationPeriod 리스트를 생성하고, 이를 flatMap으로 평탄화하여 단일 List<ProrationPeriod>로 반환합니다.
        return IntStream.range(0, datePoints.size() - 1)
                .mapToObj(i -> DefaultPeriod.of(datePoints.get(i), datePoints.get(i + 1).minusDays(1)))
                .flatMap(period -> createProratedPeriods(period).stream())
                .toList();
    }

    private List<ProratedPeriod> createProratedPeriods(Temporal period) {
        List<ProratedPeriod> prorationPeriods = new ArrayList<>();
        for (Product product : products) {
            for (MonthlyChargeItem monthlyChargeItem : product.getProductOffering().getMonthlyChargeItems()) {
                Optional<Suspension> suspension = suspensions.stream()
                        .filter(s -> s.overlapsWith(period))
                        .findFirst();
                List<AdditionalBillingFactors> additionalBillingFactors = this.additionalBillingFactors.stream()
                        .filter(billingFactor -> billingFactor.overlapsWith(period))
                        .toList();
                prorationPeriods.add(
                        ProratedPeriod.builder()
                                .period(period)
                                .contract(contract)
                                .product(product)
                                .productOffering(product.getProductOffering())
                                .monthlyChargeItem(monthlyChargeItem)
                                .suspension(suspension)
                                .additionalBillingFactors(additionalBillingFactors)
                                .build()
                );
            }
        }
        return prorationPeriods;
    }
}
