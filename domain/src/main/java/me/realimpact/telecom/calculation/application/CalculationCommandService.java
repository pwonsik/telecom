package me.realimpact.telecom.calculation.application;

import lombok.extern.slf4j.Slf4j;
import me.realimpact.telecom.calculation.api.CalculationCommandUseCase;
import me.realimpact.telecom.calculation.api.CalculationResultGroup;
import me.realimpact.telecom.calculation.application.discount.CalculationResultProrater;
import me.realimpact.telecom.calculation.application.discount.DiscountCalculator;
import me.realimpact.telecom.calculation.application.monthlyfee.MonthlyFeeCalculator;
import me.realimpact.telecom.calculation.application.onetimecharge.OneTimeChargeCalculator;
import me.realimpact.telecom.calculation.application.vat.VatCalculator;
import me.realimpact.telecom.calculation.domain.CalculationContext;
import me.realimpact.telecom.calculation.domain.CalculationResult;
import me.realimpact.telecom.calculation.domain.monthlyfee.MonthlyChargeDomain;
import me.realimpact.telecom.calculation.domain.onetimecharge.OneTimeChargeDomain;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.function.BiFunction;

/**
 * 요금 계산 요청을 처리하고 전체 계산 과정을 조율하는 서비스 클래스.
 * CalculationCommandUseCase 인터페이스를 구현하며, 실제 요금 계산 로직의 진입점 역할을 한다.
 */
@Service
@Slf4j
public class CalculationCommandService implements CalculationCommandUseCase {
    private final CalculationTargetLoader calculationTargetLoader;
    private final DiscountCalculator discountCalculator;
    private final List<MonthlyFeeCalculator<? extends MonthlyChargeDomain>> monthlyFeeCalculators;
    private final List<OneTimeChargeCalculator<? extends OneTimeChargeDomain>> oneTimeChargeCalculators;
    private final CalculationResultProrater calculationResultProrater;
    private final VatCalculator vatCalculator;

    /**
     * 필요한 의존성을 주입받아 서비스를 초기화한다.
     * @param calculationTargetLoader 계산 대상을 로드하는 로더
     * @param discountCalculator 할인 계산기
     * @param monthlyFeeCalculators 월정액 계산기 목록
     * @param oneTimeChargeCalculators 일회성 요금 계산기 목록
     * @param calculationResultProrater 계산 결과 일할 계산기
     * @param vatCalculator 부가세 계산기
     */
    public CalculationCommandService(
            CalculationTargetLoader calculationTargetLoader,
            DiscountCalculator discountCalculator,
            List<MonthlyFeeCalculator<? extends MonthlyChargeDomain>> monthlyFeeCalculators,
            List<OneTimeChargeCalculator<? extends OneTimeChargeDomain>> oneTimeChargeCalculators,
            CalculationResultProrater calculationResultProrater,
            VatCalculator vatCalculator
    ) {
        this.calculationTargetLoader = calculationTargetLoader;
        this.discountCalculator = discountCalculator;
        this.monthlyFeeCalculators = monthlyFeeCalculators;
        this.oneTimeChargeCalculators = oneTimeChargeCalculators;
        this.calculationResultProrater = calculationResultProrater;
        this.vatCalculator = vatCalculator;
    }

    /**
     * {@inheritDoc}
     * 계약 ID 목록을 받아 전체 요금 계산을 수행하고 그 결과를 반환한다.
     */
    @Override
    public List<CalculationResultGroup> calculate(List<Long> contractIds, CalculationContext ctx) {
        return calculationTargetLoader.load(contractIds, ctx).stream()
                .map(calculationTarget -> processCalculation(calculationTarget, ctx))
                .toList();
    }

