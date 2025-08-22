package me.realimpact.telecom.calculation.domain;

import me.realimpact.telecom.calculation.domain.monthlyfee.Suspension;

import java.math.BigDecimal;
import java.time.LocalDate;

public record CalculationResult(
        Long contractId,
    LocalDate billingStartDate,
    LocalDate billingEndDate,
    String productOfferingId,
    String chargeItemId,
    LocalDate effectiveStartDate,
    LocalDate effectiveEndDate,
    Suspension.SuspensionType suspensionType,
    BigDecimal fee) {
}
