package me.realimpact.telecom.calculation.application;

import me.realimpact.telecom.calculation.application.discount.DiscountCalculator;
import me.realimpact.telecom.calculation.application.monthlyfee.MonthlyFeeDataLoader;
import me.realimpact.telecom.calculation.application.onetimecharge.OneTimeChargeDataLoader;
import me.realimpact.telecom.calculation.domain.CalculationContext;
import me.realimpact.telecom.calculation.domain.discount.ContractDiscounts;
import me.realimpact.telecom.calculation.domain.monthlyfee.MonthlyChargeDomain;
import me.realimpact.telecom.calculation.domain.onetimecharge.OneTimeChargeDomain;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * 요금 계산에 필요한 모든 데이터를 로드하고, 각 계약별로 계산 대상을 생성하는 클래스.
 */
@Component
public class CalculationTargetLoader {
    private final DiscountCalculator discountCalculator;
    private final Map<Class<? extends MonthlyChargeDomain>, MonthlyFeeDataLoader<? extends MonthlyChargeDomain>> monthlyFeeDataLoaderMap;
    private final Map<Class<? extends OneTimeChargeDomain>, OneTimeChargeDataLoader<? extends OneTimeChargeDomain>> oneTimeChargeDataLoaderMap;

    /**
     * 생성자. 필요한 데이터 로더와 계산기를 주입받는다.
     * @param discountCalculator 할인 계산기
     * @param monthlyFeeDataLoaders 월정액 데이터 로더 목록
     * @param oneTimeChargeDataLoaders 일회성 요금 데이터 로더 목록
     */
    public CalculationTargetLoader(
            DiscountCalculator discountCalculator,
            List<MonthlyFeeDataLoader<? extends MonthlyChargeDomain>> monthlyFeeDataLoaders,
            List<OneTimeChargeDataLoader<? extends OneTimeChargeDomain>> oneTimeChargeDataLoaders) {
        this.discountCalculator = discountCalculator;
        this.monthlyFeeDataLoaderMap = monthlyFeeDataLoaders.stream()
                .collect(Collectors.toMap(
                        MonthlyFeeDataLoader::getDomainType,
                        Function.identity(),
                        (oldValue, newValue) -> oldValue,
                        LinkedHashMap::new
                ));
        this.oneTimeChargeDataLoaderMap = oneTimeChargeDataLoaders.stream()
                .collect(Collectors.toMap(
                        OneTimeChargeDataLoader::getDomainType,
                        Function.identity(),
                        (oldValue, newValue) -> oldValue,
                        LinkedHashMap::new
                ));
    }

    /**
     * 계약 ID 목록에 대해 요금 계산에 필요한 모든 데이터를 로드하고,
     * 각 계약별로 계산 대상을 나타내는 CalculationTarget 객체를 생성한다.
     * @param contractIds 계약 ID 목록
     * @param ctx 계산 컨텍스트
     * @return 계산 대상 목록
     */
    public List<CalculationTarget> load(List<Long> contractIds, CalculationContext ctx) {
        var monthlyFeeDataByType = loadMonthlyFeeDataByType(contractIds, ctx);
        var oneTimeChargeDataByType = loadOneTimeChargeDataByType(contractIds, ctx);
        var contractDiscountsMap = discountCalculator.read(ctx, contractIds);

        List<CalculationTarget> calculationTargets = new ArrayList<>();
        for (Long contractId : contractIds) {
            var monthlyFeeDataForContract = groupMonthlyFeeDataByContract(contractId, monthlyFeeDataByType);
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
        return calculationTargets;
    }

    /**
     * 등록된 모든 MonthlyFeeDataLoader를 실행하여 월정액 관련 데이터를 로딩한다.
     */
    private Map<Class<? extends MonthlyChargeDomain>, Map<Long, List<? extends MonthlyChargeDomain>>>
        loadMonthlyFeeDataByType(List<Long> contractIds, CalculationContext context) {
        return loadDataByType(
                contractIds,
                context,
                monthlyFeeDataLoaderMap,
                (loader, ctx) -> (Map) loader.read(contractIds, ctx)
        );
    }

    /**
     * 등록된 모든 OneTimeChargeDataLoader를 실행하여 일회성 요금 관련 데이터를 로딩한다.
     */
    private Map<Class<? extends OneTimeChargeDomain>, Map<Long, List<? extends OneTimeChargeDomain>>>
        loadOneTimeChargeDataByType(List<Long> contractIds, CalculationContext context) {
        return loadDataByType(
                contractIds,
                context,
                oneTimeChargeDataLoaderMap,
                (loader, ctx) -> (Map) loader.read(contractIds, ctx)
        );
    }

    /**
     * 제네릭을 사용하여 다양한 타입의 데이터 로더로부터 데이터를 로딩하는 공통 메서드.
     */
    private <T, L> Map<Class<? extends T>, Map<Long, List<? extends T>>>
        loadDataByType(List<Long> contractIds, CalculationContext context, Map<Class<? extends T>, L> dataLoaderMap, BiFunction<L, CalculationContext, Map<Long, List<? extends T>>> readFunction) {
        Map<Class<? extends T>, Map<Long, List<? extends T>>> result = new HashMap<>();
        for (var entry : dataLoaderMap.entrySet()) {
            var dataType = entry.getKey();
            var loader = entry.getValue();
            Map<Long, List<? extends T>> data = readFunction.apply(loader, context);
            if (!data.isEmpty()) {
                result.put(dataType, data);
            }
        }
        return result;
    }

    /**
     * 특정 계약 ID에 해당하는 월정액 데이터를 그룹화한다.
     */
    private Map<Class<? extends MonthlyChargeDomain>, List<? extends MonthlyChargeDomain>> groupMonthlyFeeDataByContract(
            Long contractId,
            Map<Class<? extends MonthlyChargeDomain>, Map<Long, List<? extends MonthlyChargeDomain>>> monthlyFeeDataByType) {
        return groupDataByContract(contractId, monthlyFeeDataByType);
    }

    /**
     * 특정 계약 ID에 해당하는 일회성 요금 데이터를 그룹화한다.
     */
    private Map<Class<? extends OneTimeChargeDomain>, List<? extends OneTimeChargeDomain>>
        groupOneTimeChargeDataByContract(
            Long contractId,
            Map<Class<? extends OneTimeChargeDomain>, Map<Long, List<? extends OneTimeChargeDomain>>> oneTimeChargeDataByType) {
        return groupDataByContract(contractId, oneTimeChargeDataByType);
    }

    /**
     * 제네릭을 사용하여 타입별로 로드된 데이터를 특정 계약 ID에 맞게 그룹화하는 공통 메서드.
     */
    private <T> Map<Class<? extends T>, List<? extends T>> groupDataByContract(
            Long contractId,
            Map<Class<? extends T>, Map<Long, List<? extends T>>> dataByType) {
        Map<Class<? extends T>, List<? extends T>> result = new HashMap<>();
        for (var entry : dataByType.entrySet()) {
            var dataType = entry.getKey();
            var dataByContract = entry.getValue();
            List<? extends T> contractData = dataByContract.get(contractId);
            if (contractData != null && !contractData.isEmpty()) {
                result.put(dataType, contractData);
            }
        }
        return result;
    }
}