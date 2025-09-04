package me.realimpact.telecom.calculation.domain.onetimecharge.policy.installment;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.ToString;
import me.realimpact.telecom.calculation.api.BillingCalculationPeriod;
import me.realimpact.telecom.calculation.api.BillingCalculationType;
import me.realimpact.telecom.calculation.domain.onetimecharge.OneTimeChargeDomain;

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
@Getter
@RequiredArgsConstructor
@ToString
public class DeviceInstallmentMaster implements OneTimeChargeDomain {
    private final Long contractId;
    private final Long installmentSequence;
    private final LocalDate installmentStartDate;
    private final Long totalInstallmentAmount;
    private final Integer installmentMonths;
    private final Integer billedCount;
    private final List<DeviceInstallmentDetail> deviceInstallmentDetailList;

    public Long getFee(BillingCalculationType billingCalculationType, BillingCalculationPeriod billingCalculationPeriod) {
        // 해지 가정이면 전체 금액. 아니면 이달치 리턴
        if (billingCalculationType.isTerminationAssumed()) {
            return deviceInstallmentDetailList.stream().mapToLong(DeviceInstallmentDetail::installmentAmount).sum();
        } else {
            // 당월, 전당월의 전월이면 현재까지 청구된 차수의 다음 차수. 전당월의 당월이면 현재까지 청구된 차수의 다다음 차수
            int nextChargeIncrement = switch (billingCalculationPeriod) {
                case POST_BILLING_CURRENT_MONTH, PRE_BILLING_PREVIOUS_MONTH -> 1;
                case PRE_BILLING_CURRENT_MONTH -> 2;
            };
            return deviceInstallmentDetailList.stream()
                    .filter(deviceInstallmentDetail -> deviceInstallmentDetail.installmentRound() == this.billedCount + nextChargeIncrement)
                    .findFirst()
                    .map(DeviceInstallmentDetail::installmentAmount)
                    .orElse(0L);
        }
    }
}