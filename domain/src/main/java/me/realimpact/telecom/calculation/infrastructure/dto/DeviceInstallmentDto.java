package me.realimpact.telecom.calculation.infrastructure.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/**
 * 단말할부 마스터 조회 결과 DTO
 * 단말할부 상세 정보를 리스트로 포함하는 1:n 구조
 */
public record DeviceInstallmentDto(
    Long contractId,
    Long installmentSequence,
    LocalDate installmentStartDate,
    BigDecimal totalInstallmentAmount,
    Integer installmentMonths,
    Integer billedCount,
    List<DeviceInstallmentDetailDto> details
) {
}