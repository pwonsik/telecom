package me.realimpact.telecom.calculation.application.discount;

import lombok.RequiredArgsConstructor;
import me.realimpact.telecom.calculation.application.Calculator;
import me.realimpact.telecom.calculation.domain.CalculationContext;
import me.realimpact.telecom.calculation.domain.CalculationResult;
import me.realimpact.telecom.calculation.domain.discount.ContractDiscount;
import me.realimpact.telecom.calculation.port.out.ContractDiscountCommandPort;
import me.realimpact.telecom.calculation.port.out.ContractDiscountQueryPort;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 계약 할인 계산기
 * MyBatis로 조회한 할인내역 DTO를 입력받아 할인 계산 결과를 생성한다.
 */
@Service
@Order(22)
@RequiredArgsConstructor
public class DiscountCalculator implements Calculator<ContractDiscount> {
    private final ContractDiscountQueryPort contractDiscountQueryPort;
    private final ContractDiscountCommandPort contractDiscountCommandPort;

    @Override
    public Map<Long, List<ContractDiscount>> read(CalculationContext ctx, List<Long> contractIds) {
        return contractDiscountQueryPort.findContractDiscounts(
                contractIds, ctx.billingStartDate(), ctx.billingEndDate()
        ).stream().collect(Collectors.groupingBy(ContractDiscount::getContractId));
    }

    @Override
    public List<CalculationResult> process(CalculationContext ctx, ContractDiscount input) {
        // 할인은 기본적으로 고정 금액 또는 비율로 적용
        // 실제 할인 금액은 다른 계산 결과와의 연관성에 따라 결정되므로 여기서는 기본값 사용
        BigDecimal discountAmount = getBaseDiscountAmount(input);
        
        return List.of(
                new CalculationResult<>(
                        input.getContractId(),
                        ctx.billingStartDate(),
                        ctx.billingEndDate(),
                        input.getProductOfferingId(),
                        input.getDiscountId(),
                        input.getDiscountId(), // revenueItemId로 discountId 사용
                        input.getDiscountStartDate(),
                        input.getDiscountEndDate(),
                        null,
                        discountAmount.negate(), // 할인은 음수로 적용
                        input,
                        this::post
                )
        );
    }

    /**
     * 기본 할인 금액을 계산한다
     * 실제 구현에서는 다른 계산 결과와 연계하여 할인을 적용해야 할 수도 있음
     */
    private BigDecimal getBaseDiscountAmount(ContractDiscount contractDiscount) {
        return switch (contractDiscount.getDiscountApplyUnit()) {
            case ContractDiscount.APPLY_UNIT_AMOUNT -> 
                contractDiscount.getDiscountAmount() != null ? BigDecimal.valueOf(contractDiscount.getDiscountAmount()) : BigDecimal.ZERO;
            case ContractDiscount.APPLY_UNIT_RATE -> {
                // 비율 할인의 경우 기준 금액이 필요하므로 여기서는 0 반환
                // 실제로는 다른 계산 결과와 연계하여 처리해야 함
                yield BigDecimal.ZERO;
            }
            default -> BigDecimal.ZERO;
        };
    }

    public void post(CalculationContext ctx, ContractDiscount input) {
        if (ctx.billingCalculationType().isPostable()) {
            contractDiscountCommandPort.updateDiscountStatus(input);
        }
    }
}