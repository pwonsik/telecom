package me.realimpact.telecom.calculation.domain.monthlyfee.policy;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

import java.math.BigDecimal;

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