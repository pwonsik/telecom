package me.realimpact.telecom.billing.batch.reader;

import me.realimpact.telecom.calculation.domain.discount.Discount;
import me.realimpact.telecom.calculation.domain.monthlyfee.ContractWithProductsAndSuspensions;
import me.realimpact.telecom.calculation.domain.discount.ContractDiscounts;
import me.realimpact.telecom.calculation.domain.onetimecharge.policy.installation.InstallationHistory;
import me.realimpact.telecom.calculation.domain.onetimecharge.policy.installment.DeviceInstallmentMaster;

import java.util.List;
import java.util.Optional;

public record CalculationTarget(
    Long contractId,
    List<ContractWithProductsAndSuspensions> contractWithProductsAndSuspensions,
    List<InstallationHistory> installationHistories,
    List<DeviceInstallmentMaster> deviceInstallmentMasters,
    List<Discount> discounts
) {
}
