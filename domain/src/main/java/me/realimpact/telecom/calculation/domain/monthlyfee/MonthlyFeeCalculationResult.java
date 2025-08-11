package me.realimpact.telecom.calculation.domain.monthlyfee;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

public record MonthlyFeeCalculationResult(
    long contractId,
    LocalDate billingStartDate,
    LocalDate billingEndDate,
    List<MonthlyFeeCalculationResultItem> items) {
}
