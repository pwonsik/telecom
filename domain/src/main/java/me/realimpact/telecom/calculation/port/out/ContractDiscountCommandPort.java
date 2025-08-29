package me.realimpact.telecom.calculation.port.out;

import me.realimpact.telecom.calculation.domain.onetimecharge.policy.discount.ContractDiscount;

public interface ContractDiscountCommandPort {
    void updateDiscountStatus(ContractDiscount contractDiscount);
}