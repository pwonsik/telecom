package me.realimpact.telecom.calculation.domain.onetimecharge.policy.installment;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * 단말할부 마스터 정보
 * 
 * @param contractId 계약 ID
 * @param installmentSequence 할부 일련번호
 * @param installmentStartDate 할부 시작일
 * @param totalInstallmentAmount 할부금 총액
 * @param installmentMonths 할부 개월수
 * @param billedCount 할부 청구 횟수
 */
public record DeviceInstallmentMaster(
    Long contractId,
    Long installmentSequence,
    LocalDate installmentStartDate,
    BigDecimal totalInstallmentAmount,
    Integer installmentMonths,
    Integer billedCount
) {
}