package me.realimpact.telecom.testgen.entity;

import lombok.*;

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