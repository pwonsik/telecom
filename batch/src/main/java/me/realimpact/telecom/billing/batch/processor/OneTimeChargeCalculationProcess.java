package me.realimpact.telecom.billing.batch.processor;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import me.realimpact.telecom.calculation.application.onetimecharge.policy.DeviceInstallmentCalculator;
import me.realimpact.telecom.calculation.application.onetimecharge.policy.InstallationFeeCalculator;
import me.realimpact.telecom.calculation.domain.CalculationResult;
import me.realimpact.telecom.calculation.domain.onetimecharge.policy.installation.InstallationHistory;
import me.realimpact.telecom.calculation.domain.onetimecharge.policy.installment.DeviceInstallmentMaster;
import me.realimpact.telecom.calculation.infrastructure.converter.OneTimeChargeDtoConverter;
import me.realimpact.telecom.calculation.infrastructure.dto.ContractDto;
import org.springframework.batch.core.configuration.annotation.StepScope;
import org.springframework.batch.item.ItemProcessor;

import java.util.List;

@StepScope
@RequiredArgsConstructor
@Slf4j
public class OneTimeChargeCalculationProcess implements ItemProcessor<ContractDto, CalculationResult> {
    private final OneTimeChargeDtoConverter oneTimeChargeDtoConverter;
    private final DeviceInstallmentCalculator deviceInstallmentCalculator;
    private final InstallationFeeCalculator installationFeeCalculator;

    @Override
    public CalculationResult process(ContractDto contractDto) throws Exception {
        try {
            List<InstallationHistory> installationHistories = oneTimeChargeDtoConverter.convertToInstallationHistories(contractDto.getInstallationHistories());
            List<DeviceInstallmentMaster> deviceInstallmentMasters = oneTimeChargeDtoConverter.convertToDeviceInstallmentMasters(contractDto.getDeviceInstallments());

            installationFeeCalculator.process(installationHistories)

            List<CalculationResult> calculationResults = installationHistories.stream()
                    .map(installationHistory -> installationFeeCalculator.process(installationHistory))
                    .toList();

            return result;

        } catch (Exception e) {
            log.error("Failed to process contract calculation for contractId: {}", contractDto.getContractId(), e);
            throw e;
        }
    }
}
