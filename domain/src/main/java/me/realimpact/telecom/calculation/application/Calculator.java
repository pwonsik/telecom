package me.realimpact.telecom.calculation.application;

import me.realimpact.telecom.calculation.api.CalculationRequest;
import me.realimpact.telecom.calculation.domain.CalculationContext;
import me.realimpact.telecom.calculation.domain.CalculationResult;

import java.util.List;

/**
 * 일회성 과금 계산을 위한 공통 인터페이스
 * 
 * @param <I> 입력 데이터 타입 (MyBatis 조회 결과 DTO 등)
 * @param <O> 출력 데이터 타입 (계산 결과)
 */
public interface Calculator<I> {
    
    /**
     * 계산에 필요한 데이터를 읽어온다
     * 
     * @param request 계산 요청 정보
     * @return 계산에 필요한 입력 데이터 목록
     */
    List<I> read(CalculationContext calculationContext, List<Long> contractIds);
    
    /**
     * 개별 입력 데이터를 처리하여 계산 결과를 생성한다
     * 
     * @param input 입력 데이터
     * @return 계산 결과
     */
    List<CalculationResult> process(CalculationContext calculationContext, I input);
    
    /**
     * 계산 결과를 저장한다
     * 
     * @param output 계산 결과 목록
     */
    void write(CalculationContext calculationContext, List<CalculationResult> output);
    
    /**
     * 계산 완료 후 후처리 작업을 수행한다
     * 
     * @param output 계산 결과 목록
     */
    void post(CalculationContext calculationContext, List<CalculationResult> output);

}