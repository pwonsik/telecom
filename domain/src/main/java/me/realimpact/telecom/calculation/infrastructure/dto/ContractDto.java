package me.realimpact.telecom.calculation.infrastructure.dto;

import java.time.LocalDate;
import java.util.List;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class ContractDto {
    private Long contractId;
    private LocalDate subscribedAt;
    private LocalDate initiallySubscribedAt;
    private LocalDate terminatedAt;
    private LocalDate prefferedTerminationDate;
    
    private List<ProductDto> products;
    private List<SuspensionDto> suspensions;
}