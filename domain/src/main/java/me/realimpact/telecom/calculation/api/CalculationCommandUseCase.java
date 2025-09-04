package me.realimpact.telecom.calculation.api;

import me.realimpact.telecom.calculation.application.CalculationTarget;
import me.realimpact.telecom.calculation.domain.CalculationContext;

import java.util.List;

public interface CalculationCommandUseCase {
    List<CalculationResultGroup> calculate(List<Long> contractIds, CalculationContext ctx);
}
