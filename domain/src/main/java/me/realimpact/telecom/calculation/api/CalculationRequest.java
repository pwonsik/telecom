package me.realimpact.telecom.calculation.api;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import java.time.LocalDate;
import java.util.List;

public record CalculationRequest(
    @NotEmpty(message = "계약 ID 목록은 비어있을 수 없습니다")
    List<Long> contractIds,
    
    @NotNull(message = "청구 시작일은 필수입니다")
    LocalDate billingStartDate, 
    
    @NotNull(message = "청구 종료일은 필수입니다")
    LocalDate billingEndDate,
    
    @NotNull(message = "청구 계산 유형은 필수입니다")
    BillingCalculationType billingCalculationType,
    
    @NotNull(message = "청구 계산 주기는 필수입니다")
    BillingCalculationPeriod billingCalculationPeriod) {

}
