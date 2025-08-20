package me.realimpact.telecom.calculation.application.onetimecharge.policy;

import me.realimpact.telecom.calculation.api.CalculationRequest;
import me.realimpact.telecom.calculation.application.onetimecharge.OneTimeChargeCalculationResult;
import me.realimpact.telecom.calculation.application.onetimecharge.OneTimeChargeCalculator;
import me.realimpact.telecom.calculation.infrastructure.dto.InstallationHistoryDto;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * 설치비 계산기
 * MyBatis로 조회한 설치내역 DTO를 입력받아 일회성 과금 계산 결과를 생성한다.
 */
@Component
public class InstallationFeeCalculator implements OneTimeChargeCalculator<InstallationHistoryDto, OneTimeChargeCalculationResult> {

    @Override
    public List<InstallationHistoryDto> read(CalculationRequest request) {
        // TODO: MyBatis를 통한 설치내역 조회 로직 구현
        return List.of();
    }

    @Override
    public OneTimeChargeCalculationResult process(InstallationHistoryDto input) {
        // TODO: 설치비 계산 로직 구현
        return null;
    }

    @Override
    public void write(List<OneTimeChargeCalculationResult> output) {
        // TODO: 계산 결과 저장 로직 구현
    }

    @Override
    public void post(List<OneTimeChargeCalculationResult> output) {
        // TODO: 후처리 로직 구현 (필요시)
    }
}