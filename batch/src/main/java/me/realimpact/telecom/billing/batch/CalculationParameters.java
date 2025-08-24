package me.realimpact.telecom.billing.batch;

import me.realimpact.telecom.calculation.api.BillingCalculationPeriod;
import me.realimpact.telecom.calculation.api.BillingCalculationType;
import me.realimpact.telecom.calculation.domain.CalculationContext;

import java.time.LocalDate;
import java.util.List;

public record CalculationParameters(
    LocalDate billingStartDate,
    LocalDate billingEndDate,
    BillingCalculationType billingCalculationType,
    BillingCalculationPeriod billingCalculationPeriod,
    int threadCount,
    List<Long> contractIds
) {
    public CalculationContext toCalculationContext() {
        return new CalculationContext(
            billingStartDate,
            billingEndDate,
            billingCalculationType,
            billingCalculationPeriod
        );
    }
}
