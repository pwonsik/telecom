
package me.realimpact.telecom.calculation.domain.monthlyfee;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.RequiredArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;

@RequiredArgsConstructor
public class Product extends Temporal {
    private final Long contractId;
    private final ProductOffering productOffering;

    private final LocalDateTime effectiveStartDateTime;
    private final LocalDateTime effectiveEndDateTime;

    private final LocalDate subscribedAt;
    private final Optional<LocalDate> activatedAt;
    private final Optional<LocalDate> terminatedAt;

    private final Temporal billingPeriod;

    @Override
    public LocalDate getCalculationStartDate() {
        return List.of(
            effectiveStartDateTime.toLocalDate(), 
            subscribedAt, 
            activatedAt.orElse(LocalDate.MIN),
            billingPeriod.getCalculationStartDate()
        ).stream().max(Comparator.naturalOrder()).orElseThrow();
    }

    @Override
    public LocalDate getCalculationEndDate() {
        return List.of(
            effectiveEndDateTime.toLocalDate(), 
            terminatedAt.orElse(LocalDate.MAX),
            billingPeriod.getCalculationEndDate()
        ).stream().min(Comparator.naturalOrder()).orElseThrow();
    }

    public ProductOffering getProductOffering() {
        return productOffering;
    }
}
