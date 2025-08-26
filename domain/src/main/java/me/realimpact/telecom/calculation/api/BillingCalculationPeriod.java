package me.realimpact.telecom.calculation.api;

import lombok.Getter;

import java.util.Arrays;
import java.util.NoSuchElementException;

@Getter
public enum BillingCalculationPeriod {
    POST_BILLING_CURRENT_MONTH("0", "당월"),  // 정기 청구 후, 당월 요금
    PRE_BILLING_PREVIOUS_MONTH("1", "전당월의 전월"), // 정기 청구 전, 전월 요금
    PRE_BILLING_CURRENT_MONTH("2", "전당월의 당월");  // 정기 청구 전, 당월 요금

    private final String code;
    private final String description;

    BillingCalculationPeriod(String code, String description) {
        this.code = code;
        this.description = description;
    }

    public static BillingCalculationPeriod fromCode(String code) {
        return Arrays.stream(BillingCalculationPeriod.values())
                .filter(period -> period.getCode().equals(code))
                .findFirst()
                .orElseThrow(() -> new NoSuchElementException("No enum constant with code " + code));
    }
}