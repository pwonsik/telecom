package me.realimpact.telecom.calculation.domain;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import me.realimpact.telecom.calculation.domain.monthlyfee.Suspension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Optional;

@Getter
@Setter
@RequiredArgsConstructor
public class CalculationResult<I> {
    private final Long contractId;
    private final LocalDate billingStartDate;
    private final LocalDate billingEndDate;
    private final String productOfferingId;
    private final String chargeItemId;
    private final String revenueItemId;
    private final LocalDate effectiveStartDate;
    private final LocalDate effectiveEndDate;
    private final Suspension.SuspensionType suspensionType;
    private final BigDecimal fee;
    private final I domain;
    private final PostProcessor<I> postProcessor;
    
    /**
     * 후처리 작업을 실행한다
     * 
     * @param ctx 계산 컨텍스트
     */
    public void executePost(CalculationContext ctx) {
        if (postProcessor != null) {
            postProcessor.process(ctx, domain);
        }
    }
}