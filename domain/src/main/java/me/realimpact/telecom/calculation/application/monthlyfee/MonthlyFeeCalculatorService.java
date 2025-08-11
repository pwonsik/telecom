package me.realimpact.telecom.calculation.application.monthlyfee;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import org.springframework.stereotype.Service;

import lombok.RequiredArgsConstructor;
import me.realimpact.telecom.calculation.api.BillingCalculationPeriod;
import me.realimpact.telecom.calculation.api.CalculationRequest;
import me.realimpact.telecom.calculation.domain.monthlyfee.AdditionalBillingFactors;
import me.realimpact.telecom.calculation.domain.monthlyfee.Contract;
import me.realimpact.telecom.calculation.domain.monthlyfee.DefaultPeriod;
import me.realimpact.telecom.calculation.domain.monthlyfee.MonthlyFeeCalculationResult;
import me.realimpact.telecom.calculation.domain.monthlyfee.Product;
import me.realimpact.telecom.calculation.domain.monthlyfee.ProratedPeriod;
import me.realimpact.telecom.calculation.domain.monthlyfee.Suspension;
import me.realimpact.telecom.calculation.port.out.ContractQueryPort;

@Service
@RequiredArgsConstructor
public class MonthlyFeeCalculatorService {

    private final ContractQueryPort contractQueryPort;

    public List<MonthlyFeeCalculationResult> calculate(CalculationRequest context) {
        // 청구기간 말일까지 계산해야 하는 유형 (정기청구나 전당월의 전월. 미래요금조회 등)은 종료일에 하루를 더해준다.
        LocalDate billingEndDate = context.billingEndDate();
        if (context.billingCalculationType().includeBillingEndDate() ||
            context.billingCalculationPeriod() == BillingCalculationPeriod.PRE_BILLING_PREVIOUS_MONTH) {
            billingEndDate = context.billingEndDate().plusDays(1);
        }

        // 청구기간
        DefaultPeriod billingPeriod = DefaultPeriod.of(context.billingStartDate(), billingEndDate);

        // 계약정보
        Contract contract = contractQueryPort.findContractWithProductsChargeItemsAndSuspensions(
                context.contractId(), billingPeriod.getStartDate(), billingPeriod.getEndDate());

        //List<AdditionalBillingFactors> billingFactors = additionalBillingFactorFactory.create(contract);
        
        // 구간 분리 - Contract가 직접 담당
        List<ProratedPeriod> proratedPeriods = contract.buildProratedPeriods(billingPeriod);

        // 계산 결과 생성
        return proratedPeriods.stream()
                .map(ProratedPeriod::calculate)
                .filter(Optional::isPresent)
                .map(Optional::get)
                .toList();
    }
}
