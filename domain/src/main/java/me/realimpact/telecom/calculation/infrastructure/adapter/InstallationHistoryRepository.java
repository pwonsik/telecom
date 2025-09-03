package me.realimpact.telecom.calculation.infrastructure.adapter;

import lombok.RequiredArgsConstructor;
import me.realimpact.telecom.calculation.domain.onetimecharge.policy.installation.InstallationHistory;
import me.realimpact.telecom.calculation.infrastructure.adapter.mybatis.InstallationHistoryMapper;
import me.realimpact.telecom.calculation.infrastructure.converter.OneTimeChargeDtoConverter;
import me.realimpact.telecom.calculation.infrastructure.dto.InstallationHistoryDto;
import me.realimpact.telecom.calculation.port.out.InstallationHistoryCommandPort;
import me.realimpact.telecom.calculation.port.out.InstallationHistoryQueryPort;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;

@Repository
@RequiredArgsConstructor
public class InstallationHistoryRepository implements InstallationHistoryQueryPort, InstallationHistoryCommandPort {
    private final InstallationHistoryMapper installationHistoryMapper;
    private final OneTimeChargeDtoConverter oneTimeChargeDtoConverter;

    @Override
    public List<InstallationHistory> findInstallations(
        List<Long> contractIds, LocalDate billingStartDate, LocalDate billingEndDate
    ) {
        List<InstallationHistoryDto> installationHistoryDtos =
            installationHistoryMapper.findInstallationsByContractIds(contractIds, billingEndDate);
        return oneTimeChargeDtoConverter.convertToInstallationHistories(installationHistoryDtos);
    }

    @Override
    public void updateChargeStatus(InstallationHistory installationHistory) {
        int updatedRows = installationHistoryMapper.updateBilledFlag(
            installationHistory.getContractId(),
            installationHistory.getSequenceNumber()
        );
        
        if (updatedRows == 0) {
            throw new IllegalStateException(
                "설치내역을 찾을 수 없습니다. contractId: " + installationHistory.getContractId() +
                ", sequenceNumber: " + installationHistory.getSequenceNumber()
            );
        }
    }
}
