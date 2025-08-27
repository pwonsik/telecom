package me.realimpact.telecom.calculation.port.out;

import me.realimpact.telecom.calculation.domain.onetimecharge.policy.installment.DeviceInstallmentMaster;

public interface DeviceInstallmentCommandPort {
    void updateChargeStatus(DeviceInstallmentMaster deviceInstallmentMaster);
}
