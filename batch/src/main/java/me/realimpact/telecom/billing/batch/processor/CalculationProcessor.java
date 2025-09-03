package me.realimpact.telecom.billing.batch.processor;

import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import me.realimpact.telecom.billing.batch.CalculationParameters;
import me.realimpact.telecom.billing.batch.CalculationResultGroup;
import me.realimpact.telecom.billing.batch.reader.CalculationTarget;
import me.realimpact.telecom.calculation.application.discount.CalculationResultProrater;
import me.realimpact.telecom.calculation.application.monthlyfee.BaseFeeCalculator;
import me.realimpact.telecom.calculation.application.discount.DiscountCalculator;
import me.realimpact.telecom.calculation.application.onetimecharge.OneTimeChargeCalculator;
import me.realimpact.telecom.calculation.application.onetimecharge.policy.DeviceInstallmentCalculator;
import me.realimpact.telecom.calculation.application.onetimecharge.policy.InstallationFeeCalculator;
import me.realimpact.telecom.calculation.application.vat.VatCalculator;
import me.realimpact.telecom.calculation.domain.CalculationContext;
import me.realimpact.telecom.calculation.domain.CalculationResult;
import me.realimpact.telecom.calculation.domain.onetimecharge.OneTimeChargeDomain;
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
    private final List<OneTimeChargeCalculator<? extends OneTimeChargeDomain>> oneTimeChargeCalculators;
    private final CalculationResultProrater calculationResultProrater;
    private final DiscountCalculator discountCalculator;
    private final VatCalculator vatCalculator;

    private final CalculationParameters calculationParameters;

    @Override
    public CalculationResultGroup process(@NonNull CalculationTarget calculationTarget) throws Exception {
        try {
            log.debug("Processing contract calculation for contractId: {}", calculationTarget.contractId());
            List<CalculationResult<?>> results = new ArrayList<>();

            CalculationContext ctx = calculationParameters.toCalculationContext();

            // 월정액 계산
            results.addAll(process(calculationTarget.contractWithProductsAndSuspensions(), baseFeeCalculator::process, ctx));

            // 일회성 과금 계산
            for (var oneTimeChargeCalculator : oneTimeChargeCalculators) {
                processOneTimeChargeCalculator(oneTimeChargeCalculator, calculationTarget, ctx, results);
            }

            // 구간분리
            results = new ArrayList<>(calculationResultProrater.prorate(ctx, results, calculationTarget.discounts()));

            // 할인
            results.addAll(discountCalculator.process(ctx, results, calculationTarget.discounts()));

            // VAT 계산 (기존 결과 기반)
            results.addAll(vatCalculator.calculateVat(ctx, results));

            log.info("Processed {} calculation results for contractId: {}",
                     results.size(), calculationTarget.contractId());

            return new CalculationResultGroup(results);
        } catch (Exception e) {
            log.error("Failed to process contract calculation for contractId: {}", calculationTarget.contractId(), e);
            throw e;
        }
    }

    /**
     * OneTimeChargeCalculator 타입 안전 처리
     */
    @SuppressWarnings("unchecked")
    private <T extends OneTimeChargeDomain> void processOneTimeChargeCalculator(
            OneTimeChargeCalculator<T> calculator,
            CalculationTarget target,
            CalculationContext ctx,
            List<CalculationResult<?>> results) {
        Class<T> inputType = calculator.getInputType();
        List<T> inputData = target.getOneTimeChargeData(inputType);
        results.addAll(process(inputData, calculator::process, ctx));
    }

    private <T> List<CalculationResult<?>> process(
            Collection<T> items,
            BiFunction<CalculationContext, T, List<? extends CalculationResult<?>>> processor,
            CalculationContext context
    ) {
        return items.stream()
                .flatMap(item -> processor.apply(context, item).stream())
                .<CalculationResult<?>>map(result -> result)
                .collect(ArrayList::new, ArrayList::add, ArrayList::addAll);
    }

}