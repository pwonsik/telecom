package me.realimpact.telecom.calculation.domain;

import java.util.List;

/**
 * 계산 결과 후처리를 위한 함수형 인터페이스
 * CalculationResult에 포함되어 Writer에서 호출된다.
 */
@FunctionalInterface
public interface PostProcessor<I> {
    
    /**
     * 계산 결과에 대한 후처리를 수행한다
     * 
     * @param ctx 계산 컨텍스트
     * @param result 계산 결과
     */
    void process(CalculationContext ctx, I input);
}