package me.realimpact.telecom.calculation.domain.monthlyfee;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public class AdditionalBillingFactors extends Temporal {
    private final Map<String, String> factors;

    private final LocalDateTime effectiveStartDateTime;
    private final LocalDateTime effectiveEndDateTime;

    private final LocalDate subscribedAt;
    private final Optional<LocalDate> activatedAt;    

    @Override
    public LocalDate getStartDate() {
        return List.of(
            effectiveStartDateTime.toLocalDate(), 
            activatedAt.orElse(LocalDate.MIN)
        ).stream().max(Comparator.naturalOrder()).orElseThrow();
    }

    @Override
    public LocalDate getEndDate() {
        return effectiveEndDateTime.toLocalDate();
    }
}