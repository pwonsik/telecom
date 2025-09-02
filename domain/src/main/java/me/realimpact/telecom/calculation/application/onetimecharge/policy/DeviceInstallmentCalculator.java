package me.realimpact.telecom.calculation.application.onetimecharge.policy;

import lombok.RequiredArgsConstructor;
import me.realimpact.telecom.calculation.application.Calculator;
import me.realimpact.telecom.calculation.domain.CalculationContext;
import me.realimpact.telecom.calculation.domain.CalculationResult;
import me.realimpact.telecom.calculation.domain.onetimecharge.OneTimeChargeCalculator;
import me.realimpact.telecom.calculation.domain.onetimecharge.policy.installment.DeviceInstallmentMaster;
import me.realimpact.telecom.calculation.port.out.DeviceInstallmentCommandPort;
import me.realimpact.telecom.calculation.port.out.DeviceInstallmentQueryPort;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 단말할부금 계산기
 * MyBatis로 조회한 할부내역 DTO를 입력받아 일회성 과금 계산 결과를 생성한다.
 */
@Service
@Order(21)
@RequiredArgsConstructor
public class DeviceInstallmentCalculator implements Calculator<DeviceInstallmentMaster>, OneTimeChargeCalculator<DeviceInstallmentMaster> {
    private final DeviceInstallmentQueryPort deviceInstallmentQueryPort;
    private final DeviceInstallmentCommandPort deviceInstallmentCommandPort;

    @Override
    public Map<Long, List<DeviceInstallmentMaster>> read(CalculationContext ctx, List<Long> contractIds) {
        return deviceInstallmentQueryPort.findDeviceInstallments(
                contractIds, ctx.billingStartDate(), ctx.billingEndDate()
        ).stream().collect(Collectors.groupingBy(DeviceInstallmentMaster::getContractId));
    }

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
            deviceInstallmentCommandPort.updateChargeStatus(input);
        }
    }
    
    // OneTimeChargeCalculator 인터페이스 구현
    @Override
    public Class<DeviceInstallmentMaster> getInputType() {
        return DeviceInstallmentMaster.class;
    }
    
    @Override
    public List<CalculationResult<?>> calculate(CalculationContext context, List<DeviceInstallmentMaster> inputs) {
        return inputs.stream()
            .flatMap(master -> process(context, master).stream())
            .map(result -> (CalculationResult<?>) result)
            .toList();
    }
}