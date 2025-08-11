package me.realimpact.telecom.calculation.application;

import java.util.List;

import org.springframework.stereotype.Service;

import lombok.RequiredArgsConstructor;
import me.realimpact.telecom.calculation.api.CalculationCommandUseCase;
import me.realimpact.telecom.calculation.api.CalculationRequest;
import me.realimpact.telecom.calculation.api.CalculationResult;
import me.realimpact.telecom.calculation.application.monthlyfee.MonthlyFeeCalculator;
import me.realimpact.telecom.calculation.domain.monthlyfee.MonthlyFeeCalculationResult;

@Service
@RequiredArgsConstructor
public class CalculationUseCase implements CalculationCommandUseCase {

    private final MonthlyFeeCalculator monthlyFeeCalculator;

    @Override
    public CalculationResult calculate(CalculationRequest context) {
        CalculationResult result = new CalculationResult();
        
        List<MonthlyFeeCalculationResult> monthlyFeeCalculationResult = monthlyFeeCalculator.calculate(context);

        return result;
    }

}
