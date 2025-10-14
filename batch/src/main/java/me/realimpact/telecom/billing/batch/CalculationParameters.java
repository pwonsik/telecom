package me.realimpact.telecom.billing.batch;

import me.realimpact.telecom.calculation.api.BillingCalculationPeriod;
import me.realimpact.telecom.calculation.api.BillingCalculationType;
import me.realimpact.telecom.calculation.domain.CalculationContext;

import java.time.LocalDate;
import java.util.List;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * 배치 작업에서 사용되는 계산 관련 파라미터를 담는 데이터 클래스.
 */
@Getter
@RequiredArgsConstructor
public class CalculationParameters {
    /**
     * 청구 시작일
     */
    private final LocalDate billingStartDate;
    /**
     * 청구 종료일
     */
    private final LocalDate billingEndDate;
    /**
     * 청구 계산 유형 (예: 일반, 최종)
     */
    private final BillingCalculationType billingCalculationType;
    /**
     * 청구 계산 기간 (예: 월별, 일별)
     */
    private final BillingCalculationPeriod billingCalculationPeriod;
    /**
     * 배치 처리 스레드 개수
     */
    private final int threadCount;
    /**
     * 특정 계약 ID 목록 (비어있을 경우 전체 계약 대상)
     */
    private final List<Long> contractIds;

    /**
     * 이 파라미터 객체를 기반으로 CalculationContext 객체를 생성하여 반환한다.
     * @return CalculationContext 객체
     */
    public CalculationContext toCalculationContext() {
        return new CalculationContext(
                billingStartDate,
                billingEndDate,
                billingCalculationType,
                billingCalculationPeriod
        );
    }
}
