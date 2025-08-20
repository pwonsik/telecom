package me.realimpact.telecom.calculation.domain.onetimecharge.policy.installation;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * 설치 이력 정보
 * 
 * @param contractId 계약 ID
 * @param sequenceNumber 일련번호
 * @param installationDate 설치일
 * @param installationFee 설치비
 * @param billedFlag 청구 여부 (Y/N)
 */
public record InstallationHistory(
    Long contractId,
    Long sequenceNumber,
    LocalDate installationDate,
    BigDecimal installationFee,
    String billedFlag
) {
}