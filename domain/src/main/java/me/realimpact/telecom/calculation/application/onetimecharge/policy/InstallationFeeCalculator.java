package me.realimpact.telecom.calculation.application.onetimecharge.policy;

import lombok.RequiredArgsConstructor;
import me.realimpact.telecom.calculation.application.Calculator;
import me.realimpact.telecom.calculation.domain.CalculationContext;
import me.realimpact.telecom.calculation.domain.CalculationResult;
import me.realimpact.telecom.calculation.domain.onetimecharge.OneTimeChargeCalculator;
import me.realimpact.telecom.calculation.domain.onetimecharge.policy.installation.InstallationHistory;
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
public class InstallationFeeCalculator implements Calculator<InstallationHistory>, OneTimeChargeCalculator<InstallationHistory> {
    private final InstallationHistoryQueryPort installationHistoryQueryPort;
    private final InstallationHistoryCommandPort installationHistoryCommandPort;

    @Override
    public Map<Long, List<InstallationHistory>> read(CalculationContext ctx, List<Long> contractIds) {
        return installationHistoryQueryPort.findInstallations(
            contractIds, ctx.billingStartDate(), ctx.billingEndDate()
        ).stream().collect(Collectors.groupingBy(InstallationHistory::contractId));
    }

    @Override
    public List<CalculationResult<InstallationHistory>> process(CalculationContext ctx, InstallationHistory input) {
        return List.of(
                new CalculationResult<>(
                    input.contractId(),
                    ctx.billingStartDate(),
                    ctx.billingEndDate(),
                    "INST",
                    "INST",
                    "INST",
                    ctx.billingStartDate(),
                    ctx.billingEndDate(),
                    null,
                    BigDecimal.valueOf(input.fee()),
                    BigDecimal.valueOf(input.fee()),
                    input,
                    this::post
                )
        );
    }

    private void post(CalculationContext ctx, InstallationHistory input) {
        if (ctx.billingCalculationType().isPostable()) {
            installationHistoryCommandPort.updateChargeStatus(input);
        }
    }
    
    // OneTimeChargeCalculator 인터페이스 구현
    @Override
    public Class<InstallationHistory> getInputType() {
        return InstallationHistory.class;
    }
    
    @Override
    public List<CalculationResult<?>> calculate(CalculationContext context, List<InstallationHistory> inputs) {
        return inputs.stream()
            .flatMap(history -> process(context, history).stream())
            .map(result -> (CalculationResult<?>) result)
            .toList();
    }
}