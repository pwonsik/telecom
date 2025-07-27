package me.realimpact.telecom.calculation.application;

import org.springframework.stereotype.Service;

import lombok.RequiredArgsConstructor;
import me.realimpact.telecom.calculation.api.CalculationCommand;
import me.realimpact.telecom.calculation.api.CalculationRequest;
import me.realimpact.telecom.calculation.api.CalculationResult;
import me.realimpact.telecom.calculation.application.monthlyfee.MonthlyFeeCalculator;
import me.realimpact.telecom.calculation.domain.monthlyfee.MonthlyFeeCalculationResult;

@Service
@RequiredArgsConstructor
public class CalculationUseCase implements CalculationCommand {

    private final MonthlyFeeCalculator monthlyFeeCalculator;

    @Override
    public CalculationResult calculate(CalculationRequest context) {
        CalculationResult result = new CalculationResult();
        
        MonthlyFeeCalculationResult monthlyFeeCalculationResult = 
            monthlyFeeCalculator.calculate(context);

        return result;
    }

}