    /**
     * 단일 계산 대상(CalculationTarget)에 대한 전체 요금 계산 프로세스를 수행한다.
     * 월정액 계산, 일회성 요금 계산, 일할 계산, 할인, 합산, 부가세 계산 순서로 진행된다.
     * @param calculationTarget 계산 대상
     * @param ctx 계산 컨텍스트
     * @return 계산 결과 그룹
     */
    public CalculationResultGroup processCalculation(CalculationTarget calculationTarget, CalculationContext ctx) {
        try {
            log.debug("Processing contract calculation for contractId: {}", calculationTarget.contractId());
            List<CalculationResult<?>> results = new ArrayList<>();

            // 1. 월정액 계산
            for (var monthlyFeeCalculator : monthlyFeeCalculators) {
                processMonthlyFeeCalculator(monthlyFeeCalculator, calculationTarget, ctx, results);
            }
            log.debug("Processed 월정액 {} calculation results for contractId: {}", results.size(), calculationTarget.contractId());

            // 2. 일회성 과금 계산
            for (var oneTimeChargeCalculator : oneTimeChargeCalculators) {
                processOneTimeChargeCalculator(oneTimeChargeCalculator, calculationTarget, ctx, results);
            }
            log.debug("Processed 일회성 {} calculation results for contractId: {}", results.size(), calculationTarget.contractId());

            // 3. 일할 계산 (Proration)
            results = new ArrayList<>(calculationResultProrater.prorate(ctx, results, calculationTarget.discounts()));
            log.debug("Processed 구간분리 {} calculation results for contractId: {}", results.size(), calculationTarget.contractId());

            // 4. 할인 적용
            results.addAll(discountCalculator.process(ctx, results, calculationTarget.discounts()));
            log.debug("Processed 할인 {} calculation results for contractId: {}", results.size(), calculationTarget.contractId());

            // 5. 구간 합치기 (Consolidation)
            results = new ArrayList<>(calculationResultProrater.consolidate(results));
            log.debug("Processed 합치기 {} calculation results for contractId: {}", results.size(), calculationTarget.contractId());

            // 6. 부가세(VAT) 계산
            results.addAll(vatCalculator.calculateVat(ctx, results));
            log.debug("Processed 부가세 {} calculation results for contractId: {}", results.size(), calculationTarget.contractId());

            log.debug("Processed {} calculation results for contractId: {}", results.size(), calculationTarget.contractId());

            return new CalculationResultGroup(results);
        } catch (Exception e) {
            log.error("Failed to process contract calculation for contractId: {}", calculationTarget.contractId(), e);
            throw e;
        }
    }

    /**
     * 제네릭을 사용하여 MonthlyFeeCalculator를 타입 안전하게 처리한다.
     * @param calculator 월정액 계산기
     * @param target 계산 대상
     * @param ctx 계산 컨텍스트
     * @param results 계산 결과를 담을 리스트
     * @param <T> MonthlyChargeDomain을 상속하는 타입
     */
    private <T extends MonthlyChargeDomain> void processMonthlyFeeCalculator(
            MonthlyFeeCalculator<T> calculator,
            CalculationTarget target,
            CalculationContext ctx,
            List<CalculationResult<?>> results) {
        Class<T> inputType = calculator.getDomainType();
        List<T> inputData = target.getMonthlyChargeData(inputType);
        results.addAll(process(inputData, calculator::process, ctx));
    }

    /**
     * 제네릭을 사용하여 OneTimeChargeCalculator를 타입 안전하게 처리한다.
     * @param calculator 일회성 요금 계산기
     * @param target 계산 대상
     * @param ctx 계산 컨텍스트
     * @param results 계산 결과를 담을 리스트
     * @param <T> OneTimeChargeDomain을 상속하는 타입
     */
    private <T extends OneTimeChargeDomain> void processOneTimeChargeCalculator(
            OneTimeChargeCalculator<T> calculator,
            CalculationTarget target,
            CalculationContext ctx,
            List<CalculationResult<?>> results) {
        Class<T> inputType = calculator.getDomainType();
        List<T> inputData = target.getOneTimeChargeData(inputType);
        results.addAll(process(inputData, calculator::process, ctx));
    }

    /**
     * 계산 항목 컬렉션에 대해 특정 프로세서(계산 로직)를 적용하는 공통 메서드.
     * @param items 계산할 항목 컬렉션
     * @param processor 계산 로직을 담은 함수
     * @param context 계산 컨텍스트
     * @param <T> 계산 항목의 타입
     * @return 계산 결과 리스트
     */
    private <T> List<CalculationResult<?>> process(
            Collection<T> items,
            BiFunction<CalculationContext, T, List<? extends CalculationResult<?>>> processor,
            CalculationContext context
    ) {
        return items.stream()
                .flatMap(item -> processor.apply(context, item).stream())
                .<CalculationResult<?>>map(result -> result)
                .toList();
    }
}