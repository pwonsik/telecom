package me.realimpact.telecom.calculation.application.discount;

import me.realimpact.telecom.calculation.domain.CalculationContext;
import me.realimpact.telecom.calculation.domain.CalculationResult;
import me.realimpact.telecom.calculation.domain.discount.Discount;
import me.realimpact.telecom.calculation.domain.monthlyfee.DefaultPeriod;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class CalculationResultProrater {
    public List<CalculationResult<?>> prorate(CalculationContext ctx,
                                              List<CalculationResult<?>> calculationResultsBeforeDiscount,
                                              List<Discount> discounts) {
        return calculationResultsBeforeDiscount.stream()
            .flatMap(calculationResult -> {
                List<DefaultPeriod> discountDates = discounts.stream()
                        .filter(discount -> discount.getProductOfferingId().equals(calculationResult.getProductOfferingId()))
                        .map(discount -> DefaultPeriod.of(
                                discount.getDiscountStartDate().isAfter(ctx.billingStartDate()) ? discount.getDiscountStartDate() : ctx.billingStartDate(),
                                discount.getDiscountEndDate().isBefore(ctx.billingEndDate()) ? discount.getDiscountEndDate() : ctx.billingEndDate()
                                )
                        )
                        .toList();
                return calculationResult.prorate(discountDates).stream();
            })
            .toList();
    }
}
