package me.realimpact.telecom.calculation.application.monthlyfee;

import java.time.LocalDate;
import java.util.List;
import java.util.Objects;

import me.realimpact.telecom.calculation.domain.monthlyfee.*;
import org.springframework.stereotype.Service;

import lombok.RequiredArgsConstructor;
import me.realimpact.telecom.calculation.api.BillingCalculationPeriod;
import me.realimpact.telecom.calculation.api.CalculationRequest;
import me.realimpact.telecom.calculation.port.out.ContractQueryPort;
import me.realimpact.telecom.calculation.port.out.CalculationResultSavePort;

@Service
@RequiredArgsConstructor
public class BaseFeeCalculator implements ProratedFeeCalculator<Contract, MonthlyFeeCalculationResult> {

    private final ContractQueryPort contractQueryPort;
    private final CalculationResultSavePort calculationResultSavePort;

    public DefaultPeriod createBillingPeriod(CalculationRequest context) {
        // 청구기간 말일까지 계산해야 하는 유형 (정기청구나 전당월의 전월. 미래요금조회 등)은 종료일에 하루를 더해준다.
        LocalDate billingEndDate = context.billingEndDate();
        if (context.billingCalculationType().includeBillingEndDate() ||
            context.billingCalculationPeriod() == BillingCalculationPeriod.PRE_BILLING_PREVIOUS_MONTH) {
            billingEndDate = context.billingEndDate().plusDays(1);
        }

        return DefaultPeriod.of(context.billingStartDate(), billingEndDate);
    }

    @Override
    public List<Contract> read(CalculationRequest context) {
        DefaultPeriod billingPeriod = createBillingPeriod(context);
        return contractQueryPort.findContractWithProductsChargeItemsAndSuspensions(
            context.contractId(), billingPeriod.getStartDate(), billingPeriod.getEndDate()
        );
    }

    @Override
    public MonthlyFeeCalculationResult process(Contract contract) {
        List<ProratedPeriod> proratedPeriods = contract.buildProratedPeriods();

        // 계산 결과 생성
        List<MonthlyFeeCalculationResultItem> monthlyFeeCalculationResultItems = proratedPeriods.stream()
            .map(ProratedPeriod::calculate)
            .filter(Objects::nonNull)
            .toList();

        return new MonthlyFeeCalculationResult(
            contract.getContractId(),
            contract.getBillingStartDate(),
            contract.getBillingEndDate(),
            monthlyFeeCalculationResultItems
        );
    }

    @Override
    public void write(List<MonthlyFeeCalculationResult> output) {
        calculationResultSavePort.batchSaveCalculationResults(output);
    }

    /**
     * 테스트를 위한 계산 메서드 (결과 반환)
     */
    public List<MonthlyFeeCalculationResult> calculateAndReturn(CalculationRequest request) {
        return read(request).stream().map(this::process).toList();
    }
}
