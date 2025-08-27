package me.realimpact.telecom.calculation.domain;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import me.realimpact.telecom.calculation.domain.monthlyfee.Suspension;

import java.math.BigDecimal;
import java.time.LocalDate;

@Getter
@Setter
@RequiredArgsConstructor
public class CalculationResult {
    private final Long contractId;
    private final LocalDate billingStartDate;
    private final LocalDate billingEndDate;
    private final String productOfferingId;
    private final String chargeItemId;
    private final String revenueItemId;
    private final LocalDate effectiveStartDate;
    private final LocalDate effectiveEndDate;
    private final Suspension.SuspensionType suspensionType;
    private final BigDecimal fee;
    private final Object domain;
}