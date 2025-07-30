package me.realimpact.telecom.calculation.domain.monthlyfee;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;

import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public class Suspension extends Temporal {

    private final LocalDateTime effectiveStartDateTime;
    private final LocalDateTime effectiveEndDateTime;
    private final SuspensionType suspensionType;

    private final Temporal billingPeriod;

    @Override
    public LocalDate getCalculationStartDate() {
        return List.of(
            effectiveStartDateTime.toLocalDate(),
            billingPeriod.getCalculationStartDate()
        ).stream().max(Comparator.naturalOrder()).orElseThrow();
    }

    @Override
    public LocalDate getCalculationEndDate() {
        return List.of(
            effectiveEndDateTime.toLocalDate(),
            billingPeriod.getCalculationEndDate()
        ).stream().min(Comparator.naturalOrder()).orElseThrow();
    }

    public SuspensionType getSuspensionType() {
        return suspensionType;
    }

    public static enum SuspensionType {
        TEMPORARY_SUSPENSION("F1","일시정지"),
        NON_PAYMENT_SUSPENSION("F3","미납정지");

        private final String code;
        private final String description;

        SuspensionType(String code, String description) {
            this.code = code;
            this.description = description;
        }
    }
}