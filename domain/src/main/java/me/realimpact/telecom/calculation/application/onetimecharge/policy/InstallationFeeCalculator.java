package me.realimpact.telecom.calculation.application.onetimecharge.policy;

import lombok.RequiredArgsConstructor;
import me.realimpact.telecom.calculation.api.CalculationRequest;
import me.realimpact.telecom.calculation.application.Calculator;
import me.realimpact.telecom.calculation.domain.CalculationContext;
import me.realimpact.telecom.calculation.domain.CalculationResult;
import me.realimpact.telecom.calculation.domain.onetimecharge.policy.installation.InstallationHistory;
import me.realimpact.telecom.calculation.infrastructure.adapter.mybatis.InstallationHistoryMapper;
import me.realimpact.telecom.calculation.port.out.InstallationHistoryQueryPort;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.List;

/**
 * 설치비 계산기
 * MyBatis로 조회한 설치내역 DTO를 입력받아 일회성 과금 계산 결과를 생성한다.
 */
@RequiredArgsConstructor
@Service
public class InstallationFeeCalculator implements Calculator<InstallationHistory> {
    private final InstallationHistoryQueryPort installationHistoryQueryPort;

    public InstallationFeeCalculator(InstallationHistoryMapper installationHistoryMapper) {
        this.installationHistoryMapper = installationHistoryMapper;
    }

    findInstallationHistories
    @Override
    public List<InstallationHistory> read(CalculationContext ctx, List<Long> contractIds) {
        return installationHistoryQueryPort.findInstallations(contractIds, ctx.billingStartDate(), ctx.billingEndDate());
    }

    @Override
    public List<CalculationResult> process(CalculationContext ctx, InstallationHistory input) {
        return List.of(
                new CalculationResult(
                        null,
                        ctx.billingStartDate(),
                        ctx.billingEndDate(),
                        null,
                        "INST",
                        null,
                        null,
                        null,
                        BigDecimal.valueOf(input.fee())
        );
    }

    @Override
    public void write(CalculationContext ctx, List<CalculationResult> output) {

    }

    @Override
    public void post(CalculationContext ctx, List<CalculationResult> output) {

    }
}