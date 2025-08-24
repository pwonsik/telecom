package me.realimpact.telecom.calculation.infrastructure.dto;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
public class SuspensionDto {
    private Long contractId;
    private String suspensionTypeCode;
    private LocalDateTime effectiveStartDateTime;
    private LocalDateTime effectiveEndDateTime;
    private String suspensionTypeDescription;
}