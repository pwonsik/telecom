package me.realimpact.telecom.calculation.domain.discount;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.ToString;
import me.realimpact.telecom.calculation.domain.CalculationContext;
import me.realimpact.telecom.calculation.domain.CalculationResult;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * 개별 할인 정보
 * 
 * @param contractId 계약 ID
 * @param discountId 할인 ID
 * @param discountStartDate 할인 시작일
 * @param discountEndDate 할인 종료일
 * @param productOfferingId 상품 오퍼링 ID
 * @param discountApplyUnit 할인 적용 단위 (RATE: 율, AMOUNT: 금액)
 * @param discountAmount 할인 금액
 * @param discountRate 할인 비율
 * @param discountAppliedAmount 적용된 할인 금액
 */
@Getter
@RequiredArgsConstructor
@ToString
public class Discount {
    
    public static final String APPLY_UNIT_RATE = "RATE";
    public static final String APPLY_UNIT_AMOUNT = "AMOUNT";
    
    private final Long contractId;
    private final String discountId;
    private final LocalDate discountStartDate;
    private final LocalDate discountEndDate;
    private final String productOfferingId;
    private final String discountApplyUnit;
    private final Long discountAmount;
    private final BigDecimal discountRate;
    private final BigDecimal discountAppliedAmount;

    /**
     * 기준 금액에 대한 할인 금액을 계산한다
     * 
     * @param baseAmount 기준 금액 (할인을 적용할 원래 금액)
     * @return 할인 금액 (양수로 반환, 실제 차감시 음수로 사용)
     */
    public BigDecimal calculateDiscount(CalculationResult<?> beforeDiscountCalculationResult) {
        BigDecimal baseFee = beforeDiscountCalculationResult.getBalance();
        if (baseFee.compareTo(BigDecimal.ZERO) <= 0) {
            return BigDecimal.ZERO;
        }
        
        return switch (discountApplyUnit) {
            case APPLY_UNIT_RATE -> {
                if (discountRate != null) {
                    yield baseFee.multiply(discountRate).divide(BigDecimal.valueOf(100), 5, RoundingMode.HALF_UP);
                }
                yield BigDecimal.ZERO;
            }
            case APPLY_UNIT_AMOUNT -> {
                if (discountAmount != null) {
                    // 할인 금액이 기준 금액보다 큰 경우 기준 금액을 반환 (100% 할인)
                    yield BigDecimal.valueOf(discountAmount).min(baseFee);
                }
                yield BigDecimal.ZERO;
            }
            default -> BigDecimal.ZERO;
        };
    }

    public boolean isDiscountTarget(CalculationResult<?> calculationResult) {
        if (!Objects.equals(this.productOfferingId, calculationResult.getProductOfferingId())) {
            return false;
        }
        if (calculationResult.getBillingStartDate().isAfter(discountEndDate) ||
            calculationResult.getBillingEndDate().isBefore(discountStartDate)) {
            return false;
        }
        return true;
    }
}