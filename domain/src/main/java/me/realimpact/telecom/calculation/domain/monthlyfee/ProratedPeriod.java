package me.realimpact.telecom.calculation.domain.monthlyfee;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.temporal.TemporalAdjuster;
import java.util.List;
import java.util.Optional;

import lombok.Builder;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import me.realimpact.telecom.calculation.domain.CalculationContext;
import me.realimpact.telecom.calculation.domain.CalculationResult;
import me.realimpact.telecom.calculation.domain.monthlyfee.Suspension.SuspensionType;

@RequiredArgsConstructor
@Builder
@Getter
public class ProratedPeriod extends Temporal {
    private final Temporal period;
    private final ContractWithProductsAndSuspensions contractWithProductsAndSuspensions;
    private final Product product;
    private final ProductOffering productOffering;
    private final MonthlyChargeItem monthlyChargeItem;
    private final Optional<Suspension> suspension;
    private final List<AdditionalBillingFactor> additionalBillingFactors;

    public long getDayOfMonth() {
        return period.getStartDate().lengthOfMonth();
    }

    private BigDecimal calculateSuspensionRatio() {
        return suspension
            .map(s -> s.getSuspensionType() == SuspensionType.TEMPORARY_SUSPENSION
                ? monthlyChargeItem.getSuspensionChargeRatio() 
                : BigDecimal.ZERO)
            .orElse(BigDecimal.ONE);
    }

    @Override
    public LocalDate getStartDate() {
        return period.getStartDate();
    }

    @Override
    public LocalDate getEndDate() {
        return period.getEndDate();
    }

    public CalculationResult calculate(CalculationContext ctx) {
        BigDecimal proratedFee = monthlyChargeItem.getPrice(additionalBillingFactors)
                .multiply(BigDecimal.valueOf(this.getUsageDays()))
                .multiply(calculateSuspensionRatio())
                .divide(BigDecimal.valueOf(this.getDayOfMonth()), 5, RoundingMode.HALF_UP);

        return new CalculationResult(
                this.contractWithProductsAndSuspensions.getContractId(),
            ctx.billingStartDate(),
            ctx.billingEndDate(),
            productOffering.getProductOfferingId(),
            monthlyChargeItem.getChargeItemId(),
            getStartDate(),
            getEndDate(),
            suspension.map(Suspension::getSuspensionType).orElse(null),
            proratedFee
        );
    }


}