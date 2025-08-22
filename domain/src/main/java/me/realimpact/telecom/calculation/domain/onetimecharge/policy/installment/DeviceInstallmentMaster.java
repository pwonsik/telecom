package me.realimpact.telecom.calculation.domain.onetimecharge.policy.installment;

import lombok.RequiredArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/**
 * 단말할부 마스터 정보
 * 
 * @param contractId 계약 ID
 * @param installmentSequence 할부 일련번호
 * @param installmentStartDate 할부 시작일
 * @param totalInstallmentAmount 할부금 총액
 * @param installmentMonths 할부 개월수
 * @param billedCount 할부 청구 횟수
 */
@RequiredArgsConstructor
public class DeviceInstallmentMaster {
    private final Long contractId;
    private final Long installmentSequence;
    private final LocalDate installmentStartDate;
    private final Long totalInstallmentAmount;
    private final Integer installmentMonths;
    private final Integer billedCount;
    private final List<DeviceInstallmentDetail> deviceInstallmentDetailList;

    public Long getFee() {
        return deviceInstallmentDetailList.stream()
                .filter(deviceInstallmentDetail -> deviceInstallmentDetail.installmentRound() == this.billedCount + 1)
                .findFirst()
                .map(DeviceInstallmentDetail::installmentAmount)
                .orElse(0L);
    }
}