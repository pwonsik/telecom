package me.realimpact.telecom.calculation.domain.monthlyfee;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;
import java.util.Map;

import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public class ExclusiveLineContractHistory implements ProvidesAdditionalBillingDetails {

    private final Long contractId;
    private final LocalDate activatedAt;

    private final LocalDateTime effectiveStartDateTime;
    private final LocalDateTime effectiveEndDateTime;

    private final Temporal billingPeriod;

    // 예시를 위함
    private final String lineSpeedCode;
    private final String lineOfferTypeCode;

    private LocalDate getCalculationStartDate() {
        return List.of(
            effectiveStartDateTime.toLocalDate(), 
            activatedAt,
            billingPeriod.getCalculationStartDate()
        ).stream().max(Comparator.naturalOrder()).orElseThrow();
    }

    private LocalDate getCalculationEndDate() {
        return List.of(
            effectiveEndDateTime.toLocalDate(), 
            billingPeriod.getCalculationEndDate()
        ).stream().min(Comparator.naturalOrder()).orElseThrow();
    }

    @Override
    public AdditionalBillingFactors getAdditionalBillingFactors() {
        return new AdditionalBillingFactors(
            Map.of(
                "lineSpeedCode", lineSpeedCode,
                "lineOfferTypeCode", lineOfferTypeCode
            ),
            getCalculationStartDate(),
            getCalculationEndDate()
        );
    }
}
