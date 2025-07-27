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

    @Override
    public LocalDate getStartDate() {
        return List.of(
            subscribedAt, 
            initiallySubscribedAt
        ).stream().max(Comparator.naturalOrder()).orElseThrow();
    }

    @Override
    public LocalDate getEndDate() {
        return terminatedAt.orElse(LocalDate.MAX);
    }
}
