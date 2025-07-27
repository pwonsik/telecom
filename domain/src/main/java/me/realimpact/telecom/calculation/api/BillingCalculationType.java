package me.realimpact.telecom.calculation.api;

import lombok.Getter;

@Getter
public enum BillingCalculationType {
    REALTIME_CHARGE_INQUIRY("O6", "실시간요금조회"),
    FUTURE_CHARGE_INQUIRY("O8", "미래요금조회"),
    REVENUE_CONFIRMATION("B0", "매출확정"),
    REVENUE_ESTIMATION("BB", "매출추정");

    private final String code;
    private final String description;

    BillingCalculationType(String code, String description) {
        this.code = code;
        this.description = description;
    }
}
