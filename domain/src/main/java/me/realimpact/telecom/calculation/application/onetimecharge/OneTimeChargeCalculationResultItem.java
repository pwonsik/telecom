package me.realimpact.telecom.calculation.application.onetimecharge;

import java.math.BigDecimal;

/**
 * 일회성 과금 계산 결과 항목
 * 
 * @param chargeItemCode 과금 항목 코드
 * @param fee 요금
 */
public record OneTimeChargeCalculationResultItem(
    String chargeItemCode,
    BigDecimal fee
) {
}