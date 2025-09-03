package me.realimpact.telecom.calculation.application.discount;

import lombok.RequiredArgsConstructor;
import me.realimpact.telecom.calculation.domain.CalculationContext;
import me.realimpact.telecom.calculation.domain.CalculationResult;
import me.realimpact.telecom.calculation.domain.discount.ContractDiscounts;
import me.realimpact.telecom.calculation.domain.discount.Discount;
import me.realimpact.telecom.calculation.port.out.ContractDiscountCommandPort;
import me.realimpact.telecom.calculation.port.out.ContractDiscountQueryPort;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * 계약 할인 계산기
 * 중첩 구조의 ContractDiscount를 처리하여 각 할인별로 CalculationResult를 생성한다.
 */
@Service
@Order(22)
@RequiredArgsConstructor
public class DiscountCalculator {
    private final ContractDiscountQueryPort contractDiscountQueryPort;
    private final ContractDiscountCommandPort contractDiscountCommandPort;

    public Map<Long, ContractDiscounts> read(CalculationContext ctx, List<Long> contractIds) {
        return contractDiscountQueryPort.findContractDiscounts(
            contractIds, ctx.billingStartDate(), ctx.billingEndDate()
        ).stream().collect(
            Collectors.toMap(ContractDiscounts::contractId, Function.identity())
        );
    }

    public List<? extends CalculationResult<?>> process(CalculationContext ctx,
                                              List<? extends CalculationResult<?>> proratedCalculationResultsBeforeDiscount,
                                              List<Discount> discounts) {
        List<CalculationResult<?>> results = new ArrayList<>();
        // ContractDiscount의 각 Discount에 대해 CalculationResult 생성
        for (Discount discount : discounts) {
            for (CalculationResult<?> befDcCalResult : proratedCalculationResultsBeforeDiscount) {
                if (!discount.isDiscountTarget(befDcCalResult)) {
                    continue;
                }
                BigDecimal discountAmount = discount.calculateDiscount(befDcCalResult);
                if (discountAmount.equals(BigDecimal.ZERO)) {
                    continue;
                }
                // 잔액 차감
                befDcCalResult.debitBalance(discountAmount);

                CalculationResult<?> discountResult = new CalculationResult<>(
                        befDcCalResult.getContractId(),
                        befDcCalResult.getBillingStartDate(),
                        befDcCalResult.getBillingEndDate(),
                        befDcCalResult.getProductOfferingId(),
                        "DC",   // 임시
                        "DC",  // 임시
                        befDcCalResult.getEffectiveStartDate(),
                        befDcCalResult.getEffectiveEndDate(),
                        befDcCalResult.getSuspensionType(),
                        discountAmount.negate(),
                        BigDecimal.ZERO,
                        discount,
                        this::post
                );
                results.add(discountResult);
            }
        }
        return results;
    }


    /**
     * 할인 처리 완료 후 상태 업데이트
     */
    public void post(CalculationContext ctx, Discount input) {
        if (ctx.billingCalculationType().isPostable()) {
            contractDiscountCommandPort.applyDiscount(input);
        }
    }
}