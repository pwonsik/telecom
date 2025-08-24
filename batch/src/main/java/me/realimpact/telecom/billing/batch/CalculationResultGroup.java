package me.realimpact.telecom.billing.batch;

import me.realimpact.telecom.calculation.domain.CalculationResult;

import java.util.List;

public record CalculationResultGroup(
    List<CalculationResult> calculationResults
) {
}
