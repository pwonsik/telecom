package me.realimpact.telecom.calculation.domain;

import me.realimpact.telecom.calculation.api.BillingCalculationPeriod;
import me.realimpact.telecom.calculation.api.BillingCalculationType;

import java.time.LocalDate;

public record CalculationContext(
        LocalDate billingStartDate,
        LocalDate billingEndDate,
        BillingCalculationType billingCalculationType,
        BillingCalculationPeriod billingCalculationPeriod) {
}
