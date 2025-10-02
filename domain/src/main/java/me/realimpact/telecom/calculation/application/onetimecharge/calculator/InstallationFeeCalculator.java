package me.realimpact.telecom.calculation.application.onetimecharge.calculator;

import lombok.RequiredArgsConstructor;
import me.realimpact.telecom.calculation.domain.CalculationContext;
import me.realimpact.telecom.calculation.domain.CalculationResult;
import me.realimpact.telecom.calculation.application.onetimecharge.OneTimeChargeCalculator;
import me.realimpact.telecom.calculation.domain.onetimecharge.policy.installation.InstallationHistory;
import me.realimpact.telecom.calculation.port.out.InstallationHistoryCommandPort;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.List;

/**
 * 설치비 계산기
 * 단일 책임: 설치비 계산 로직만 담당
 */
@RequiredArgsConstructor
@Component
@Order(23)
public class InstallationFeeCalculator implements OneTimeChargeCalculator<InstallationHistory> {
    private final InstallationHistoryCommandPort installationHistoryCommandPort;

    @Override
    public List<CalculationResult<InstallationHistory>> process(CalculationContext ctx, InstallationHistory input) {
        return List.of(
                new CalculationResult<>(
                    input.getContractId(),
                    ctx.billingStartDate(),
                    ctx.billingEndDate(),
                    "INST",
                    "INST",
                    "INST",
                    ctx.billingStartDate(),
                    ctx.billingEndDate(),
                    null,
                    BigDecimal.valueOf(input.getFee()),
                    BigDecimal.valueOf(input.getFee()),
                    input,
                    this::post
                )
        );
    }

    private void post(CalculationContext ctx, InstallationHistory input) {
        if (ctx.billingCalculationType().isPostable()) {
            //installationHistoryCommandPort.updateChargeStatus(input);
        }
    }
    
    // OneTimeChargeCalculator 인터페이스 구현
    @Override
    public Class<InstallationHistory> getDomainType() {
        return InstallationHistory.class;
    }

}