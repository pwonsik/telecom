package me.realimpact.telecom.calculation.domain.monthlyfee;

import me.realimpact.telecom.calculation.domain.monthlyfee.Suspension.SuspensionType;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Optional;

/**
 * 일할 계산을 위한 순수 계산 데이터
 * CalculationResult 생성을 위한 기본 정보를 담고 있음
 */
public record ProratedCalculationData(
    Long contractId,
    String productOfferingId,
    String chargeItemId,
    String revenueItemId,
    LocalDate periodStartDate,
    LocalDate periodEndDate,
    Optional<SuspensionType> suspensionType,
    BigDecimal proratedFee,
    BigDecimal balance
) {
}