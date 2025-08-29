package me.realimpact.telecom.calculation.domain.discount;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * 계약 할인 정보
 * 
 * @param contractId 계약 ID
 * @param discountId 할인 ID
 * @param discountStartDate 할인 시작일
 * @param discountEndDate 할인 종료일
 * @param productOfferingId 상품 오퍼링 ID
 * @param discountAplyUnit 할인 적용 단위 (RATE: 율, AMOUNT: 금액)
 * @param discountAmt 할인 금액
 * @param discountRate 할인 비율
 */
@Getter
@RequiredArgsConstructor
public class ContractDiscount {
    
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
    public BigDecimal getDiscountAmount(BigDecimal baseAmount) {
        if (baseAmount == null || baseAmount.compareTo(BigDecimal.ZERO) <= 0) {
            return BigDecimal.ZERO;
        }
        
        return switch (discountApplyUnit) {
            case APPLY_UNIT_RATE -> {
                if (discountRate != null) {
                    yield baseAmount.multiply(discountRate);
                }
                yield BigDecimal.ZERO;
            }
            case APPLY_UNIT_AMOUNT -> {
                if (discountAmount != null) {
                    // 할인 금액이 기준 금액보다 큰 경우 기준 금액을 반환 (100% 할인)
                    yield BigDecimal.valueOf(discountAmount).min(baseAmount);
                }
                yield BigDecimal.ZERO;
            }
            default -> BigDecimal.ZERO;
        };
    }
    
    /**
     * 할인이 특정 상품에 적용되는지 확인
     * 
     * @param productOfferingId 확인할 상품 오퍼링 ID
     * @return 적용 가능 여부
     */
    public boolean isApplicableToProduct(String productOfferingId) {
        return this.productOfferingId != null && this.productOfferingId.equals(productOfferingId);
    }
    
    /**
     * 특정 날짜에 할인이 유효한지 확인
     * 
     * @param targetDate 확인할 날짜
     * @return 유효 여부
     */
    public boolean isValidOn(LocalDate targetDate) {
        if (targetDate == null) {
            return false;
        }
        
        return !targetDate.isBefore(discountStartDate) && !targetDate.isAfter(discountEndDate);
    }
}