package me.realimpact.telecom.calculation.domain.monthlyfee;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;

@RequiredArgsConstructor
@Getter
public class Suspension extends Temporal {

    private final LocalDateTime effectiveStartDateTime;
    private final LocalDateTime effectiveEndDateTime;
    private final SuspensionType suspensionType;

    @Override
    public LocalDate getStartDate() {
        return effectiveStartDateTime.toLocalDate();
    }

    @Override
    public LocalDate getEndDate() {
        return effectiveEndDateTime.toLocalDate();
    }

    @Getter
    public enum SuspensionType {
        TEMPORARY_SUSPENSION("F1","일시정지"),
        NON_PAYMENT_SUSPENSION("F3","미납정지");

        private final String code;
        private final String description;

        SuspensionType(String code, String description) {
            this.code = code;
            this.description = description;
        }

        public static SuspensionType fromCode(String code) {
            return java.util.Arrays.stream(values())
                    .filter(type -> type.code.equals(code))
                    .findFirst()
                    .orElseThrow(() -> new IllegalArgumentException("Unknown suspension type code: " + code));
        }
    }
}