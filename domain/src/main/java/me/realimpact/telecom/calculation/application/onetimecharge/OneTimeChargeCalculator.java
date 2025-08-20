package me.realimpact.telecom.calculation.application.onetimecharge;

import me.realimpact.telecom.calculation.api.CalculationRequest;

import java.util.List;

/**
 * 일회성 과금 계산을 위한 공통 인터페이스
 * 
 * @param <I> 입력 데이터 타입 (MyBatis 조회 결과 DTO 등)
 * @param <O> 출력 데이터 타입 (계산 결과)
 */
public interface OneTimeChargeCalculator<I, O> {
    
    /**
     * 계산에 필요한 데이터를 읽어온다
     * 
     * @param request 계산 요청 정보
     * @return 계산에 필요한 입력 데이터 목록
     */
    List<I> read(CalculationRequest request);
    
    /**
     * 개별 입력 데이터를 처리하여 계산 결과를 생성한다
     * 
     * @param input 입력 데이터
     * @return 계산 결과
     */
    O process(I input);
    
    /**
     * 계산 결과를 저장한다
     * 
     * @param output 계산 결과 목록
     */
    void write(List<O> output);
    
    /**
     * 계산 완료 후 후처리 작업을 수행한다
     * 
     * @param output 계산 결과 목록
     */
    void post(List<O> output);
    
    /**
     * 일회성 과금 계산의 전체 플로우를 실행한다
     * 
     * @param request 계산 요청 정보
     */
    default void calculate(CalculationRequest request) {
        List<O> results = read(request).stream()
                .map(this::process)
                .toList();
        write(results);
        post(results);
    }
}