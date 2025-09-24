package me.realimpact.telecom.calculation.domain.monthlyfee;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.ToString;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.stream.IntStream;
import java.util.stream.Stream;

@RequiredArgsConstructor
@Getter
@ToString
public class ContractWithProductsAndSuspensions extends Temporal implements MonthlyChargeDomain {
    private final Long contractId;
    
    private final LocalDate subscribedAt;
    private final LocalDate initiallySubscribedAt;
    private final Optional<LocalDate> terminatedAt;
    private final Optional<LocalDate> prefferedTerminationDate;
    private final LocalDate billingStartDate;
    private final LocalDate billingEndDate;

    private final List<Product> products;
    private final List<Suspension> suspensions;
    private final List<AdditionalBillingFactor> additionalBillingFactors;

    @Override
    public LocalDate getStartDate() {
        return Stream.of(
            subscribedAt,
            initiallySubscribedAt
        ).max(Comparator.naturalOrder()).orElseThrow();
    }

    @Override
    public LocalDate getEndDate() {
        return Stream.of(
            terminatedAt.orElse(LocalDate.MAX),
            prefferedTerminationDate.orElse(LocalDate.MAX)
        ).min(Comparator.naturalOrder()).orElseThrow();
    }
    
    /**
     * 계약 정보, 상품 정보, 정지 정보를 기반으로 일할 계산을 위한 구간들을 생성한다.
     *
     * @return 일할 계산 구간 목록
     */
    public List<ProratedPeriod> buildProratedPeriods() {
        DefaultPeriod billingPeriod = DefaultPeriod.of(billingStartDate, billingEndDate);
        // 계약, 상품, 정지의 모든 시작/종료 날짜들을 수집
        Stream<LocalDate> contractDates = Stream.of(
            this.getEffectiveCalculationStartDate(billingPeriod), 
            this.getEffectiveCalculationEndDate(billingPeriod)
        );
        
        Stream<LocalDate> productsDates = this.products.stream()
            .flatMap(product -> Stream.of(
                product.getEffectiveCalculationStartDate(billingPeriod), 
                product.getEffectiveCalculationEndDate(billingPeriod)
            ));
            
        Stream<LocalDate> suspensionDates = this.suspensions.stream()
            .flatMap(suspension -> Stream.of(
                suspension.getEffectiveCalculationStartDate(billingPeriod), 
                suspension.getEffectiveCalculationEndDate(billingPeriod)
            ));

        Stream<LocalDate> additionalBillingFactorDates = this.additionalBillingFactors.stream()
                .flatMap(additionalBillingFactor -> Stream.of(
                        additionalBillingFactor.getEffectiveCalculationStartDate(billingPeriod),
                        additionalBillingFactor.getEffectiveCalculationEndDate(billingPeriod)
                ));

        // 모든 날짜 지점들을 정렬하여 구간 경계 생성
        List<LocalDate> datePoints = Stream.of(contractDates, productsDates, suspensionDates, additionalBillingFactorDates)
            .flatMap(datePoint -> datePoint)
            .distinct()
            .sorted()
            .toList();

        // 각 구간별로 ProratedPeriod 리스트를 생성
        return IntStream.range(0, datePoints.size() - 1)
            .mapToObj(i -> DefaultPeriod.of(
                datePoints.get(i), 
                datePoints.get(i + 1).minusDays(1)
            ))
            .flatMap(period -> createProratedPeriods(period, billingPeriod).stream())
            .toList();
    }
    
    /**
     * 주어진 기간에 대해 상품별, 과금항목별 ProratedPeriod를 생성한다.
     */
    private List<ProratedPeriod> createProratedPeriods(Temporal period, DefaultPeriod billingPeriod) {
        List<ProratedPeriod> proratedPeriods = new ArrayList<>();
        
        for (Product product : this.products) {
            // 상품이 해당 기간과 겹치는지 확인
            if (!product.overlapsWith(period)) {
                continue;
            }

            for (ChargeItem chargeItem : product.getProductOffering().getChargeItems()) {
                // 해당 기간에 겹치는 정지이력 찾기
                Optional<Suspension> overlappedSuspension = this.suspensions.stream()
                    .filter(s -> s.overlapsWith(period))
                    .findFirst();

                List<AdditionalBillingFactor> overlappedAdditionalBillingFactor = this.additionalBillingFactors.stream()
                        .filter(bf -> bf.overlapsWith(period))
                        .toList();

                proratedPeriods.add(
                    ProratedPeriod.builder()
                        .period(period)
                        .contractWithProductsAndSuspensions(this)
                        .product(product)
                        .productOffering(product.getProductOffering())
                        .chargeItem(chargeItem)
                        .suspension(overlappedSuspension)
                        .additionalBillingFactors(overlappedAdditionalBillingFactor)
                        .build()
                );
            }
        }
        
        return proratedPeriods;
    }
}
