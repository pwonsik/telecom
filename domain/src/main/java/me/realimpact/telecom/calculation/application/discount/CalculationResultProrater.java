package me.realimpact.telecom.calculation.application.discount;

import me.realimpact.telecom.calculation.domain.CalculationResult;
import me.realimpact.telecom.calculation.domain.discount.Discount;
import me.realimpact.telecom.calculation.domain.monthlyfee.DefaultPeriod;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class CalculationResultProrater {
    public List<CalculationResult<?>> prorate(List<CalculationResult<?>> calculationResultsBeforeDiscount,
                                             List<Discount> discounts) {
        return calculationResultsBeforeDiscount.stream()
            .flatMap(calculationResult -> {
                List<DefaultPeriod> discountDates = discounts.stream()
                    .map(discount -> DefaultPeriod.of(discount.getDiscountStartDate(), discount.getDiscountEndDate()))
                    .toList();
                return calculationResult.prorate(discountDates).stream();
            })
            .toList();
    }
}
