package me.realimpact.telecom.calculation.domain.onetimecharge;

import me.realimpact.telecom.calculation.domain.CalculationContext;

import java.util.List;

/**
 * OneTimeCharge 데이터 로딩 인터페이스
 * 특정 OneTimeCharge 도메인 타입의 데이터 로딩 로직을 정의
 *
 * @param <T> OneTimeCharge 도메인 타입
 */
public interface OneTimeChargeDataLoader<T extends OneTimeChargeDomain> {
    
    /**
     * 로딩할 수 있는 데이터 타입 반환
     * @return 데이터 타입
     */
    Class<T> getDataType();
    
    /**
     * 계약 ID 목록에 대한 데이터 로딩
     * @param contractIds 계약 ID 목록
     * @param context 계산 컨텍스트
     * @return 로딩된 데이터 목록
     */
    List<T> loadData(List<Long> contractIds, CalculationContext context);
}