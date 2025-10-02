package me.realimpact.telecom.calculation.application;

import lombok.extern.slf4j.Slf4j;
import me.realimpact.telecom.calculation.api.CalculationCommandUseCase;
import me.realimpact.telecom.calculation.api.CalculationResultGroup;
import me.realimpact.telecom.calculation.application.discount.CalculationResultProrater;
import me.realimpact.telecom.calculation.application.discount.DiscountCalculator;
import me.realimpact.telecom.calculation.application.monthlyfee.MonthlyFeeDataLoader;
import me.realimpact.telecom.calculation.application.monthlyfee.MonthlyFeeCalculator;
import me.realimpact.telecom.calculation.domain.monthlyfee.MonthlyChargeDomain;
import me.realimpact.telecom.calculation.application.onetimecharge.OneTimeChargeCalculator;
import me.realimpact.telecom.calculation.application.vat.VatCalculator;
import me.realimpact.telecom.calculation.domain.CalculationContext;
import me.realimpact.telecom.calculation.application.onetimecharge.OneTimeChargeDataLoader;
import me.realimpact.telecom.calculation.domain.CalculationResult;
import me.realimpact.telecom.calculation.domain.discount.ContractDiscounts;
import me.realimpact.telecom.calculation.domain.onetimecharge.OneTimeChargeDomain;

import java.util.*;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;

@Service
@Slf4j
public class CalculationCommandService implements CalculationCommandUseCase {
    private final DiscountCalculator discountCalculator;

    // Monthly Fee 관련
    private final Map<Class<? extends MonthlyChargeDomain>, MonthlyFeeDataLoader<? extends MonthlyChargeDomain>> monthlyFeeDataLoaderMap;
    private final List<MonthlyFeeCalculator<? extends MonthlyChargeDomain>> monthlyFeeCalculators;

    // OneTime Charge 관련
    private final Map<Class<? extends OneTimeChargeDomain>, OneTimeChargeDataLoader<? extends OneTimeChargeDomain>>
            oneTimeChargeDataLoaderMap;
    private final List<OneTimeChargeCalculator<? extends OneTimeChargeDomain>> oneTimeChargeCalculators;

    private final CalculationResultProrater calculationResultProrater;
    private final VatCalculator vatCalculator;

    public CalculationCommandService(
            DiscountCalculator discountCalculator,
            List<MonthlyFeeDataLoader<? extends MonthlyChargeDomain>> monthlyFeeDataLoaders,
            List<MonthlyFeeCalculator<? extends MonthlyChargeDomain>> monthlyFeeCalculators,
            List<OneTimeChargeDataLoader<? extends OneTimeChargeDomain>> oneTimeChargeDataLoaders,
            List<OneTimeChargeCalculator<? extends OneTimeChargeDomain>> oneTimeChargeCalculators,
            CalculationResultProrater calculationResultProrater,
            VatCalculator vatCalculator
    ) {
        this.discountCalculator = discountCalculator;

        // Monthly Fee DataLoader List를 Map으로 변환
        this.monthlyFeeDataLoaderMap = monthlyFeeDataLoaders.stream()
                .collect(Collectors.toMap(
                        MonthlyFeeDataLoader::getDomainType,
                        Function.identity(),
                        (oldValue, newValue) -> oldValue,
                        LinkedHashMap::new
                ));
        this.monthlyFeeCalculators = monthlyFeeCalculators;

        // OneTime Charge DataLoader List를 Map으로 변환
        this.oneTimeChargeDataLoaderMap = oneTimeChargeDataLoaders.stream()
                .collect(Collectors.toMap(
                        OneTimeChargeDataLoader::getDomainType,
                        Function.identity(),
                        (oldValue, newValue) -> oldValue,
                        LinkedHashMap::new
                ));
        this.oneTimeChargeCalculators = oneTimeChargeCalculators;

        this.calculationResultProrater = calculationResultProrater;
        this.vatCalculator = vatCalculator;

        log.info("Registered {} MonthlyFee DataLoaders: {}",
                monthlyFeeDataLoaders.size(),
                monthlyFeeDataLoaders.stream()
                        .map(loader -> loader.getDomainType().getSimpleName())
                        .collect(Collectors.joining(", ")));

        log.info("Registered {} OneTimeCharge DataLoaders: {}",
                oneTimeChargeDataLoaders.size(),
                oneTimeChargeDataLoaders.stream()
                        .map(loader -> loader.getDomainType().getSimpleName())
                        .collect(Collectors.joining(", ")));
    }

