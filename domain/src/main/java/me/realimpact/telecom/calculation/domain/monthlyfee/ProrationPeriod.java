package me.realimpact.telecom.calculation.domain.monthlyfee;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
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
    private final List<AdditionalBillingFactors> additionalBillingFactors;

    public long getDayOfMonth() {
        return period.getCalculationStartDate().getDayOfMonth();
    }

    public BigDecimal getProratedAmount(BigDecimal amount) {
        return amount
                .multiply(BigDecimal.valueOf(this.getUsageDays()))
                .multiply(calculateSuspensionRatio())
                .divide(BigDecimal.valueOf(this.getDayOfMonth()), 2, RoundingMode.HALF_UP);
    }

    private BigDecimal calculateSuspensionRatio() {
        return suspension
            .map(s -> s.getSuspensionType() == SuspensionType.TEMPORARY_SUSPENSION 
                ? monthlyChargeItem.getSuspensionChargeRatio() 
                : BigDecimal.ZERO)
            .orElse(BigDecimal.ONE);
    }

    @Override
    public LocalDate getCalculationStartDate() {
        return period.getCalculationStartDate();
    }

    @Override
    public LocalDate getCalculationEndDate() {
        return period.getCalculationEndDate();
    }

    /**
     * 추가 과금 요소 값을 타입에 맞게 반환합니다.
     * 
     * @param key   조회할 요소의 키
     * @param clazz 반환받고자 하는 타입 (예: String.class, Long.class)
     * @return      해당 타입의 Optional 값
     */
    public <T> Optional<T> getAdditionalBillingFactor(String key, Class<T> clazz) {
        for (AdditionalBillingFactors additionalBillingFactor : additionalBillingFactors) {
            Optional<String> factor = additionalBillingFactor.getFactor(key);
            if (factor.isPresent()) {
                String value = factor.get();
                try {
                    if (clazz == String.class) {
                        return Optional.of(clazz.cast(value));
                    } else if (clazz == Long.class) {
                        return Optional.of(clazz.cast(Long.valueOf(value)));
                    }
                } catch (Exception e) {
                    throw e;
                }
            }
        }
        return Optional.empty();
    }
}