package me.realimpact.telecom.calculation.application;

import lombok.RequiredArgsConstructor;
import me.realimpact.telecom.calculation.api.CalculationCommandUseCase;
import me.realimpact.telecom.calculation.api.CalculationRequest;
import me.realimpact.telecom.calculation.api.CalculationResponse;
import me.realimpact.telecom.calculation.application.monthlyfee.BaseFeeCalculator;
import me.realimpact.telecom.calculation.application.onetimecharge.policy.DeviceInstallmentCalculator;
import me.realimpact.telecom.calculation.application.onetimecharge.policy.InstallationFeeCalculator;
import me.realimpact.telecom.calculation.application.vat.VatCalculator;
import me.realimpact.telecom.calculation.domain.CalculationResult;
import me.realimpact.telecom.calculation.domain.CalculationContext;

import java.util.ArrayList;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class CalculationCommandService implements CalculationCommandUseCase {

    // 장점이 있을까..
    //private final List<? extends Calculator<?>> calculators;

    private final BaseFeeCalculator baseFeeCalculator;
    private final InstallationFeeCalculator installationFeeCalculator;
    private final DeviceInstallmentCalculator deviceInstallmentCalculator;
    private final VatCalculator vatCalculator;

    @Transactional
    @Override
    public List<CalculationResponse> calculate(CalculationRequest calculationRequest) {
        CalculationContext ctx = new CalculationContext(
            calculationRequest.billingStartDate(),
            calculationRequest.billingEndDate(),
            calculationRequest.billingCalculationType(),
            calculationRequest.billingCalculationPeriod()
        );
        List<Long> contractIds = calculationRequest.contractIds();
        
        // 각 계산기 실행
        List<CalculationResult<?>> results = new ArrayList<>();
        results.addAll(baseFeeCalculator.execute(ctx, contractIds));
        results.addAll(installationFeeCalculator.execute(ctx, contractIds));
        results.addAll(deviceInstallmentCalculator.execute(ctx, contractIds));
        
        // VAT 계산 (기존 결과 기반)
        List<CalculationResult<?>> vatResults = vatCalculator.calculateVat(ctx, results);
        results.addAll(vatResults);
        
        return results.stream()
            .map(calculationResult ->
                new CalculationResponse(calculationResult.getContractId(), calculationResult.getFee().longValue())
            )
            .toList();
    }

}
