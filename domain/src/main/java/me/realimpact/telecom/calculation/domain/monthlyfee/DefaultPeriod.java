package me.realimpact.telecom.calculation.domain.monthlyfee;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

import java.time.LocalDate;

@Getter
@RequiredArgsConstructor(access = AccessLevel.PRIVATE)
public class DefaultPeriod extends Temporal{
    private final LocalDate startDate;
    private final LocalDate endDate;

    public static DefaultPeriod of(LocalDate startDate, LocalDate endDate) {
        return new DefaultPeriod(startDate, endDate);
    }
}
