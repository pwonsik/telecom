package me.realimpact.telecom.calculation.api;

import java.time.LocalDate;
import java.util.List;


public record CalculationRequest(
    List<Long> contractIds,
    LocalDate billingStartDate, 
    LocalDate billingEndDate,
    BillingCalculationType billingCalculationType,
    BillingCalculationPeriod billingCalculationPeriod) {

}
