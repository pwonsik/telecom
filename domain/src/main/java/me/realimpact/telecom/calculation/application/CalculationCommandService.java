package me.realimpact.telecom.calculation.application;

import org.springframework.stereotype.Service;

import lombok.RequiredArgsConstructor;
import me.realimpact.telecom.calculation.api.CalculationCommandUseCase;
import me.realimpact.telecom.calculation.api.CalculationRequest;
import me.realimpact.telecom.calculation.application.monthlyfee.BaseFeeCalculator;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class CalculationCommandService implements CalculationCommandUseCase {

    private final BaseFeeCalculator monthlyFeeCalculatorService;

    @Transactional
    @Override
    public CalculationResult calculate(CalculationRequest calculationRequest) {
        CalculationResult result = new CalculationResult();
        
        monthlyFeeCalculatorService.calculate(calculationRequest);

        return result;
    }

}
