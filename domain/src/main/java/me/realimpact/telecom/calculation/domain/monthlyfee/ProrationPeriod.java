package me.realimpact.telecom.calculation.domain.monthlyfee;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Optional;

import lombok.Builder;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import me.realimpact.telecom.calculation.domain.monthlyfee.Suspension.SuspensionType;

@RequiredArgsConstructor
@Builder
@Getter
public class ProrationPeriod extends Temporal {
    private final Period period;
    private final Contract contract;
    private final Product product;
    private final ProductOffering productOffering;
    private final MonthlyChargeItem monthlyChargeItem;
    private final Optional<Suspension> suspension;
    private final List<AdditionalBillingFactors> billingFactors;

    public long getUsageDays() {
        return ChronoUnit.DAYS.between(period.getStartDate(), period.getEndDate());
    }        

    public long getDayOfMonth() {
        return period.getStartDate().getDayOfMonth();
    }

    public BigDecimal getProratedAmount(BigDecimal amount) {
        return amount
                .multiply(BigDecimal.valueOf(this.getUsageDays()))
                .multiply(getRatio())
                .divide(BigDecimal.valueOf(this.getDayOfMonth()), 2, RoundingMode.HALF_UP);
    }

    private BigDecimal getRatio() {
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
}