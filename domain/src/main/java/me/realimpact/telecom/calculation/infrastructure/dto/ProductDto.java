package me.realimpact.telecom.calculation.infrastructure.dto;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class ProductDto {
    private Long contractId;
    private String productOfferingId;
    private LocalDateTime effectiveStartDateTime;
    private LocalDateTime effectiveEndDateTime;
    private LocalDate subscribedAt;
    private LocalDate activatedAt;
    private LocalDate terminatedAt;
    
    private String productOfferingName;
    
    private List<MonthlyChargeItemDto> monthlyChargeItems;
}