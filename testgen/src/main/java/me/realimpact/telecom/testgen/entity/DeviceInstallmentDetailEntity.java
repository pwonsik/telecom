package me.realimpact.telecom.testgen.entity;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 단말할부 상세 엔티티
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class DeviceInstallmentDetailEntity {
    private Long contractId;
    private Long installmentSequence;
    private Integer installmentRound;
    private BigDecimal installmentAmount;
    private LocalDate billingCompletedDate;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}