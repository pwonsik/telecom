package me.realimpact.telecom.calculation.api;

public enum BillingCalculationPeriod {
    PRE_BILLING_PREVIOUS_MONTH, // 정기 청구 전, 전월 요금
    PRE_BILLING_CURRENT_MONTH,  // 정기 청구 전, 당월 요금
    POST_BILLING_CURRENT_MONTH  // 정기 청구 후, 당월 요금
}