package me.realimpact.telecom.calculation.domain.monthlyfee;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;

import me.realimpact.telecom.calculation.domain.model.*;

import java.util.stream.IntStream;

public class ProrationPeriodBuilder {
    private final Contract contract;
    private final List<Product> products;
    private final List<Suspension> suspensions;
    private final List<AdditionalBillingFactors> billingFactors;
    private final Period billingPeriod;

    public ProrationPeriodBuilder(
        Contract contract, 
        List<Product> products, 
        List<Suspension> suspensions, 
        List<AdditionalBillingFactors> billingFactors,
        Period billingPeriod) {

        this.contract = contract;
        this.products = products;
        this.suspensions = suspensions;
        this.billingFactors = billingFactors;
        this.billingPeriod = billingPeriod;
    }

    public List<ProrationPeriod> build() {
        Stream<LocalDate> contractDates = Stream.of(contract.getStartDate(), contract.getEndDate());
        Stream<LocalDate> productsDates = products.stream().flatMap(s -> Stream.of(s.getStartDate(), s.getEndDate()));
        Stream<LocalDate> suspensionDates = suspensions.stream().flatMap(s -> Stream.of(s.getStartDate(), s.getEndDate()));
        Stream<LocalDate> billingFactorDates = billingFactors.stream().flatMap(bf -> Stream.of(bf.getStartDate(), bf.getEndDate()));
        Stream<LocalDate> billingDates = Stream.of(billingPeriod.getStartDate(), billingPeriod.getEndDate());

        List<LocalDate> datePoints = Stream.of(contractDates, productsDates, suspensionDates, billingFactorDates, billingDates)
                .flatMap(s -> s)
                .distinct()
                .sorted()
                .toList();

        // 각 구간별로 ProrationPeriod 리스트를 생성하고, 이를 flatMap으로 평탄화하여 단일 List<ProrationPeriod>로 반환합니다.
        return IntStream.range(0, datePoints.size() - 1)
                .mapToObj(i -> Period.of(datePoints.get(i), datePoints.get(i + 1)))
                .flatMap(period -> createProrationPeriods(period).stream())
                .toList();
    }

    private List<ProrationPeriod> createProrationPeriods(Period period) {
        List<ProrationPeriod> prorationPeriods = new ArrayList<>();
        for (Product product : products) {
            for (MonthlyChargeItem monthlyChargeItem : product.getProductOffering().getMonthlyChargeItems()) {
                Optional<Suspension> suspension = suspensions.stream()
                        .filter(s -> s.overlapsWith(period))
                        .findFirst();
                List<AdditionalBillingFactors> billingFactors = this.billingFactors.stream()
                        .filter(billingFactor -> billingFactor.overlapsWith(period))
                        .toList();
                prorationPeriods.add(
                        ProrationPeriod.builder()
                                .period(period)
                                .contract(contract)
                                .product(product)
                                .productOffering(product.getProductOffering())
                                .monthlyChargeItem(monthlyChargeItem)
                                .suspension(suspension)
                                .billingFactors(billingFactors)
                                .build()
                );
            }
        }
        return prorationPeriods;
    }
}
