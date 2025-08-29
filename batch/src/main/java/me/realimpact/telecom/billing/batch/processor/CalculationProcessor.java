package me.realimpact.telecom.billing.batch.processor;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import me.realimpact.telecom.billing.batch.CalculationParameters;
import me.realimpact.telecom.billing.batch.CalculationResultGroup;
import me.realimpact.telecom.billing.batch.reader.CalculationTarget;
import me.realimpact.telecom.calculation.application.monthlyfee.BaseFeeCalculator;
import me.realimpact.telecom.calculation.application.discount.DiscountCalculator;
import me.realimpact.telecom.calculation.application.onetimecharge.policy.DeviceInstallmentCalculator;
import me.realimpact.telecom.calculation.application.onetimecharge.policy.InstallationFeeCalculator;
import me.realimpact.telecom.calculation.application.vat.VatCalculator;
import me.realimpact.telecom.calculation.domain.CalculationContext;
import me.realimpact.telecom.calculation.domain.CalculationResult;
import org.springframework.batch.core.configuration.annotation.StepScope;
import org.springframework.batch.item.ItemProcessor;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.function.BiFunction;

/**
 * Spring Batch ItemProcessor 구현체
 * ContractDto를 받아서 월정액 계산을 수행하고 결과를 반환
 */
@StepScope
@RequiredArgsConstructor
@Slf4j
public class CalculationProcessor implements ItemProcessor<CalculationTarget, CalculationResultGroup> {

    private final BaseFeeCalculator baseFeeCalculator;
    private final InstallationFeeCalculator installationFeeCalculator;
    private final DeviceInstallmentCalculator deviceInstallmentCalculator;
    private final DiscountCalculator discountCalculator;
    private final VatCalculator vatCalculator;

    private final CalculationParameters calculationParameters;

    @Override
    public CalculationResultGroup process(CalculationTarget calculationTarget) throws Exception {
        try {
            log.debug("Processing contract calculation for contractId: {}", calculationTarget.contractId());
            List<CalculationResult> results = new ArrayList<>();

            CalculationContext ctx = calculationParameters.toCalculationContext();

            // 월정액 계산
            results.addAll(process(calculationTarget.contractWithProductsAndSuspensions(), baseFeeCalculator::process, ctx));

            // 설치비
            results.addAll(process(calculationTarget.installationHistories(), installationFeeCalculator::process, ctx));

            // 할부
            results.addAll(process(calculationTarget.deviceInstallmentMasters(), deviceInstallmentCalculator::process, ctx));

            // 할인
            results.addAll(process(calculationTarget.contractDiscounts(), discountCalculator::process, ctx));

            // VAT 계산 (기존 결과 기반)
            List<CalculationResult> vatResults = vatCalculator.calculateVat(ctx, results);
            results.addAll(vatResults);

            log.info("Processed {} calculation results (including {} VAT results) for contractId: {}", 
                     results.size(), vatResults.size(), calculationTarget.contractId());

            return new CalculationResultGroup(results);
        } catch (Exception e) {
            log.error("Failed to process contract calculation for contractId: {}", calculationTarget.contractId(), e);
            throw e;
        }
    }

    private <T> List<CalculationResult> process(
            Collection<T> items,
            BiFunction<CalculationContext, T, List<CalculationResult>> processor,
            CalculationContext context
    ) {
        return items.stream()
                .flatMap(item -> processor.apply(context, item).stream())
                .toList();
    }

}