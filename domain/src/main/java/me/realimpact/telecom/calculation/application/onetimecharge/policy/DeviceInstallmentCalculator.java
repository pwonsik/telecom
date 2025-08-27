package me.realimpact.telecom.calculation.application.onetimecharge.policy;

import me.realimpact.telecom.calculation.application.Calculator;
import me.realimpact.telecom.calculation.domain.CalculationContext;
import me.realimpact.telecom.calculation.domain.CalculationResult;
import me.realimpact.telecom.calculation.domain.onetimecharge.policy.installation.InstallationHistory;
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
public class DeviceInstallmentCalculator implements Calculator<DeviceInstallmentMaster> {
    DeviceInstallmentQueryPort deviceInstallmentQueryPort;
    DeviceInstallmentCommandPort deviceInstallmentCommandPort;

    @Override
    public Map<Long, List<DeviceInstallmentMaster>> read(CalculationContext ctx, List<Long> contractIds) {
        return deviceInstallmentQueryPort.findDeviceInstallments(
                contractIds, ctx.billingStartDate(), ctx.billingEndDate()
        ).stream().collect(Collectors.groupingBy(DeviceInstallmentMaster::getContractId));
    }

    @Override
    public List<CalculationResult> process(CalculationContext ctx, DeviceInstallmentMaster input) {
        return List.of(
                new CalculationResult(
                        input.getContractId(),
                        ctx.billingStartDate(),
                        ctx.billingEndDate(),
                        "HALBU",
                        "HALBU",
                        ctx.billingStartDate(),
                        ctx.billingEndDate(),
                        null,
                        BigDecimal.valueOf(input.getFee()),
                        input
                )
        );
    }

    @Override
    public void write(CalculationContext ctx, List<CalculationResult> output) {

    }

    @Override
    public void post(CalculationContext ctx, List<CalculationResult> output) {
        output.forEach(calculationResult -> {
            deviceInstallmentCommandPort.updateChargeStatus((DeviceInstallmentMaster) calculationResult.getDomain());
        });
    }
}