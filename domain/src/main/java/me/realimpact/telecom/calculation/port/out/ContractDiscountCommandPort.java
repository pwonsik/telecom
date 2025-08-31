package me.realimpact.telecom.calculation.port.out;

import me.realimpact.telecom.calculation.domain.discount.ContractDiscounts;

public interface ContractDiscountCommandPort {
    void updateDiscountStatus(ContractDiscounts contractDiscounts);
}