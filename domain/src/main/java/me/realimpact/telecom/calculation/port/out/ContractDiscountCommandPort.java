package me.realimpact.telecom.calculation.port.out;

import me.realimpact.telecom.calculation.domain.discount.ContractDiscounts;
import me.realimpact.telecom.calculation.domain.discount.Discount;

public interface ContractDiscountCommandPort {
    void applyDiscount(Discount discount);
}