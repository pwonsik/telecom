package me.realimpact.telecom.calculation.domain;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import lombok.With;
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
    private final BigDecimal balance;
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
    public List<CalculationResult<?>> prorate(List<DefaultPeriod> periods) {
        List<LocalDate> datePoints = getDatePoints(periods);

        // 각 구간별로 ProratedPeriod 리스트를 생성
        // 명시적 타입 힌트. 신기하군
        return IntStream.range(0, datePoints.size() - 1)
            .mapToObj(i -> DefaultPeriod.of(
                datePoints.get(i),
                datePoints.get(i + 1)
            ))
            .<CalculationResult<?>>map(this::createProratedResult)
            .toList();
    }

    private List<LocalDate> getDatePoints(List<DefaultPeriod> periods) {
        // periods 중 구간을 분리할 후보군을 선정한다.
        Stream<LocalDate> startDatesStream = periods.stream()
            .filter(this::overlapsWith)
            .map(DefaultPeriod::getStartDate);

        Stream<LocalDate> endDatesStream = periods.stream()
            .filter(this::overlapsWith)
            .map(DefaultPeriod::getEndDate);

        return Stream.of(Stream.of(effectiveStartDate), Stream.of(effectiveEndDate), startDatesStream, endDatesStream)
            .flatMap(Function.identity())
            .sorted()
            .toList();
    }

    private boolean overlapsWith(DefaultPeriod period) {
        return (period.getStartDate().isBefore(effectiveEndDate) || period.getStartDate().isEqual(effectiveEndDate)) &&
            (effectiveStartDate.isBefore(period.getEndDate()) || effectiveStartDate.isEqual(period.getEndDate()));
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
            proratedFee,      // 일할 계산된 금액
            domain,           // 기존 도메인 객체 유지
            postProcessor     // 기존 PostProcessor 유지
        );
    }

    public CalculationResult<?> debitBalance(BigDecimal balanceToDebit) {
        return new CalculationResult<>(
            contractId,
            billingStartDate,
            billingEndDate,
            productOfferingId,
            chargeItemId,
            revenueItemId,
            effectiveStartDate,
            effectiveEndDate,
            suspensionType,
            fee,
            fee.subtract(balanceToDebit),
            domain,           // 기존 도메인 객체 유지
            postProcessor     // 기존 PostProcessor 유지
        );
    }
}