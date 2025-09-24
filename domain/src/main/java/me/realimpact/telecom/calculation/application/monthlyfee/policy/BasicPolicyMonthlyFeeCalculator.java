package me.realimpact.telecom.calculation.application.monthlyfee.policy;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import me.realimpact.telecom.calculation.api.BillingCalculationPeriod;
import me.realimpact.telecom.calculation.application.monthlyfee.MonthlyFeeCalculator;
import me.realimpact.telecom.calculation.application.monthlyfee.MonthlyFeeDataLoader;
import me.realimpact.telecom.calculation.domain.CalculationContext;
import me.realimpact.telecom.calculation.domain.CalculationResult;
import me.realimpact.telecom.calculation.domain.monthlyfee.ContractWithProductsAndSuspensions;
import me.realimpact.telecom.calculation.domain.monthlyfee.DefaultPeriod;
import me.realimpact.telecom.calculation.domain.monthlyfee.MonthlyChargeDomain;
import me.realimpact.telecom.calculation.infrastructure.adapter.ProductQueryPortResolver;
import me.realimpact.telecom.calculation.port.out.CalculationResultSavePort;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 기본 정책 기반 월정액 계산기
 * MonthlyFeeDataLoader와 MonthlyFeeCalculator 인터페이스를 모두 구현
 */
@Component
@RequiredArgsConstructor
@Slf4j
@Order(10)
public class BasicPolicyMonthlyFeeCalculator implements
        MonthlyFeeDataLoader<ContractWithProductsAndSuspensions>,
        MonthlyFeeCalculator<ContractWithProductsAndSuspensions> {

    private final ProductQueryPortResolver productQueryPortResolver;
    private final CalculationResultSavePort calculationResultSavePort;

    private DefaultPeriod createBillingPeriod(CalculationContext ctx) {
        // 청구기간 말일까지 계산해야 하는 유형 (정기청구나 전당월의 전월. 미래요금조회 등)은 종료일에 하루를 더해준다.
        LocalDate billingEndDate = ctx.billingEndDate();
        if (ctx.billingCalculationType().includeBillingEndDate() ||
                ctx.billingCalculationPeriod() == BillingCalculationPeriod.PRE_BILLING_PREVIOUS_MONTH) {
            billingEndDate = ctx.billingEndDate().plusDays(1);
        }
        return DefaultPeriod.of(ctx.billingStartDate(), billingEndDate);
    }

    // Calculator 인터페이스 구현 (기존 방식 유지)
    @Override
    public Map<Long, List<MonthlyChargeDomain>> read(List<Long> contractIds, CalculationContext ctx) {
        DefaultPeriod billingPeriod = createBillingPeriod(ctx);
        Map<Long, List<ContractWithProductsAndSuspensions>> specificData = productQueryPortResolver.getProductQueryPort(ctx.billingCalculationType())
                .findContractsAndProductInventoriesByContractIds(
                    contractIds, billingPeriod.getStartDate(), billingPeriod.getEndDate()
            ).stream().collect(Collectors.groupingBy(ContractWithProductsAndSuspensions::getContractId));

        Map<Long, List<MonthlyChargeDomain>> result = new HashMap<>();
        specificData.forEach((contractId, monthlyChargeDomains) ->
                result.put(contractId, new ArrayList<>(monthlyChargeDomains)));
        return result;
    }

    @Override
    public List<CalculationResult<ContractWithProductsAndSuspensions>> process(
        CalculationContext ctx,
        ContractWithProductsAndSuspensions contractWithProductInventoriesAndSuspensions
    ) {
        var result = contractWithProductInventoriesAndSuspensions.buildProratedPeriods().stream()
            .map(proratedPeriod -> {
                var data = proratedPeriod.calculateProratedData();
                return new CalculationResult<ContractWithProductsAndSuspensions>(
                    data.contractId(),
                    ctx.billingStartDate(),
                    ctx.billingEndDate(),
                    data.productOfferingId(),
                    data.chargeItemId(),
                    data.revenueItemId(),
                    data.periodStartDate(),
                    data.periodEndDate(),
                    data.suspensionType().orElse(null),
                    data.proratedFee(),
                    data.balance(),
                    null,
                    null // BasicPolicyMonthlyFeeCalculator는 후처리가 필요 없음
                );
            })
            .filter(Objects::nonNull)
            .toList();
        return result;
    }

    // MonthlyFeeDataLoader 인터페이스 구현
    @Override
    public Class<ContractWithProductsAndSuspensions> getDataType() {
        return ContractWithProductsAndSuspensions.class;
    }


    // MonthlyFeeCalculator 인터페이스 구현
    @Override
    public Class<ContractWithProductsAndSuspensions> getInputType() {
        return ContractWithProductsAndSuspensions.class;
    }

    /**
     * 테스트를 위한 계산 메서드 (결과 반환)
     */
    public List<CalculationResult<ContractWithProductsAndSuspensions>> calculateAndReturn(CalculationContext calculationContext, List<Long> contractIds) {
        return read(contractIds, calculationContext).values().stream()
                .flatMap(obj -> process(calculationContext, (ContractWithProductsAndSuspensions) obj.get(0)).stream()).toList();
    }
}