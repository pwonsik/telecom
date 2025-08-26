package me.realimpact.telecom.calculation.application.onetimecharge.policy;

import lombok.RequiredArgsConstructor;
import me.realimpact.telecom.calculation.application.Calculator;
import me.realimpact.telecom.calculation.domain.CalculationContext;
import me.realimpact.telecom.calculation.domain.CalculationResult;
import me.realimpact.telecom.calculation.domain.onetimecharge.policy.installation.InstallationHistory;
import me.realimpact.telecom.calculation.infrastructure.adapter.mybatis.InstallationHistoryMapper;
import me.realimpact.telecom.calculation.port.out.CalculationResultSavePort;
import me.realimpact.telecom.calculation.port.out.InstallationHistoryCommandPort;
import me.realimpact.telecom.calculation.port.out.InstallationHistoryQueryPort;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 설치비 계산기
 * MyBatis로 조회한 설치내역 DTO를 입력받아 일회성 과금 계산 결과를 생성한다.
 */
@RequiredArgsConstructor
@Service
@Order(23)
public class InstallationFeeCalculator implements Calculator<InstallationHistory> {
    private final InstallationHistoryQueryPort installationHistoryQueryPort;
    private final InstallationHistoryCommandPort installationHistoryCommandPort;
    private final CalculationResultSavePort calculationResultSavePort;

    @Override
    public Map<Long, List<InstallationHistory>> read(CalculationContext ctx, List<Long> contractIds) {
        return installationHistoryQueryPort.findInstallations(
            contractIds, ctx.billingStartDate(), ctx.billingEndDate()
        ).stream().collect(Collectors.groupingBy(InstallationHistory::contractId));
    }

    @Override
    public List<CalculationResult> process(CalculationContext ctx, InstallationHistory input) {
        return List.of(
                new CalculationResult(
                        input.contractId(),
                        ctx.billingStartDate(),
                        ctx.billingEndDate(),
                        "INST",
                        "INST",
                        ctx.billingStartDate(),
                        ctx.billingEndDate(),
                        null,
                        BigDecimal.valueOf(input.fee()),
                        input
                )
        );
    }

    @Override
    public void write(CalculationContext ctx, List<CalculationResult> output) {
        calculationResultSavePort.save(ctx, output);
    }

    @Override
    public void post(CalculationContext ctx, List<CalculationResult> output) {
        output.forEach(calculationResult -> {
            installationHistoryCommandPort.updateChargeStatus((InstallationHistory)calculationResult.getDomain());
        });
    }
}