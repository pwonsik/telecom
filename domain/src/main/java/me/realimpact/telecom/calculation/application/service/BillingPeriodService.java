package me.realimpact.telecom.calculation.application.service;

import me.realimpact.telecom.calculation.api.BillingCalculationPeriod;
import me.realimpact.telecom.calculation.domain.CalculationContext;
import me.realimpact.telecom.calculation.domain.monthlyfee.DefaultPeriod;
import org.springframework.stereotype.Service;

import java.time.LocalDate;

/**
 * 청구기간 생성 공통 서비스
 * DataLoader와 Calculator에서 공통으로 사용하는 청구기간 생성 로직을 제공
 */
@Service
public class BillingPeriodService {

    /**
     * 청구 컨텍스트를 기반으로 청구 기간을 생성한다
     *
     * @param ctx 계산 컨텍스트
     * @return 청구 기간
     */
    public DefaultPeriod createBillingPeriod(CalculationContext ctx) {
        // 청구기간 말일까지 계산해야 하는 유형 (정기청구나 전당월의 전월. 미래요금조회 등)은 종료일에 하루를 더해준다.
        LocalDate billingEndDate = ctx.billingEndDate();
        if (ctx.billingCalculationType().includeBillingEndDate() ||
                ctx.billingCalculationPeriod() == BillingCalculationPeriod.PRE_BILLING_PREVIOUS_MONTH) {
            billingEndDate = ctx.billingEndDate().plusDays(1);
        }
        return DefaultPeriod.of(ctx.billingStartDate(), billingEndDate);
    }
}