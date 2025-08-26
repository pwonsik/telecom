package me.realimpact.telecom.billing.batch;

import me.realimpact.telecom.calculation.api.BillingCalculationPeriod;
import me.realimpact.telecom.calculation.api.BillingCalculationType;
import me.realimpact.telecom.calculation.domain.CalculationContext;

import java.time.LocalDate;
import java.util.List;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public class CalculationParameters {
    private final LocalDate billingStartDate;
    private final LocalDate billingEndDate;
    private final BillingCalculationType billingCalculationType;
    private final BillingCalculationPeriod billingCalculationPeriod;
    private final int threadCount;
    private final List<Long> contractIds;

    public CalculationContext toCalculationContext() {
        return new CalculationContext(
                billingStartDate,
                billingEndDate,
                billingCalculationType,
                billingCalculationPeriod
        );
    }
}