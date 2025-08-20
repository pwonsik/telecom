package me.realimpact.telecom.calculation.application.onetimecharge;

import java.time.LocalDate;
import java.util.List;

/**
 * 일회성 과금 계산 결과
 * 
 * @param contractId 계약 ID
 * @param billingStartDate 청구 시작일
 * @param billingEndDate 청구 종료일  
 * @param items 계산 결과 항목 목록
 */
public record OneTimeChargeCalculationResult(
    Long contractId,
    LocalDate billingStartDate,
    LocalDate billingEndDate,
    List<OneTimeChargeCalculationResultItem> items
) {
}