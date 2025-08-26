package me.realimpact.telecom.calculation.infrastructure.dto;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/**
 * 단말할부 마스터 조회 결과 DTO
 * 단말할부 상세 정보를 리스트로 포함하는 1:n 구조
 */
@Getter
@Setter
@NoArgsConstructor
public class DeviceInstallmentDto {
    private Long contractId;
    private Long installmentSequence;
    private LocalDate installmentStartDate;
    private BigDecimal totalInstallmentAmount;
    private Integer installmentMonths;
    private Integer billedCount;
    private List<DeviceInstallmentDetailDto> details;
}