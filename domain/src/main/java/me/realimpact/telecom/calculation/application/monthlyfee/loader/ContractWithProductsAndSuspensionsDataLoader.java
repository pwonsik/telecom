package me.realimpact.telecom.calculation.application.monthlyfee.loader;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import me.realimpact.telecom.calculation.application.monthlyfee.MonthlyFeeDataLoader;
import me.realimpact.telecom.calculation.application.service.BillingPeriodService;
import me.realimpact.telecom.calculation.domain.CalculationContext;
import me.realimpact.telecom.calculation.domain.monthlyfee.ContractWithProductsAndSuspensions;
import me.realimpact.telecom.calculation.domain.monthlyfee.DefaultPeriod;
import me.realimpact.telecom.calculation.domain.monthlyfee.MonthlyChargeDomain;
import me.realimpact.telecom.calculation.infrastructure.adapter.ProductQueryPortResolver;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * ContractWithProductsAndSuspensions 데이터 로딩 전담 클래스
 * 단일 책임: 계약 상품 정보 및 정지 정보 데이터 로딩
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class ContractWithProductsAndSuspensionsDataLoader implements MonthlyFeeDataLoader<ContractWithProductsAndSuspensions> {

    private final ProductQueryPortResolver productQueryPortResolver;
    private final BillingPeriodService billingPeriodService;

    @Override
    public Class<ContractWithProductsAndSuspensions> getDomainType() {
        return ContractWithProductsAndSuspensions.class;
    }

    @Override
    public Map<Long, List<? extends MonthlyChargeDomain>> read(List<Long> contractIds, CalculationContext ctx) {
        log.debug("Loading ContractWithProductsAndSuspensions data for {} contracts", contractIds.size());

        DefaultPeriod billingPeriod = billingPeriodService.createBillingPeriod(ctx);

        Map<Long, List<ContractWithProductsAndSuspensions>> specificData = productQueryPortResolver
                .getProductQueryPort(ctx.billingCalculationType())
                .findContractsAndProductInventoriesByContractIds(
                    contractIds, billingPeriod.getStartDate(), billingPeriod.getEndDate()
            ).stream()
            .collect(Collectors.groupingBy(ContractWithProductsAndSuspensions::getContractId));

        log.debug("Loaded ContractWithProductsAndSuspensions data for {} contracts", specificData.size());

        return specificData.entrySet().stream()
            .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
    }
}