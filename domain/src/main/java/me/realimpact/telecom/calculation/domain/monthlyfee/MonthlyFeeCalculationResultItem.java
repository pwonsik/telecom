package me.realimpact.telecom.calculation.domain.monthlyfee;

import java.math.BigDecimal;
import java.time.LocalDate;

public record MonthlyFeeCalculationResultItem(
    String productOfferingId,
    String monthlyChargeItemId,
    LocalDate effectiveStartDate,
    LocalDate effectiveEndDate,
    Suspension.SuspensionType suspensionType,
    BigDecimal fee) {
}
