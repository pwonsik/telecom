package me.realimpact.telecom.calculation.domain.monthlyfee;

import java.util.Optional;

/* 하나의 상세 구간/상품/과금항목에 대한 계산로직을 책임진다. 이의 구현체는 policy 패키지에 개발한다.*/
public interface MonthlyChargingPolicy {
    Optional<MonthlyFeeCalculationResult> calculate(ProrationPeriod prorationPeriod);
}
