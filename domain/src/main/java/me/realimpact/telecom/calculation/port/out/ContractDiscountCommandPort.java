package me.realimpact.telecom.calculation.port.out;

import me.realimpact.telecom.calculation.domain.discount.ContractDiscount;

public interface ContractDiscountCommandPort {
    void updateDiscountStatus(ContractDiscount contractDiscount);
}