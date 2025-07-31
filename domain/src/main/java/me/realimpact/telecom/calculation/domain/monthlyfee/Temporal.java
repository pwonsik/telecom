package me.realimpact.telecom.calculation.domain.monthlyfee;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;

public abstract class Temporal {
    public abstract LocalDate getStartDate();
    public abstract LocalDate getEndDate();

    public LocalDate getEffectiveCalculationStartDate(Temporal billingPeriod) {
        LocalDate startDate = getStartDate();
        LocalDate billingStartDate = billingPeriod.getStartDate();
        return startDate.isAfter(billingStartDate) 
            ? startDate
            : billingStartDate;
    }

    public LocalDate getEffectiveCalculationEndDate(Temporal billingPeriod) {
        LocalDate endDate = getEndDate();
        LocalDate billingEndDate = billingPeriod.getEndDate();
        return endDate.isBefore(billingEndDate) 
            ? endDate
            : billingEndDate;
    }

    /**
     * Temporal의 기간이 주어진 period와 중첩되는지 확인합니다.
     * 중첩되면 true, 아니면 false를 반환합니다.
     */
    public boolean overlapsWith(Temporal temporal) {
        return this.getEndDate().isAfter(temporal.getStartDate()) 
            && this.getStartDate().isBefore(temporal.getEndDate());
    }

    public long getUsageDays() {
        return ChronoUnit.DAYS.between(getStartDate(), getEndDate())+1;
    } 
}
