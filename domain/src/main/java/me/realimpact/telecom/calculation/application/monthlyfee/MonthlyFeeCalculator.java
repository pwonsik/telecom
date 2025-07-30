package me.realimpact.telecom.calculation.application.monthlyfee;

import java.util.List;
import java.util.Optional;

import org.springframework.stereotype.Service;

import lombok.RequiredArgsConstructor;
import me.realimpact.telecom.calculation.api.CalculationRequest;
import me.realimpact.telecom.calculation.domain.monthlyfee.AdditionalBillingFactors;
import me.realimpact.telecom.calculation.domain.monthlyfee.Contract;
import me.realimpact.telecom.calculation.domain.monthlyfee.MonthlyChargingPolicy;
import me.realimpact.telecom.calculation.domain.monthlyfee.MonthlyFeeCalculationResult;
import me.realimpact.telecom.calculation.domain.monthlyfee.Period;
import me.realimpact.telecom.calculation.domain.monthlyfee.Product;
import me.realimpact.telecom.calculation.domain.monthlyfee.ProrationPeriod;
import me.realimpact.telecom.calculation.domain.monthlyfee.ProrationPeriodBuilder;
import me.realimpact.telecom.calculation.domain.monthlyfee.Suspension;
import me.realimpact.telecom.calculation.domain.monthlyfee.policy.MonthlyChargingPolicyFactory;
import me.realimpact.telecom.calculation.port.out.ContractQueryPort;
import me.realimpact.telecom.calculation.port.out.ProductQueryPort;

@Service
@RequiredArgsConstructor
public class MonthlyFeeCalculator {

    private final ContractQueryPort contractQueryPort;
    private final ProductQueryPort productQueryPort;
    private final MonthlyChargingPolicyFactory monthlyChargingPolicyFactory;

    public List<MonthlyFeeCalculationResult> calculate(CalculationRequest context) {
        // 청구기간
        Period billingPeriod = Period.of(context.billingStartDate(), context.billingEndDate());

        // 계약정보
        Contract contract = contractQueryPort.findByContractId(context.contractId());
        // 정지 이력
        List<Suspension> suspensions = contractQueryPort.findSuspensionHistory(context.contractId());
        // 상품정보
        List<Product> products = productQueryPort.findByContractId(context.contractId());
        // 추가 요금 정보
        List<AdditionalBillingFactors> billingFactors = contractQueryPort.findAdditionalBillingFactors(context.contractId());
        
        // 구간 분리
        ProrationPeriodBuilder prorationPeriodBuilder = new ProrationPeriodBuilder(contract, products, suspensions, billingFactors, billingPeriod);
        List<ProrationPeriod> prorationPeriods = prorationPeriodBuilder.build();

        // 계산 결과 생성
        return prorationPeriods.stream()
                .map(prorationPeriod -> {
                    MonthlyChargingPolicy policy = monthlyChargingPolicyFactory.getPolicy(prorationPeriod.getMonthlyChargeItem().getCalculationMethod());
                    return policy.calculate(prorationPeriod);
                })
                .filter(Optional::isPresent)
                .map(Optional::get)
                .toList();
    }
}
