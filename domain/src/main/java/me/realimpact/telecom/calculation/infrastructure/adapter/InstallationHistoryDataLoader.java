package me.realimpact.telecom.calculation.infrastructure.adapter;

import lombok.RequiredArgsConstructor;
import me.realimpact.telecom.calculation.domain.CalculationContext;
import me.realimpact.telecom.calculation.domain.installationhistory.InstallationHistory;
import me.realimpact.telecom.calculation.domain.onetimecharge.OneTimeChargeDataLoader;
import me.realimpact.telecom.calculation.port.out.InstallationHistoryQueryPort;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * InstallationHistory 데이터 로더
 * 설치 이력 데이터를 로딩하는 역할
 */
@Component
@RequiredArgsConstructor
public class InstallationHistoryDataLoader implements OneTimeChargeDataLoader<InstallationHistory> {
    
    private final InstallationHistoryQueryPort installationHistoryQueryPort;
    
    @Override
    public Class<InstallationHistory> getDataType() {
        return InstallationHistory.class;
    }
    
    @Override
    public List<InstallationHistory> loadData(List<Long> contractIds, CalculationContext context) {
        return installationHistoryQueryPort.findInstallationHistories(
            contractIds,
            context.getBillingStartDate(), 
            context.getBillingEndDate()
        );
    }
}