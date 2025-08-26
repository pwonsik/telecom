package me.realimpact.telecom.calculation.infrastructure.dto;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * 설치이력 조회 결과 DTO
 */
@Getter
@Setter
@NoArgsConstructor
public class InstallationHistoryDto {
    private Long contractId;
    private Long sequenceNumber;
    private LocalDate installationDate;
    private BigDecimal installationFee;
    private String billedFlag;
}