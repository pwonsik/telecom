package me.realimpact.telecom.calculation.domain.onetimecharge.policy.installment;

/**
 * 단말할부 상세 정보
 * 단말할부마스터와 1:N 관계
 * 
 * @param contractId 계약 ID
 * @param installmentSequence 할부 일련번호
 * @param installmentRound 할부 회차
 * @param installmentAmount 할부금
 * @param billingCompletedDate 청구 완료일
 */
public record DeviceInstallmentDetail(
    Integer installmentRound,
    Long installmentAmount
) {
}