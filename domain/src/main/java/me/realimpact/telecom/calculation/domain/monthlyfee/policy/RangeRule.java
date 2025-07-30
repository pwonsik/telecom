package me.realimpact.telecom.calculation.domain.monthlyfee.policy;

import java.math.BigDecimal;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
@Getter
public class RangeRule {
    private final long from;
    private final long to;
    private final BigDecimal fee;

    public boolean isInRange(long value) {
        return value >= from && value <= to;
    }
} 