    public List<CalculationTarget> loadCalculationTargets(List<Long> contractIds, CalculationContext ctx) {
        // Monthly Fee 데이터를 Map으로 로딩
        var monthlyFeeDataByType = loadMonthlyFeeDataByType(contractIds, ctx);

        // OneTimeCharge 데이터를 Map으로 로딩 - 조건문 없음
        var oneTimeChargeDataByType = loadOneTimeChargeDataByType(contractIds, ctx);

        // 할인 (기존 방식 유지)
        var contractDiscountsMap = discountCalculator.read(ctx, contractIds);

        List<CalculationTarget> calculationTargets = new ArrayList<>();

        // 모든 조회 대상을 calculationTarget으로 모은다.
        for (Long contractId : contractIds) {
            // Monthly Fee 데이터를 계약별로 그룹화
            var monthlyFeeDataForContract = groupMonthlyFeeDataByContract(contractId, monthlyFeeDataByType);

            // OneTimeCharge 데이터를 계약별로 그룹화
            var oneTimeChargeDataForContract = groupOneTimeChargeDataByContract(contractId, oneTimeChargeDataByType);

            var discounts = Optional.ofNullable(contractDiscountsMap.get(contractId))
                    .map(ContractDiscounts::discounts)
                    .orElse(Collections.emptyList());

            CalculationTarget calculationTarget = new CalculationTarget(
                    contractId,
                    monthlyFeeDataForContract,
                    oneTimeChargeDataForContract,
                    discounts
            );
            calculationTargets.add(calculationTarget);
        }

        log.info("*********생성된 calculationTargets 개수: {}", calculationTargets.size());

        return calculationTargets;
    }


    /**
     * 모든 MonthlyFeeDataLoader를 실행하여 데이터 로딩
     */
    private Map<Class<? extends MonthlyChargeDomain>, Map<Long, List<? extends MonthlyChargeDomain>>>
        loadMonthlyFeeDataByType(List<Long> contractIds, CalculationContext context) {

        Map<Class<? extends MonthlyChargeDomain>, Map<Long, List<? extends MonthlyChargeDomain>>> result = new HashMap<>();

        for (var entry : monthlyFeeDataLoaderMap.entrySet()) {
            var dataType = entry.getKey();
            var loader = entry.getValue();
            Map<Long, List<? extends MonthlyChargeDomain>> data = loader.read(contractIds, context);
            if (!data.isEmpty()) {
                result.put(dataType, data);
            }
        }

        return result;
    }

    /**
     * 특정 계약의 MonthlyFee 데이터 그룹화
     */
    private Map<Class<? extends MonthlyChargeDomain>, List<? extends MonthlyChargeDomain>> groupMonthlyFeeDataByContract(
            Long contractId,
            Map<Class<? extends MonthlyChargeDomain>, Map<Long, List<? extends MonthlyChargeDomain>>> monthlyFeeDataByType) {

        Map<Class<? extends MonthlyChargeDomain>, List<? extends MonthlyChargeDomain>> result = new HashMap<>();

        for (var entry : monthlyFeeDataByType.entrySet()) {
            var dataType = entry.getKey();
            var dataByContract = entry.getValue();

            List<? extends MonthlyChargeDomain> contractData = dataByContract.get(contractId);
            if (contractData != null && !contractData.isEmpty()) {
                result.put(dataType, contractData);
            }
        }

        return result;
    }

