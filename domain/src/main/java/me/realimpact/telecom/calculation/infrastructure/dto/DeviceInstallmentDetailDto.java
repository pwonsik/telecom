package me.realimpact.telecom.calculation.infrastructure.dto;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * 단말할부 상세 조회 결과 DTO
 */
@Getter
@Setter
@NoArgsConstructor
public class DeviceInstallmentDetailDto {
    private Integer installmentRound;
    private BigDecimal installmentAmount;
    private LocalDate billingCompletedDate;
}