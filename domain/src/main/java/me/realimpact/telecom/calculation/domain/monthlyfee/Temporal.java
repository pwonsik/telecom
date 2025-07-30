package me.realimpact.telecom.calculation.domain.monthlyfee;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;

public abstract class Temporal {
    public abstract LocalDate getCalculationStartDate();
    public abstract LocalDate getCalculationEndDate();

    /**
     * Temporal의 기간이 주어진 period와 중첩되는지 확인합니다.
     * 중첩되면 true, 아니면 false를 반환합니다.
     */
    public boolean overlapsWith(Temporal temporal) {
        return !this.getCalculationEndDate().isBefore(temporal.getCalculationStartDate()) 
            && !this.getCalculationStartDate().isAfter(temporal.getCalculationEndDate());
    }

    public long getUsageDays() {
        return ChronoUnit.DAYS.between(getCalculationStartDate(), getCalculationEndDate());
    } 
}
