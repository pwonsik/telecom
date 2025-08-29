package me.realimpact.telecom.calculation.infrastructure.converter;

import me.realimpact.telecom.calculation.domain.discount.ContractDiscount;
import me.realimpact.telecom.calculation.infrastructure.dto.ContractDiscountDto;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * 일회성 과금 도메인 객체와 DTO 간의 변환을 담당하는 컨버터
 */
@Component
public class ContractDiscountDtoConverter {

    /**
     * ContractDiscountDto를 ContractDiscount 도메인 객체로 변환
     */
    public ContractDiscount convertToContractDiscount(ContractDiscountDto dto) {
        return new ContractDiscount(
            dto.getContractId(),
            dto.getDiscountId(),
            dto.getDiscountStartDate(),
            dto.getDiscountEndDate(),
            dto.getProductOfferingId(),
            dto.getDiscountApplyUnit(),
            dto.getDiscountAmount(),
            dto.getDiscountRate(),
                dto.getDiscountAppliedAmount()
        );
    }

    /**
     * ContractDiscountDto 리스트를 ContractDiscount 도메인 객체 리스트로 변환
     */
    public List<ContractDiscount> convertToContractDiscounts(List<ContractDiscountDto> dtos) {
        return dtos.stream()
            .map(this::convertToContractDiscount)
            .toList();
    }
}