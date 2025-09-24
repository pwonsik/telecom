package me.realimpact.telecom.calculation.domain.monthlyfee;

/**
 * MonthlyCharge 도메인 객체임을 표시하는 마커 인터페이스
 * 모든 MonthlyCharge 관련 도메인 객체는 이 인터페이스를 구현해야 함
 */
public interface MonthlyChargeDomain {
    /**
     * 계약 ID 반환
     * @return 계약 ID
     */
    Long getContractId();
}