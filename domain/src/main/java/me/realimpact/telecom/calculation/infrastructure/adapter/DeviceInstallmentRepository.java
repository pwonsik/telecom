package me.realimpact.telecom.calculation.infrastructure.adapter;

import lombok.RequiredArgsConstructor;
import me.realimpact.telecom.calculation.domain.onetimecharge.policy.installation.InstallationHistory;
import me.realimpact.telecom.calculation.domain.onetimecharge.policy.installment.DeviceInstallmentMaster;
import me.realimpact.telecom.calculation.infrastructure.adapter.mybatis.DeviceInstallmentMapper;
import me.realimpact.telecom.calculation.infrastructure.adapter.mybatis.InstallationHistoryMapper;
import me.realimpact.telecom.calculation.infrastructure.converter.OneTimeChargeDtoConverter;
import me.realimpact.telecom.calculation.infrastructure.dto.DeviceInstallmentDto;
import me.realimpact.telecom.calculation.infrastructure.dto.InstallationHistoryDto;
import me.realimpact.telecom.calculation.port.out.DeviceInstallmentQueryPort;
import me.realimpact.telecom.calculation.port.out.InstallationHistoryCommandPort;
import me.realimpact.telecom.calculation.port.out.InstallationHistoryQueryPort;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;

@Repository
@RequiredArgsConstructor
public class DeviceInstallmentRepository implements DeviceInstallmentQueryPort {
    private final DeviceInstallmentMapper deviceInstallmentMapper;
    private final OneTimeChargeDtoConverter oneTimeChargeDtoConverter;

    @Override
    public List<DeviceInstallmentMaster> findDeviceInstallments(
        List<Long> contractIds, LocalDate billingStartDate, LocalDate billingEndDate
    ) {
        List<DeviceInstallmentDto> deviceInstallmentDtos =
            deviceInstallmentMapper.findInstallmentsByContractIds(contractIds, billingEndDate);
        return oneTimeChargeDtoConverter.convertToDeviceInstallmentMasters(deviceInstallmentDtos);
    }

}