    /**
     * 모든 DataLoader를 실행하여 OneTimeCharge 데이터 로딩
     * key : OneTimeCharge종류
     * value : key가 계약Id이고, value가 domain의 list인 map
     */
    private Map<Class<? extends OneTimeChargeDomain>, Map<Long, List<? extends OneTimeChargeDomain>>>
        loadOneTimeChargeDataByType(List<Long> contractIds, CalculationContext context) {

        Map<Class<? extends OneTimeChargeDomain>, Map<Long, List<? extends OneTimeChargeDomain>>> result = new HashMap<>();

        // Map을 순회하면서 각 DataLoader 실행 - 조건문 완전 제거
        //for (Map.Entry<Class<? extends OneTimeChargeDomain>, OneTimeChargeDataLoader<? extends OneTimeChargeDomain>>
        for (var entry : oneTimeChargeDataLoaderMap.entrySet()) {
//            Class<? extends OneTimeChargeDomain> dataType = entry.getKey();
//            OneTimeChargeDataLoader<? extends OneTimeChargeDomain> loader = entry.getValue();
            var dataType = entry.getKey();
            var loader = entry.getValue();
            Map<Long, List<? extends OneTimeChargeDomain>> data = loader.read(contractIds, context);
            if (!data.isEmpty()) {
                result.put(dataType, data);
            }
        }

        return result;
    }
    /**
     * 특정 계약의 OneTimeCharge 데이터 그룹화
     */
    private Map<Class<? extends OneTimeChargeDomain>, List<? extends OneTimeChargeDomain>>
        groupOneTimeChargeDataByContract(
            Long contractId,
            Map<Class<? extends OneTimeChargeDomain>, Map<Long, List<? extends OneTimeChargeDomain>>> oneTimeChargeDataByType) {

        Map<Class<? extends OneTimeChargeDomain>, List<? extends OneTimeChargeDomain>> result = new HashMap<>();

        //for (Map.Entry<Class<? extends OneTimeChargeDomain>, Map<Long, List<? extends OneTimeChargeDomain>>>
        for (var entry : oneTimeChargeDataByType.entrySet()) {

//            Class<? extends OneTimeChargeDomain> dataType = entry.getKey();
//            Map<Long, List<? extends OneTimeChargeDomain>> dataByContract = entry.getValue();
            var dataType = entry.getKey();
            var dataByContract = entry.getValue();

            List<? extends OneTimeChargeDomain> contractData = dataByContract.get(contractId);
            if (contractData != null && !contractData.isEmpty()) {
                result.put(dataType, contractData);
            }
        }

        return result;
    }

    public CalculationResultGroup processCalculation(CalculationTarget calculationTarget, CalculationContext ctx) {
        try {
            log.debug("Processing contract calculation for contractId: {}", calculationTarget.contractId());
            List<CalculationResult<?>> results = new ArrayList<>();

            // 월정액 계산
            for (var monthlyFeeCalculator : monthlyFeeCalculators) {
                processMonthlyFeeCalculator(monthlyFeeCalculator, calculationTarget, ctx, results);
            }
            log.debug("Processed 월정액 {} calculation results for contractId: {}", results.size(), calculationTarget.contractId());

            // 일회성 과금 계산
            for (var oneTimeChargeCalculator : oneTimeChargeCalculators) {
                processOneTimeChargeCalculator(oneTimeChargeCalculator, calculationTarget, ctx, results);
            }
            log.debug("Processed 일회성 {} calculation results for contractId: {}", results.size(), calculationTarget.contractId());

            // 구간분리
            results = new ArrayList<>(calculationResultProrater.prorate(ctx, results, calculationTarget.discounts()));
            log.debug("Processed 구간분리 {} calculation results for contractId: {}", results.size(), calculationTarget.contractId());

            // 할인
            results.addAll(discountCalculator.process(ctx, results, calculationTarget.discounts()));
            log.debug("Processed 할인 {} calculation results for contractId: {}", results.size(), calculationTarget.contractId());

            // 구간 합치기
            results = new ArrayList<>(calculationResultProrater.consolidate(results));
            log.debug("Processed 합치기 {} calculation results for contractId: {}", results.size(), calculationTarget.contractId());

            // VAT 계산 (기존 결과 기반)
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
     * MonthlyFeeCalculator 타입 안전 처리
     */
    //@SuppressWarnings("unchecked")
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
     * OneTimeChargeCalculator 타입 안전 처리
     */
    //@SuppressWarnings("unchecked")
    private <T extends OneTimeChargeDomain> void processOneTimeChargeCalculator(
            OneTimeChargeCalculator<T> calculator,
            CalculationTarget target,
            CalculationContext ctx,
            List<CalculationResult<?>> results) {
        Class<T> inputType = calculator.getDomainType();
        List<T> inputData = target.getOneTimeChargeData(inputType);
        results.addAll(process(inputData, calculator::process, ctx));
    }

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

    @Override
    public List<CalculationResultGroup> calculate(List<Long> contractIds, CalculationContext ctx) {
        return loadCalculationTargets(contractIds, ctx).stream()
                .map(calculationTarget -> processCalculation(calculationTarget, ctx))
                .toList();
    }
}
