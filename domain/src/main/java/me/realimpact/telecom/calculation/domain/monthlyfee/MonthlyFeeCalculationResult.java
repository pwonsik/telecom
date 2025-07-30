package me.realimpact.telecom.calculation.domain.monthlyfee;

import java.math.BigDecimal;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
@Getter
public class MonthlyFeeCalculationResult {
    private final ProrationPeriod prorationPeriod;
    private final BigDecimal fee;
}
