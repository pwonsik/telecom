package me.realimpact.telecom.calculation.application.monthlyfee;

import me.realimpact.telecom.calculation.domain.CalculationContext;
import me.realimpact.telecom.calculation.domain.CalculationResult;
import me.realimpact.telecom.calculation.domain.monthlyfee.MonthlyChargeDomain;

import java.util.List;

/**
 * Monthly Fee 계산기 인터페이스
 * 특정 Monthly Fee 도메인 타입에 대한 계산 로직을 정의
 *
 * @param <T> MonthlyChargeDomain을 구현한 Monthly Fee 도메인 타입
 */
public interface MonthlyFeeCalculator<T extends MonthlyChargeDomain> {

    /**
     * 처리할 수 있는 입력 데이터 타입 반환
     * @return 입력 데이터 타입
     */
    Class<T> getDomainType();

    /**
     * Monthly Fee 계산 실행
     * @param context 계산 컨텍스트
     * @param input 입력 데이터
     * @return 계산 결과 목록
     */
    List<CalculationResult<T>> process(CalculationContext context, T input);
}