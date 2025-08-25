package me.realimpact.telecom.billing.batch.processor;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import me.realimpact.telecom.billing.batch.CalculationParameters;
import me.realimpact.telecom.billing.batch.CalculationResultGroup;
import me.realimpact.telecom.billing.batch.reader.CalculationTarget;
import me.realimpact.telecom.calculation.application.monthlyfee.BaseFeeCalculator;
import me.realimpact.telecom.calculation.application.onetimecharge.policy.DeviceInstallmentCalculator;
import me.realimpact.telecom.calculation.application.onetimecharge.policy.InstallationFeeCalculator;
import me.realimpact.telecom.calculation.domain.CalculationContext;
import me.realimpact.telecom.calculation.domain.CalculationResult;
import me.realimpact.telecom.calculation.domain.monthlyfee.ContractWithProductsAndSuspensions;
import me.realimpact.telecom.calculation.infrastructure.converter.ContractDtoToDomainConverter;
import me.realimpact.telecom.calculation.infrastructure.converter.OneTimeChargeDtoConverter;
import me.realimpact.telecom.calculation.infrastructure.dto.ContractProductsSuspensionsDto;
import org.springframework.batch.core.configuration.annotation.StepScope;
import org.springframework.batch.item.ItemProcessor;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.function.BiFunction;
import java.util.stream.Stream;

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

    private final CalculationParameters calculationParameters;

    @Override
    public CalculationResultGroup process(CalculationTarget calculationTarget) throws Exception {
        try {
            log.debug("Processing contract calculation for contractId: {}", calculationTarget.contractId());
            List<CalculationResult> calculationResults = new ArrayList<>();

            // 월정액 계산
            processAndAddResults(
                    calculationTarget.contractWithProductsAndSuspensions(),
                    baseFeeCalculator::process,
                    calculationParameters.toCalculationContext(),
                    calculationResults
            );

            // 설치비
            processAndAddResults(
                    calculationTarget.installationHistories(),
                    installationFeeCalculator::process,
                    calculationParameters.toCalculationContext(),
                    calculationResults
            );

            // 할부
            processAndAddResults(
                    calculationTarget.deviceInstallmentMasters(),
                    deviceInstallmentCalculator::process,
                    calculationParameters.toCalculationContext(),
                    calculationResults
            );

            return new CalculationResultGroup(calculationResults);
        } catch (Exception e) {
            log.error("Failed to process contract calculation for contractId: {}", calculationTarget.contractId(), e);
            throw e;
        }
    }

    private <T> void processAndAddResults(
            Collection<T> items,
            BiFunction<CalculationContext, T, List<CalculationResult>> processor,
            CalculationContext context,
            List<CalculationResult> results
    ) {
        results.addAll(
                items.stream()
                        .flatMap(item -> processor.apply(context, item).stream())
                        .toList()
        );
    }

}