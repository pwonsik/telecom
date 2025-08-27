package me.realimpact.telecom.calculation.port.out;

import me.realimpact.telecom.calculation.domain.onetimecharge.policy.installation.InstallationHistory;

import java.time.LocalDate;
import java.util.List;

public interface InstallationHistoryQueryPort {
    List<InstallationHistory> findInstallations(List<Long> contractIds, LocalDate billingStartDate, LocalDate billingEndDate);
}
