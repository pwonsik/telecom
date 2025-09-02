package me.realimpact.telecom.calculation.domain.onetimecharge.policy.installation;

import me.realimpact.telecom.calculation.domain.onetimecharge.OneTimeChargeDomain;

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
    Long installationFee,
    String billedFlag
) implements OneTimeChargeDomain {
    public Long fee() {
        return installationFee();
    }
}