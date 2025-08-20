package me.realimpact.telecom.calculation.infrastructure.dto;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * 설치이력 조회 결과 DTO
 */
public record InstallationHistoryDto(
    Long contractId,
    Long sequenceNumber,
    LocalDate installationDate,
    BigDecimal installationFee,
    String billedFlag
) {
}