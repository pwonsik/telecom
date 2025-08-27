package me.realimpact.telecom.calculation.port.out;

import me.realimpact.telecom.calculation.domain.onetimecharge.policy.installation.InstallationHistory;
import me.realimpact.telecom.calculation.domain.onetimecharge.policy.installment.DeviceInstallmentMaster;

import java.time.LocalDate;
import java.util.List;

public interface DeviceInstallmentQueryPort {
    List<DeviceInstallmentMaster> findDeviceInstallments(List<Long> contractIds, LocalDate billingStartDate, LocalDate billingEndDate);
}
