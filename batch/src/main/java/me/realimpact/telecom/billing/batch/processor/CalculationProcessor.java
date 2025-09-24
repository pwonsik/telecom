package me.realimpact.telecom.billing.batch.processor;

import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import me.realimpact.telecom.billing.batch.CalculationParameters;
import me.realimpact.telecom.calculation.api.CalculationResultGroup;
import me.realimpact.telecom.calculation.application.CalculationCommandService;
import me.realimpact.telecom.calculation.application.CalculationTarget;
import me.realimpact.telecom.calculation.domain.CalculationContext;
import org.springframework.batch.item.ItemProcessor;

/**
 * Spring Batch ItemProcessor 구현체
 * ContractDto를 받아서 월정액 계산을 수행하고 결과를 반환
 */

@RequiredArgsConstructor
@Slf4j
public class CalculationProcessor implements ItemProcessor<CalculationTarget, CalculationResultGroup> {

    private final CalculationCommandService calculationCommandService;

    private final CalculationParameters calculationParameters;

    @Override
    public CalculationResultGroup process(@NonNull CalculationTarget calculationTarget) throws Exception {
        CalculationContext ctx = calculationParameters.toCalculationContext();
        return calculationCommandService.processCalculation(calculationTarget, ctx);
    }
}