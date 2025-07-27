package me.realimpact.telecom.calculation.api;

import java.time.LocalDate;


public record CalculationRequest(
    long contractId, 
    LocalDate billingStartDate, 
    LocalDate billingEndDate,
    BillingCalculationType billingCalculationType,
    BillingCalculationPeriod billingCalculationPeriod) {

}
