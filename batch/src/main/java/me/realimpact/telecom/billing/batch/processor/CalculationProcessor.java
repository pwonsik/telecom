package me.realimpact.telecom.billing.batch.processor;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import me.realimpact.telecom.billing.batch.CalculationParameters;
import me.realimpact.telecom.billing.batch.CalculationResultGroup;
import me.realimpact.telecom.calculation.application.Calculator;
import me.realimpact.telecom.calculation.application.monthlyfee.BaseFeeCalculator;
import me.realimpact.telecom.calculation.application.onetimecharge.policy.DeviceInstallmentCalculator;
import me.realimpact.telecom.calculation.application.onetimecharge.policy.InstallationFeeCalculator;
import me.realimpact.telecom.calculation.domain.CalculationResult;
import me.realimpact.telecom.calculation.domain.monthlyfee.ContractWithProductsAndSuspensions;
import me.realimpact.telecom.calculation.infrastructure.converter.ContractDtoToDomainConverter;
import me.realimpact.telecom.calculation.infrastructure.converter.OneTimeChargeDtoConverter;
import me.realimpact.telecom.calculation.infrastructure.dto.ContractDto;
import org.springframework.batch.core.configuration.annotation.StepScope;
import org.springframework.batch.item.ItemProcessor;

import java.util.List;
import java.util.stream.Stream;

/**
 * Spring Batch ItemProcessor 구현체
 * ContractDto를 받아서 월정액 계산을 수행하고 결과를 반환
 */
@StepScope
@RequiredArgsConstructor
@Slf4j
public class CalculationProcessor implements ItemProcessor<ContractDto, CalculationResultGroup> {

    private final BaseFeeCalculator baseFeeCalculator;
    private final InstallationFeeCalculator installationFeeCalculator;
    private final DeviceInstallmentCalculator deviceInstallmentCalculator;

    private final ContractDtoToDomainConverter contractDtoToDomainConverter;
    private final OneTimeChargeDtoConverter oneTimeChargeDtoConverter;
    private final CalculationParameters calculationParameters;

    @Override
    public CalculationResultGroup process(ContractDto contractDto) throws Exception {
        try {
            log.debug("Processing contract calculation for contractId: {}", contractDto.getContractId());
            
            // 1. DTO를 도메인 객체로 변환
            ContractWithProductsAndSuspensions contractWithProductsAndSuspensions =
                contractDtoToDomainConverter.convertToContract(contractDto);

            // 2. 월정액 계산 수행 (순수 계산 로직만)
            List<CalculationResult> baseFeeCalculationResult = baseFeeCalculator.process(
                calculationParameters.toCalculationContext(),
                contractWithProductsAndSuspensions
            );
            List<CalculationResult> installationFeeCalculationResult = contractDto.getInstallationHistories().stream()
                .flatMap(installationHistoryDto -> installationFeeCalculator.process(
                        calculationParameters.toCalculationContext(), oneTimeChargeDtoConverter.convertToInstallationHistory(installationHistoryDto)
                    ).stream()
                )
                .toList();

            List<CalculationResult> deviceInstallmentFeeCalculationResult = contractDto.getDeviceInstallments().stream()
                .flatMap(deviceInstallmentDto -> deviceInstallmentCalculator.process(
                        calculationParameters.toCalculationContext(), oneTimeChargeDtoConverter.convertToDeviceInstallmentMaster(deviceInstallmentDto)
                    ).stream()
                )
                .toList();

            return new CalculationResultGroup(
                Stream.of(
                    baseFeeCalculationResult,
                    installationFeeCalculationResult,
                    deviceInstallmentFeeCalculationResult
                ).flatMap(List::stream).toList()
            );
            
        } catch (Exception e) {
            log.error("Failed to process contract calculation for contractId: {}", contractDto.getContractId(), e);
            throw e;
        }
    }
}