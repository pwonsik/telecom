package me.realimpact.telecom.calculation.domain.onetimecharge.policy.installation;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.ToString;
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
@Getter
@RequiredArgsConstructor
@ToString
public class InstallationHistory implements OneTimeChargeDomain {
    private final Long contractId;
    private final Long sequenceNumber;
    private final LocalDate installationDate;
    private final Long installationFee;
    private final String billedFlag;

    public Long getFee() {
        return installationFee;
    }
}