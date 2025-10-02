package me.realimpact.telecom.calculation.application.onetimecharge;

import me.realimpact.telecom.calculation.domain.CalculationContext;
import me.realimpact.telecom.calculation.domain.CalculationResult;
import me.realimpact.telecom.calculation.domain.onetimecharge.OneTimeChargeDomain;

import java.util.List;

/**
 * OneTimeCharge 계산기 인터페이스
 * 특정 OneTimeCharge 도메인 타입에 대한 계산 로직을 정의
 *
 * @param <T> OneTimeCharge 도메인 타입
 */
public interface OneTimeChargeCalculator<T extends OneTimeChargeDomain> {
    
    /**
     * 처리할 수 있는 입력 데이터 타입 반환
     * @return 입력 데이터 타입
     */
    Class<T> getDomainType();
    
    /**
     * OneTimeCharge 계산 실행
     * @param context 계산 컨텍스트
     * @param inputs 입력 데이터 목록
     * @return 계산 결과 목록
     */
    List<CalculationResult<T>> process(CalculationContext context, T input);
}