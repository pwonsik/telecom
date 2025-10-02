package me.realimpact.telecom.calculation.application.onetimecharge.loader;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import me.realimpact.telecom.calculation.application.onetimecharge.OneTimeChargeDataLoader;
import me.realimpact.telecom.calculation.domain.CalculationContext;
import me.realimpact.telecom.calculation.domain.onetimecharge.OneTimeChargeDomain;
import me.realimpact.telecom.calculation.domain.onetimecharge.policy.installation.InstallationHistory;
import me.realimpact.telecom.calculation.port.out.InstallationHistoryQueryPort;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * InstallationHistory 데이터 로딩 전담 클래스
 * 단일 책임: 설치 내역 데이터 로딩
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class InstallationHistoryDataLoader implements OneTimeChargeDataLoader<InstallationHistory> {

    private final InstallationHistoryQueryPort installationHistoryQueryPort;

    @Override
    public Class<InstallationHistory> getDomainType() {
        return InstallationHistory.class;
    }

    @Override
    public Map<Long, List<? extends OneTimeChargeDomain>> read(List<Long> contractIds, CalculationContext ctx) {
        log.debug("Loading InstallationHistory data for {} contracts", contractIds.size());

        Map<Long, List<InstallationHistory>> specificData = installationHistoryQueryPort
                .findInstallations(contractIds, ctx.billingStartDate(), ctx.billingEndDate())
                .stream()
                .collect(Collectors.groupingBy(InstallationHistory::getContractId));

        log.debug("Loaded InstallationHistory data for {} contracts", specificData.size());

        return specificData.entrySet().stream()
            .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
    }
}