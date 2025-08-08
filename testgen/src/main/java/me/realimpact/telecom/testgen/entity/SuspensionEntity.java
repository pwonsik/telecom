package me.realimpact.telecom.testgen.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SuspensionEntity {
    private Long contractId;
    private String suspensionTypeCode;
    private LocalDateTime effectiveStartDateTime;
    private LocalDateTime effectiveEndDateTime;
    private String suspensionTypeDescription;
}