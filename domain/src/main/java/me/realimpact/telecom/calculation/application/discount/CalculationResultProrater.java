package me.realimpact.telecom.calculation.application.discount;

import me.realimpact.telecom.calculation.domain.CalculationContext;
import me.realimpact.telecom.calculation.domain.CalculationResult;
import me.realimpact.telecom.calculation.domain.discount.Discount;
import me.realimpact.telecom.calculation.domain.monthlyfee.DefaultPeriod;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Component
public class CalculationResultProrater {
    // calculationResultsBeforeDiscount와 discounts의 구간을 부딪쳐서 나눈다.
    public List<? extends CalculationResult<?>> prorate(CalculationContext ctx,
                                              List<CalculationResult<?>> calculationResultsBeforeDiscount,
                                              List<Discount> discounts) {
        return calculationResultsBeforeDiscount.stream()
            .flatMap(calculationResult -> {
                List<DefaultPeriod> discountDates = discounts.stream()
                        .filter(discount -> discount.getProductOfferingId().equals(calculationResult.getProductOfferingId()))
                        .map(discount -> DefaultPeriod.of(
                                discount.getDiscountStartDate().isAfter(ctx.billingStartDate()) ? discount.getDiscountStartDate() : ctx.billingStartDate(),
                                discount.getDiscountEndDate().isBefore(ctx.billingEndDate()) ? discount.getDiscountEndDate() : ctx.billingEndDate()
                                )
                        )
                        .toList();

                // 적합한 discount가 없으면 원본 CalculationResult를 그대로 반환
                if (discountDates.isEmpty()) {
                    return Stream.of(calculationResult);
                }

                return calculationResult.prorate(discountDates).stream();
            })
            .toList();
    }
    
    /**
     * CalculationResult 목록을 contract_id와 revenue_item_id로 그룹화하여 통합
     * fee와 balance를 합계하여 새로운 CalculationResult를 생성
     * 
     * @param results 통합할 CalculationResult 목록
     * @return 통합된 CalculationResult 목록
     */
    public List<CalculationResult<?>> consolidate(List<CalculationResult<?>> results) {
        // contract_id와 revenue_item_id로 그룹화
        Map<ConsolidationKey, List<CalculationResult<?>>> groupedResults = results.stream()
            .collect(Collectors.groupingBy(result -> 
                new ConsolidationKey(result.getContractId(), result.getRevenueItemId())
            ));
        
        // 각 그룹을 통합된 CalculationResult로 변환
        return groupedResults.entrySet().stream()
            .<CalculationResult<?>>map(entry -> consolidateGroup(entry.getKey(), entry.getValue()))
            .toList();
    }
    
    /**
     * 동일한 contract_id, revenue_item_id를 가진 CalculationResult 그룹을 하나로 통합
     */
    private CalculationResult<?> consolidateGroup(ConsolidationKey key, List<CalculationResult<?>> group) {
        if (group.isEmpty()) {
            throw new IllegalArgumentException("Empty group cannot be consolidated");
        }
        
        // 기준이 되는 첫 번째 결과 (메타데이터용)
        CalculationResult<?> template = group.get(0);
        
        // fee와 balance 합계 계산
        BigDecimal totalFee = group.stream()
            .map(CalculationResult::getFee)
            .reduce(BigDecimal.ZERO, BigDecimal::add);
            
        BigDecimal totalBalance = group.stream()
            .map(CalculationResult::getBalance)
            .reduce(BigDecimal.ZERO, BigDecimal::add);

        // 통합된 CalculationResult 생성
        return new CalculationResult<>(
            template.getContractId(),
            template.getBillingStartDate(),
            template.getBillingEndDate(),
            "#",
            "#",
            template.getRevenueItemId(),
                template.getBillingStartDate(),
                template.getBillingEndDate(),
            null,
            totalFee,
            totalBalance,
            null, // domain은 통합 시 null (개별 객체 참조 불가능)
            null  // postProcessor는 통합 시 null (개별 처리 불가능)
        );
    }
    
    /**
     * 통합을 위한 그룹화 키
     */
    private record ConsolidationKey(Long contractId, String revenueItemId) {}
}
