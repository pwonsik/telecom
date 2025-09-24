package me.realimpact.telecom.calculation.domain;

import lombok.*;
import me.realimpact.telecom.calculation.domain.monthlyfee.DefaultPeriod;
import me.realimpact.telecom.calculation.domain.monthlyfee.Suspension;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.function.Function;
import java.util.stream.IntStream;
import java.util.stream.Stream;

@Getter
@Setter
//@RequiredArgsConstructor
@AllArgsConstructor
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
    private BigDecimal balance;
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
     * @return 일할 계산된 CalculationResult 목록
     */
    public List<? extends CalculationResult<?>> prorate(List<DefaultPeriod> periods) {
        if (periods == null || periods.isEmpty()) {
            return List.of();
        }

        // 각 period와 현재 CalculationResult가 겹치는 부분만 계산
        return periods.stream()
            .filter(this::overlapsWith)
            .map(period -> {
                // 겹치는 구간 계산
                LocalDate intersectionStart = effectiveStartDate.isAfter(period.getStartDate())
                    ? effectiveStartDate : period.getStartDate();
                LocalDate intersectionEnd = effectiveEndDate.isBefore(period.getEndDate())
                    ? effectiveEndDate : period.getEndDate();

                return DefaultPeriod.of(intersectionStart, intersectionEnd);
            })
            .filter(period -> period.getStartDate().isBefore(period.getEndDate())) // 유효한 구간만
            .map(this::createProratedResult)
            .toList();
    }


    private boolean overlapsWith(DefaultPeriod period) {
        return effectiveStartDate.isBefore(period.getEndDate()) &&
               period.getStartDate().isBefore(effectiveEndDate);
    }
    
    /**
     * 주어진 기간에 대한 일할 계산된 CalculationResult를 생성
     */
    private CalculationResult<?> createProratedResult(DefaultPeriod period) {
        // 원래 기간의 일수 계산
        long originalDays = ChronoUnit.DAYS.between(effectiveStartDate, effectiveEndDate) + 1;
        // 새로운 구간의 일수 계산  
        long proratedDays = ChronoUnit.DAYS.between(period.getStartDate(), period.getEndDate()) + 1;
        
        // 일할 비율 계산
        BigDecimal prorateRatio = BigDecimal.valueOf(proratedDays)
            .divide(BigDecimal.valueOf(originalDays), 5, RoundingMode.HALF_UP);
        
        // 일할 계산된 금액
        BigDecimal proratedFee = fee != null ? 
            fee.multiply(prorateRatio).setScale(2, RoundingMode.HALF_UP) : 
            BigDecimal.ZERO;
        BigDecimal proratedBalance = new BigDecimal(proratedFee.toString());
        // 새로운 CalculationResult 생성 (기존 속성 유지, 날짜와 금액만 변경)
        return new CalculationResult<>(
            contractId,
            billingStartDate,
            billingEndDate,
            productOfferingId,
            chargeItemId,
            revenueItemId,
            period.getStartDate(),      // 새로운 유효 시작일
            period.getEndDate(),        // 새로운 유효 종료일
            suspensionType,
            proratedFee,      // 일할 계산된 금액
                proratedBalance,      // 일할 계산된 금액
            domain,           // 기존 도메인 객체 유지
            postProcessor     // 기존 PostProcessor 유지
        );
    }

    public void debitBalance(BigDecimal balanceToDebit) {
        balance = balance.subtract(balanceToDebit);
    }
}