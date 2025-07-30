package me.realimpact.telecom.calculation.domain.monthlyfee;

import java.time.LocalDate;

public class Period extends Temporal {
    private final LocalDate startDate;
    private final LocalDate endDate;

    private Period(LocalDate startDate, LocalDate endDate) {
        this.startDate = startDate;
        this.endDate = endDate;
    }

    public static Period of(LocalDate startDate, LocalDate endDate) {
        return new Period(startDate, endDate);
    }

    @Override
    public LocalDate getCalculationStartDate() {
        return startDate;
    }

    @Override
    public LocalDate getCalculationEndDate() {
        return endDate;
    }
}
