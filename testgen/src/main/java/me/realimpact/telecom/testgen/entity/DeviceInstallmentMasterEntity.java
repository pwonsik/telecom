package me.realimpact.telecom.testgen.entity;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 단말할부 마스터 엔티티
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class DeviceInstallmentMasterEntity {
    private Long contractId;
    private Long installmentSequence;
    private LocalDate installmentStartDate;
    private BigDecimal totalInstallmentAmount;
    private Integer installmentMonths;
    private Integer billedCount;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}