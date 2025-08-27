package me.realimpact.telecom.calculation.infrastructure.adapter.other;

import me.realimpact.telecom.calculation.domain.onetimecharge.policy.installment.DeviceInstallmentMaster;
import me.realimpact.telecom.calculation.port.out.DeviceInstallmentCommandPort;
import org.springframework.stereotype.Component;

@Component
public class DeviceInstallmentServiceClient implements DeviceInstallmentCommandPort {
    @Override
    public void updateChargeStatus(DeviceInstallmentMaster deviceInstallmentMaster) {
        // 오더의 서비스를 주입받아서 호출하거나 이벤트 발행 필요
    }
}
