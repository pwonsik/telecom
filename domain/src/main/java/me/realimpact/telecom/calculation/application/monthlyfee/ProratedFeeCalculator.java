package me.realimpact.telecom.calculation.application.monthlyfee;

import me.realimpact.telecom.calculation.api.CalculationRequest;

import java.util.List;

public interface ProratedFeeCalculator<I,O> {
    List<I> read(CalculationRequest request);
    O process(I input);
    void write(List<O> output);

    default void calculate(CalculationRequest request) {
        write(read(request).stream().map(this::process).toList());
    }
}
