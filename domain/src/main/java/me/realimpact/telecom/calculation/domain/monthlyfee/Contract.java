package me.realimpact.telecom.calculation.domain.monthlyfee;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
@Getter
public class Contract extends Temporal {
    private final Long contractId;
    
    private final LocalDate subscribedAt;
    private final LocalDate initiallySubscribedAt;
    private final Optional<LocalDate> terminatedAt;
    private final Optional<LocalDate> prefferedTerminationDate;
    
    // MyBatis 중첩 구조에 맞춰 products와 suspensions 포함
    private final List<Product> products;
    private final List<Suspension> suspensions;


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

    public Object getServiceCode() {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'getServiceCode'");
    }
    
    /**
     * 계약 정보, 상품 정보, 정지 정보를 기반으로 일할 계산을 위한 구간들을 생성한다.
     * 
     * @param billingPeriod 청구 기간
     * @return 일할 계산 구간 목록
     */
    public List<ProratedPeriod> buildProratedPeriods(DefaultPeriod billingPeriod) {
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

        // 모든 날짜 지점들을 정렬하여 구간 경계 생성
        List<LocalDate> datePoints = Stream.of(contractDates, productsDates, suspensionDates)
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
            if (product.overlapsWith(period)) {
                for (MonthlyChargeItem monthlyChargeItem : product.getProductOffering().getMonthlyChargeItems()) {
                    // 해당 기간에 제상 상태인 정지 찾기
                    Optional<Suspension> suspension = this.suspensions.stream()
                        .filter(s -> s.overlapsWith(period))
                        .findFirst();
                    
                    // TODO: AdditionalBillingFactors 처리 로직 추가 필요
                    List<AdditionalBillingFactors> additionalBillingFactors = List.of();
                    
                    proratedPeriods.add(
                        ProratedPeriod.builder()
                            .period(period)
                            .contract(this)
                            .product(product)
                            .productOffering(product.getProductOffering())
                            .monthlyChargeItem(monthlyChargeItem)
                            .suspension(suspension)
                            .additionalBillingFactors(additionalBillingFactors)
                            .build()
                    );
                }
            }
        }
        
        return proratedPeriods;
    }
}
