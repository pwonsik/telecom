package me.realimpact.telecom.calculation.infrastructure.adapter;

import lombok.RequiredArgsConstructor;
import me.realimpact.telecom.calculation.domain.discount.ContractDiscounts;
import me.realimpact.telecom.calculation.domain.discount.Discount;
import me.realimpact.telecom.calculation.infrastructure.adapter.mybatis.ContractDiscountMapper;
import me.realimpact.telecom.calculation.infrastructure.converter.ContractDiscountDtoConverter;
import me.realimpact.telecom.calculation.infrastructure.dto.ContractDiscountDto;
import me.realimpact.telecom.calculation.port.out.ContractDiscountCommandPort;
import me.realimpact.telecom.calculation.port.out.ContractDiscountQueryPort;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;

@Repository
@RequiredArgsConstructor
public class ContractDiscountRepository implements ContractDiscountQueryPort, ContractDiscountCommandPort {
    private final ContractDiscountMapper contractDiscountMapper;
    private final ContractDiscountDtoConverter contractDiscountDtoConverter;

    @Override
    public List<ContractDiscounts> findContractDiscounts(
        List<Long> contractIds, LocalDate billingStartDate, LocalDate billingEndDate
    ) {
        List<ContractDiscountDto> contractDiscountDtos =
            contractDiscountMapper.findDiscountsByContractIds(contractIds, billingStartDate, billingEndDate);
        return contractDiscountDtoConverter.convertToContractDiscounts(contractDiscountDtos);
    }

    @Override
    public void applyDiscount(Discount discount) {
        contractDiscountMapper.applyDiscount(discount);
    }
}