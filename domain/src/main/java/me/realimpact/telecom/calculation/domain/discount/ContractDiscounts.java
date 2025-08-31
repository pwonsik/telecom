package me.realimpact.telecom.calculation.domain.discount;

import java.util.List;

/**
 * 계약 할인 정보
 * 계약 ID를 키로 하고, 해당 계약의 모든 할인 내역을 리스트로 관리한다.
 *
 * @param contractId 계약 ID
 * @param discounts  할인 내역 리스트
 */
public record ContractDiscounts(Long contractId, List<Discount> discounts) {

}