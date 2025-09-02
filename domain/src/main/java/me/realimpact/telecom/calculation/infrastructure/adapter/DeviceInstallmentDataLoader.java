package me.realimpact.telecom.calculation.infrastructure.adapter;

import lombok.RequiredArgsConstructor;
import me.realimpact.telecom.calculation.domain.CalculationContext;
import me.realimpact.telecom.calculation.domain.deviceinstallment.DeviceInstallmentMaster;
import me.realimpact.telecom.calculation.domain.onetimecharge.OneTimeChargeDataLoader;
import me.realimpact.telecom.calculation.port.out.DeviceInstallmentQueryPort;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * DeviceInstallmentMaster 데이터 로더
 * 단말기 할부 데이터를 로딩하는 역할
 */
@Component
@RequiredArgsConstructor
public class DeviceInstallmentDataLoader implements OneTimeChargeDataLoader<DeviceInstallmentMaster> {
    
    private final DeviceInstallmentQueryPort deviceInstallmentQueryPort;
    
    @Override
    public Class<DeviceInstallmentMaster> getDataType() {
        return DeviceInstallmentMaster.class;
    }
    
    @Override
    public List<DeviceInstallmentMaster> loadData(List<Long> contractIds, CalculationContext context) {
        return deviceInstallmentQueryPort.findDeviceInstallmentMasters(
            contractIds,
            context.getBillingStartDate(), 
            context.getBillingEndDate()
        );
    }
}