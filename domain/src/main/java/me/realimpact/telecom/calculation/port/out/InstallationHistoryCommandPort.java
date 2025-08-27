package me.realimpact.telecom.calculation.port.out;

import me.realimpact.telecom.calculation.domain.onetimecharge.policy.installation.InstallationHistory;

import java.time.LocalDate;
import java.util.List;

public interface InstallationHistoryCommandPort {
    void updateChargeStatus(InstallationHistory installationHistory);
}
