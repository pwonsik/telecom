package me.realimpact.telecom.testgen.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ProductEntity {
    private Long contractId;
    private String productOfferingId;
    private LocalDateTime effectiveStartDateTime;
    private LocalDateTime effectiveEndDateTime;
    private LocalDate subscribedAt;
    private LocalDate activatedAt;
    private LocalDate terminatedAt;
}