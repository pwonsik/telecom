package me.realimpact.telecom.calculation.application.monthlyfee;

import lombok.RequiredArgsConstructor;
import me.realimpact.telecom.calculation.api.BillingCalculationPeriod;
import me.realimpact.telecom.calculation.application.Calculator;
import me.realimpact.telecom.calculation.domain.CalculationContext;
import me.realimpact.telecom.calculation.domain.CalculationResult;
import me.realimpact.telecom.calculation.domain.monthlyfee.ContractWithProductsAndSuspensions;
import me.realimpact.telecom.calculation.domain.monthlyfee.DefaultPeriod;
import me.realimpact.telecom.calculation.port.out.CalculationResultSavePort;
import me.realimpact.telecom.calculation.port.out.ProductQueryPort;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Order(10)
public class BaseFeeCalculator implements Calculator<ContractWithProductsAndSuspensions> {

    private final ProductQueryPort productQueryPort;
    private final CalculationResultSavePort calculationResultSavePort;

    private DefaultPeriod createBillingPeriod(CalculationContext calculationContext) {
        // 청구기간 말일까지 계산해야 하는 유형 (정기청구나 전당월의 전월. 미래요금조회 등)은 종료일에 하루를 더해준다.
        LocalDate billingEndDate = calculationContext.billingEndDate();
        if (calculationContext.billingCalculationType().includeBillingEndDate() ||
                calculationContext.billingCalculationPeriod() == BillingCalculationPeriod.PRE_BILLING_PREVIOUS_MONTH) {
            billingEndDate = calculationContext.billingEndDate().plusDays(1);
        }
        return DefaultPeriod.of(calculationContext.billingStartDate(), billingEndDate);
    }

    @Override
    public Map<Long, List<ContractWithProductsAndSuspensions>> read(CalculationContext ctx, List<Long> contractIds) {
        DefaultPeriod billingPeriod = createBillingPeriod(ctx);
        return productQueryPort.findContractsAndProductInventoriesByContractIds(
            contractIds, billingPeriod.getStartDate(), billingPeriod.getEndDate()
        ).stream().collect(Collectors.groupingBy(ContractWithProductsAndSuspensions::getContractId));
    }

    @Override
    public List<CalculationResult> process(
        CalculationContext ctx,
        ContractWithProductsAndSuspensions contractWithProductInventoriesAndSuspensions
    ) {
        return contractWithProductInventoriesAndSuspensions.buildProratedPeriods().stream()
            .map(proratedPeriod -> proratedPeriod.calculate(ctx))
            .filter(Objects::nonNull)
            .toList();
    }

    @Override
    public void write(CalculationContext ctx, List<CalculationResult> output) {
        calculationResultSavePort.save(ctx, output);
    }

    @Override
    public void post(CalculationContext ctx, List<CalculationResult> output) {
        // do nothing
    }

    /**
     * 테스트를 위한 계산 메서드 (결과 반환)
     */
    public List<CalculationResult> calculateAndReturn(CalculationContext calculationContext, List<Long> contractIds) {
        return read(calculationContext, contractIds).values().stream()
                .flatMap(obj -> process(calculationContext, obj.get(0)).stream()).toList();
    }
}
