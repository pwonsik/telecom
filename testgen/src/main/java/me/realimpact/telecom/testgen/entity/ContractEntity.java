package me.realimpact.telecom.testgen.entity;

import lombok.*;

import java.time.LocalDate;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ContractEntity {
    private Long contractId;
    private LocalDate subscribedAt;
    private LocalDate initiallySubscribedAt;
    private LocalDate terminatedAt;
    private LocalDate prefferedTerminationDate;
}