package me.realimpact.telecom.calculation.api;

import lombok.Getter;

import java.util.Arrays;
import java.util.NoSuchElementException;

@Getter
public enum BillingCalculationType {
    REALTIME_CHARGE_INQUIRY("O6", "실시간요금조회"),
    FUTURE_CHARGE_INQUIRY("O8", "미래요금조회"),
    REVENUE_CONFIRMATION("B0", "매출확정"),
    REVENUE_ESTIMATION("BB", "매출추정"),
    EXPECTATION_PENALTY_CREATION("BH", "예상해지비용적재"),
    EXPECTATION_PENALTY_INQUIRY("OZ", "예상할인반환금조회"),
    TERMINATION_INQUIRY("O1", "해지핫빌"),
    PREVIEW_INQUIRY("OP", "미리보기"),
    BF_SALE_INQUIRY("OB", "스마트플래너_요금조회");

    private final String code;
    private final String description;

    BillingCalculationType(String code, String description) {
        this.code = code;
        this.description = description;
    }

    public boolean includeBillingEndDate() {
        return this == REVENUE_CONFIRMATION || this == REVENUE_ESTIMATION || this == FUTURE_CHARGE_INQUIRY;
    }

    public boolean isTerminationAssumed() {
        return this == TERMINATION_INQUIRY || this == EXPECTATION_PENALTY_CREATION || this == EXPECTATION_PENALTY_INQUIRY;
    }

    public static BillingCalculationType fromCode(String code) {
        return Arrays.stream(BillingCalculationType.values())
                .filter(period -> period.getCode().equals(code))
                .findFirst()
                .orElseThrow(() -> new NoSuchElementException("No enum constant with code " + code));
    }

    public boolean isPostable() {
        return this == REVENUE_CONFIRMATION;
    }
}
