package me.realimpact.telecom.calculation.infrastructure.dto;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * 단말할부 상세 조회 결과 DTO
 */
public record DeviceInstallmentDetailDto(
    Integer installmentRound,
    BigDecimal installmentAmount,
    LocalDate billingCompletedDate
) {
}