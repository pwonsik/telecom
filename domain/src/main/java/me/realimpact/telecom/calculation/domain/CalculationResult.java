package me.realimpact.telecom.calculation.domain;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import me.realimpact.telecom.calculation.domain.monthlyfee.Suspension;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.IntStream;
import java.util.stream.Stream;

@Getter
@Setter
@RequiredArgsConstructor
public class CalculationResult<I> {
    private final Long contractId;
    private final LocalDate billingStartDate;
    private final LocalDate billingEndDate;
    private final String productOfferingId;
    private final String chargeItemId;
    private final String revenueItemId;
    private final LocalDate effectiveStartDate;
    private final LocalDate effectiveEndDate;
    private final Suspension.SuspensionType suspensionType;
    private final BigDecimal fee;
    private final I domain;
    private final PostProcessor<I> postProcessor;
    
    /**
     * 후처리 작업을 실행한다
     * 
     * @param ctx 계산 컨텍스트
     */
    public void executePost(CalculationContext ctx) {
        if (postProcessor != null) {
            postProcessor.process(ctx, domain);
        }
    }

    /**
     * 주어진 기간으로 일할 계산하여 CalculationResult 목록을 반환한다
     * 
     * @param startDate 새로운 청구 시작일
     * @param endDate 새로운 청구 종료일  
     * @return 일할 계산된 CalculationResult 목록
     */
    public List<CalculationResult> prorate(LocalDate startDate, LocalDate endDate) {
        // 입력 검증
        if (startDate == null || endDate == null) {
            throw new RuntimeException("startDate and endDate must not be null");
        }
        
        // 기존 유효 기간이 null인 경우 처리
        if (effectiveStartDate == null || effectiveEndDate == null) {
            throw new RuntimeException("startDate and endDate must not be null");
        }
        
        // 겹치는 구간 계산 - 두 기간의 교집합
        LocalDate overlapStart = effectiveStartDate.isAfter(startDate) ? effectiveStartDate : startDate;
        LocalDate overlapEnd = effectiveEndDate.isBefore(endDate) ? effectiveEndDate : endDate;

        // 겹치는 구간에 대한 일할 계산 결과 생성
        if (overlapEnd.isAfter(overlapStart) || overlapStart.equals(overlapEnd)) {
            CalculationResult proratedResult = createProratedResult(overlapStart, overlapEnd);
            return List.of(proratedResult);
        }
        return List.of();
    }
    
    /**
     * 주어진 기간에 대한 일할 계산된 CalculationResult를 생성
     */
    private CalculationResult createProratedResult(LocalDate periodStart, LocalDate periodEnd) {
        // 원래 기간의 일수 계산
        long originalDays = ChronoUnit.DAYS.between(effectiveStartDate, effectiveEndDate) + 1;
        // 새로운 구간의 일수 계산  
        long proratedDays = ChronoUnit.DAYS.between(periodStart, periodEnd) + 1;
        
        // 일할 비율 계산
        BigDecimal prorateRatio = BigDecimal.valueOf(proratedDays)
            .divide(BigDecimal.valueOf(originalDays), 5, RoundingMode.HALF_UP);
        
        // 일할 계산된 금액
        BigDecimal proratedFee = fee != null ? 
            fee.multiply(prorateRatio).setScale(2, RoundingMode.HALF_UP) : 
            BigDecimal.ZERO;
        
        // 새로운 CalculationResult 생성 (기존 속성 유지, 날짜와 금액만 변경)
        return new CalculationResult<>(
            contractId,
            billingStartDate,
            billingEndDate,
            productOfferingId,
            chargeItemId,
            revenueItemId,
            periodStart,      // 새로운 유효 시작일
            periodEnd,        // 새로운 유효 종료일
            suspensionType,
            proratedFee,      // 일할 계산된 금액
            domain,           // 기존 도메인 객체 유지
            postProcessor     // 기존 PostProcessor 유지
        );
    }
}