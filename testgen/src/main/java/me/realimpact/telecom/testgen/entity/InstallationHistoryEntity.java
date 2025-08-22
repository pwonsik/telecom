package me.realimpact.telecom.testgen.entity;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 설치 이력 엔티티
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class InstallationHistoryEntity {
    private Long contractId;
    private Long sequenceNumber;
    private LocalDate installationDate;
    private BigDecimal installationFee;
    private String billedFlag;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}