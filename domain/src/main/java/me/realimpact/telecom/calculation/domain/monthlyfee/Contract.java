package me.realimpact.telecom.calculation.domain.monthlyfee;

import java.time.LocalDate;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;

import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public class Contract extends Temporal {
    private final Long contractId;
    
    private final LocalDate subscribedAt;
    private final LocalDate initiallySubscribedAt;
    private final Optional<LocalDate> terminatedAt;
    private final Optional<LocalDate> prefferedTerminationDate;
    private final Temporal billingPeriod;

    @Override
    public LocalDate getCalculationStartDate() {
        return List.of(
            subscribedAt, 
            initiallySubscribedAt,
            billingPeriod.getCalculationStartDate()
        ).stream().max(Comparator.naturalOrder()).orElseThrow();
    }

    @Override
    public LocalDate getCalculationEndDate() {
        return List.of(
            terminatedAt.orElse(LocalDate.MAX),
            prefferedTerminationDate.orElse(LocalDate.MAX),
            billingPeriod.getCalculationEndDate()
        ).stream().min(Comparator.naturalOrder()).orElseThrow();
    }
}
