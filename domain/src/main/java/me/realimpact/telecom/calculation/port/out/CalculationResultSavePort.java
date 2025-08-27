package me.realimpact.telecom.calculation.port.out;

import me.realimpact.telecom.calculation.domain.CalculationContext;
import me.realimpact.telecom.calculation.domain.CalculationResult;

import java.util.List;

public interface CalculationResultSavePort {
    void save(CalculationContext calculationContext, List<CalculationResult> results);
}
