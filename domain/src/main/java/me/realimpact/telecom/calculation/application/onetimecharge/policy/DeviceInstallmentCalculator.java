package me.realimpact.telecom.calculation.application.onetimecharge.policy;

import me.realimpact.telecom.calculation.api.CalculationRequest;
import me.realimpact.telecom.calculation.application.Calculator;
import me.realimpact.telecom.calculation.domain.CalculationContext;
import me.realimpact.telecom.calculation.domain.CalculationResult;
import me.realimpact.telecom.calculation.domain.onetimecharge.policy.installment.DeviceInstallmentMaster;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * 단말할부금 계산기
 * MyBatis로 조회한 할부내역 DTO를 입력받아 일회성 과금 계산 결과를 생성한다.
 */
@Component
public class DeviceInstallmentCalculator implements Calculator<DeviceInstallmentMaster> {

    @Override
    public List<DeviceInstallmentMaster> read(CalculationContext calculationContext, List<Long> contractIds) {
        return List.of();
    }

    @Override
    public List<CalculationResult> process(CalculationContext calculationContext, DeviceInstallmentMaster input) {
        return List.of();
    }

    @Override
    public void write(CalculationContext calculationContext, List<CalculationResult> output) {

    }

    @Override
    public void post(CalculationContext calculationContext, List<CalculationResult> output) {

    }
}