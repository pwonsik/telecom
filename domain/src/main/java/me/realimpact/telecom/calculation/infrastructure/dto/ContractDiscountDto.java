package me.realimpact.telecom.calculation.infrastructure.dto;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * 계약 할인 조회 결과 DTO
 * contract_discount 테이블과 매핑
 */
@Getter
@Setter
@NoArgsConstructor
public class ContractDiscountDto {
    private Long contractId;
    private String discountId;
    private LocalDate discountStartDate;
    private LocalDate discountEndDate;
    private String productOfferingId;
    private String discountApplyUnit;
    private Long discountAmount;
    private BigDecimal discountRate;
    private BigDecimal discountAppliedAmount;
}