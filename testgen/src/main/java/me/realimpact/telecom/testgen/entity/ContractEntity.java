package me.realimpact.telecom.testgen.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

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