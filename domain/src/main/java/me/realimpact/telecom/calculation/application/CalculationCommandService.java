package me.realimpact.telecom.calculation.application;

import lombok.RequiredArgsConstructor;
import me.realimpact.telecom.calculation.api.CalculationCommandUseCase;
import me.realimpact.telecom.calculation.api.CalculationRequest;
import me.realimpact.telecom.calculation.api.CalculationResponse;
import me.realimpact.telecom.calculation.domain.CalculationContext;
import me.realimpact.telecom.calculation.domain.CalculationResult;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class CalculationCommandService implements CalculationCommandUseCase {

    private final List<Calculator<?>> calculators;

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
        List<CalculationResult> calculationResults =
            calculators.stream()
                .flatMap(calculator -> calculator.execute(ctx, contractIds).stream())
                .toList();
        return calculationResults.stream()
            .map(calculationResult ->
                new CalculationResponse(calculationResult.contractId(), calculationResult.fee().longValue())
            )
            .toList();
    }

}
