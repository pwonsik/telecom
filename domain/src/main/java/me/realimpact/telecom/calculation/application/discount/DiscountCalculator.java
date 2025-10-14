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
 * 계약에 적용되는 할인을 계산하는 서비스 클래스.
 * 할인 정보를 조회하고, 각 할인 항목에 대한 계산 결과를 생성한다.
 */
@Service
@Order(22)
@RequiredArgsConstructor
public class DiscountCalculator {
    private final ContractDiscountQueryPort contractDiscountQueryPort;
    private final ContractDiscountCommandPort contractDiscountCommandPort;

    /**
     * 주어진 계약 ID 목록에 대한 할인 정보를 조회한다.
     * @param ctx 계산 컨텍스트
     * @param contractIds 계약 ID 목록
     * @return 계약 ID를 키로 하고, ContractDiscounts를 값으로 하는 맵
     */
    public Map<Long, ContractDiscounts> read(CalculationContext ctx, List<Long> contractIds) {
        return contractDiscountQueryPort.findContractDiscounts(
            contractIds, ctx.billingStartDate(), ctx.billingEndDate()
        ).stream().collect(
            Collectors.toMap(ContractDiscounts::contractId, Function.identity())
        );
    }

    /**
     * 일할 계산된 요금 결과에 대해 할인을 적용하고, 할인에 대한 계산 결과를 생성한다.
     * @param ctx 계산 컨텍스트
     * @param proratedCalculationResultsBeforeDiscount 할인이 적용되기 전의 일할 계산된 요금 결과 목록
     * @param discounts 적용할 할인 목록
     * @return 할인 계산 결과 목록
     */
    public List<? extends CalculationResult<?>> process(CalculationContext ctx,
                                              List<? extends CalculationResult<?>> proratedCalculationResultsBeforeDiscount,
                                              List<Discount> discounts) {
        List<CalculationResult<?>> results = new ArrayList<>();
        // 각 할인 항목에 대해 순회하며 계산 결과를 생성한다.
        for (Discount discount : discounts) {
            for (CalculationResult<?> befDcCalResult : proratedCalculationResultsBeforeDiscount) {
                // 할인이 적용될 수 있는 대상인지 확인한다.
                if (!discount.isDiscountTarget(befDcCalResult)) {
                    continue;
                }
                
                // 할인 금액을 계산한다.
                BigDecimal discountAmount = discount.calculateDiscount(befDcCalResult);
                if (discountAmount.equals(BigDecimal.ZERO)) {
                    continue;
                }
                
                // 원본 요금 결과에서 할인 금액만큼 차감한다.
                befDcCalResult.debitBalance(discountAmount);

                // 할인에 대한 새로운 계산 결과를 생성한다.
                CalculationResult<?> discountResult = new CalculationResult<>(
                        befDcCalResult.getContractId(),
                        befDcCalResult.getBillingStartDate(),
                        befDcCalResult.getBillingEndDate(),
                        befDcCalResult.getProductOfferingId(),
                        "DC",   // 임시: 할인 항목 코드
                        "DC",  // 임시: 할인 항목 이름
                        befDcCalResult.getEffectiveStartDate(),
                        befDcCalResult.getEffectiveEndDate(),
                        befDcCalResult.getSuspensionType(),
                        discountAmount.negate(), // 할인 금액은 음수로 표현
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
     * 할인 처리 완료 후, 할인 적용 상태를 업데이트한다.
     * @param ctx 계산 컨텍스트
     * @param input 적용된 할인 정보
     */
    public void post(CalculationContext ctx, Discount input) {
        if (ctx.billingCalculationType().isPostable()) {
            contractDiscountCommandPort.applyDiscount(input);
        }
    }
}
