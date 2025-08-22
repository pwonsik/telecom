package me.realimpact.telecom.calculation.application.monthlyfee;

import java.time.LocalDate;
import java.util.List;
import java.util.Objects;

import me.realimpact.telecom.calculation.application.Calculator;
import me.realimpact.telecom.calculation.domain.CalculationContext;
import me.realimpact.telecom.calculation.domain.CalculationResult;
import me.realimpact.telecom.calculation.domain.monthlyfee.*;
import me.realimpact.telecom.calculation.infrastructure.adapter.mybatis.CalculationResultMapper;
import me.realimpact.telecom.calculation.infrastructure.converter.CalculationResultFlattener;
import me.realimpact.telecom.calculation.port.out.CalculationResultSavePort;
import org.springframework.stereotype.Service;

import lombok.RequiredArgsConstructor;
import me.realimpact.telecom.calculation.api.BillingCalculationPeriod;
import me.realimpact.telecom.calculation.api.CalculationRequest;
import me.realimpact.telecom.calculation.port.out.ContractQueryPort;

@Service
@RequiredArgsConstructor
public class BaseFeeCalculator implements Calculator<ContractWithProductsAndSuspensions> {

    private final ContractQueryPort contractQueryPort;
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
    public List<ContractWithProductsAndSuspensions> read(CalculationContext calculationContext, List<Long> contractIds) {
        DefaultPeriod billingPeriod = createBillingPeriod(calculationContext);
        return contractQueryPort.findContractsAndProductInventoriesByContractIds(
            context.contractIds(), billingPeriod.getStartDate(), billingPeriod.getEndDate()
        );
    }

    @Override
    public List<CalculationResult> process(CalculationContext calculationContext, ContractWithProductsAndSuspensions contractWithProductInventoriesAndSuspensions) {
        return contractWithProductInventoriesAndSuspensions.buildProratedPeriods().stream()
            .map(proratedPeriod -> proratedPeriod.calculate(calculationContext))
            .filter(Objects::nonNull)
            .toList();
    }

    @Override
    public void write(CalculationContext calculationContext, List<CalculationResult> output) {
        calculationResultSavePort.save(calculationContext, output);
    }

    @Override
    public void post(CalculationContext calculationContext, List<CalculationResult> output) {
        // do nothing
    }

    /**
     * 테스트를 위한 계산 메서드 (결과 반환)
     */
    public List<CalculationResult> calculateAndReturn(CalculationContext calculationContext, List<Long> contractIds) {
        return read(calculationContext, contractIds).stream().flatMap(obj -> process(calculationContext, obj).stream()).toList();
    }
}
