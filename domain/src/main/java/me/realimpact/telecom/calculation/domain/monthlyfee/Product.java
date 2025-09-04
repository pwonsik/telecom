
package me.realimpact.telecom.calculation.domain.monthlyfee;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.ToString;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.Optional;
import java.util.stream.Stream;

@Getter
@RequiredArgsConstructor
@ToString
public class Product extends Temporal {
    private final Long contractId;
    private final ProductOffering productOffering;

    private final LocalDateTime effectiveStartDateTime;
    private final LocalDateTime effectiveEndDateTime;

    private final LocalDate subscribedAt;
    private final Optional<LocalDate> activatedAt;
    private final Optional<LocalDate> terminatedAt;

    @Override
    public LocalDate getStartDate() {
        return Stream.of(
            effectiveStartDateTime.toLocalDate(),
            subscribedAt,
            activatedAt.orElse(LocalDate.MIN)
        ).max(Comparator.naturalOrder()).orElseThrow();
    }

    @Override
    public LocalDate getEndDate() {
        return Stream.of(
            effectiveEndDateTime.toLocalDate(),
            terminatedAt.orElse(LocalDate.MAX)
        ).min(Comparator.naturalOrder()).orElseThrow();
    }

}
