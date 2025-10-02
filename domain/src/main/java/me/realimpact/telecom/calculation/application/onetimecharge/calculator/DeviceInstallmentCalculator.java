package me.realimpact.telecom.calculation.application.onetimecharge.calculator;

import lombok.RequiredArgsConstructor;
import me.realimpact.telecom.calculation.domain.CalculationContext;
import me.realimpact.telecom.calculation.domain.CalculationResult;
import me.realimpact.telecom.calculation.application.onetimecharge.OneTimeChargeCalculator;
import me.realimpact.telecom.calculation.domain.onetimecharge.policy.installment.DeviceInstallmentMaster;
import me.realimpact.telecom.calculation.port.out.DeviceInstallmentCommandPort;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.List;

/**
 * 단말할부금 계산기
 * 단일 책임: 단말할부금 계산 로직만 담당
 */
@Component
@Order(21)
@RequiredArgsConstructor
public class DeviceInstallmentCalculator implements OneTimeChargeCalculator<DeviceInstallmentMaster> {
    private final DeviceInstallmentCommandPort deviceInstallmentCommandPort;

    @Override
    public List<CalculationResult<DeviceInstallmentMaster>> process(CalculationContext ctx, DeviceInstallmentMaster input) {
        BigDecimal fee = BigDecimal.valueOf(input.getFee(ctx.billingCalculationType(), ctx.billingCalculationPeriod()));
        BigDecimal balance = new BigDecimal(fee.unscaledValue(), fee.scale());
        return List.of(
                new CalculationResult<>(
                        input.getContractId(),
                        ctx.billingStartDate(),
                        ctx.billingEndDate(),
                        "HALBU",
                        "HALBU",
                        "HALBU",
                        ctx.billingStartDate(),
                        ctx.billingEndDate(),
                        null,
                        fee,
                        balance,
                        input,
                        this::post
                )
        );
    }


    public void post(CalculationContext ctx, DeviceInstallmentMaster input) {
        if (ctx.billingCalculationType().isPostable()) {
            //deviceInstallmentCommandPort.updateChargeStatus(input);
        }
    }
    
    // OneTimeChargeCalculator 인터페이스 구현
    @Override
    public Class<DeviceInstallmentMaster> getDomainType() {
        return DeviceInstallmentMaster.class;
    }

}