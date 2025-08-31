package me.realimpact.telecom.calculation.infrastructure.dto;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;

/**
 * 계약 할인 조회 결과 DTO (중첩 구조)
 * contractId를 키로 하고, 해당 계약의 모든 할인 내역을 포함
 */
@Getter
@Setter
@NoArgsConstructor
public class ContractDiscountDto {
    private Long contractId;
    private List<DiscountDto> discounts;
}