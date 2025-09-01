package me.realimpact.telecom.calculation.infrastructure.dto;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * 개별 할인 정보 DTO
 * contract_discount 테이블의 할인 관련 컬럼과 매핑
 */
@Getter
@Setter
@NoArgsConstructor
public class DiscountDto {
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