package me.realimpact.telecom.calculation.application.monthlyfee;

import java.util.List;
import java.util.Optional;

import org.springframework.stereotype.Service;

import lombok.RequiredArgsConstructor;
import me.realimpact.telecom.calculation.api.CalculationRequest;
import me.realimpact.telecom.calculation.domain.monthlyfee.AdditionalBillingFactors;
import me.realimpact.telecom.calculation.domain.monthlyfee.Contract;
import me.realimpact.telecom.calculation.domain.monthlyfee.DefaultPeriod;
import me.realimpact.telecom.calculation.domain.monthlyfee.MonthlyFeeCalculationResult;
import me.realimpact.telecom.calculation.domain.monthlyfee.Product;
import me.realimpact.telecom.calculation.domain.monthlyfee.ProratedPeriod;
import me.realimpact.telecom.calculation.domain.monthlyfee.ProratedPeriodBuilder;
import me.realimpact.telecom.calculation.domain.monthlyfee.Suspension;
import me.realimpact.telecom.calculation.port.out.ContractQueryPort;
import me.realimpact.telecom.calculation.port.out.ProductQueryPort;

@Service
@RequiredArgsConstructor
public class MonthlyFeeCalculator {

    private final ContractQueryPort contractQueryPort;
    private final ProductQueryPort productQueryPort;
    private final AdditionalBillingFactorFactory additionalBillingFactorFactory;

    public List<MonthlyFeeCalculationResult> calculate(CalculationRequest context) {
        // 청구기간
        DefaultPeriod billingPeriod = DefaultPeriod.of(context.billingStartDate(), context.billingEndDate());

        // 계약정보
        Contract contract = contractQueryPort.findByContractId(context.contractId());
        
        // 정지 이력
        List<Suspension> suspensions = contractQueryPort.findSuspensionHistory(context.contractId());
        
        // 상품정보
        List<Product> products = productQueryPort.findByContractId(context.contractId());
        
        // 전용회선이라 가정
        List<AdditionalBillingFactors> billingFactors = additionalBillingFactorFactory.create(contract);
        
        // 구간 분리
        ProratedPeriodBuilder proratedPeriodBuilder = new ProratedPeriodBuilder(contract, products, suspensions, billingFactors, billingPeriod);
        List<ProratedPeriod> proratedPeriods = proratedPeriodBuilder.build();

        // 계산 결과 생성
        return proratedPeriods.stream()
                .map(proratedPeriod -> proratedPeriod.calculate())
                .filter(Optional::isPresent)
                .map(Optional::get)
                .toList();
    }
}
