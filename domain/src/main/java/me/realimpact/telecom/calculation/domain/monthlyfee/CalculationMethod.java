package me.realimpact.telecom.calculation.domain.monthlyfee;

import lombok.Getter;

import java.util.Arrays;

@Getter
public enum CalculationMethod {
    FLAT_RATE("00","일반적인 정액 요율"),
    MATCHING_FACTOR("01","과금요소 매칭 요금"),
    RANGE_FACTOR("02","범위 요금"),
    UNIT_PRICE_FACTOR("03","단가 요금"),
    STEP_FACTOR("04", "구간별 누적 요금"),
    TIER_FACTOR("05", "구간별 통합 요금");

    private final String code;
    private final String description;

    CalculationMethod(String code, String description) {
        this.code = code;
        this.description = description;
    }

    public static CalculationMethod fromCode(String code) {
        return Arrays.stream(values())
                .filter(method -> method.code.equals(code))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Unknown calculation method code: " + code));
    }
}
