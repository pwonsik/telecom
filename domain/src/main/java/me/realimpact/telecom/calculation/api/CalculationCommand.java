package me.realimpact.telecom.calculation.api;

public interface CalculationCommand {
    public CalculationResult calculate(CalculationRequest context);
}
