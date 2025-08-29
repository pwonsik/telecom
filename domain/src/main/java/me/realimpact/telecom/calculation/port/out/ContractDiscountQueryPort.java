package me.realimpact.telecom.calculation.port.out;

import me.realimpact.telecom.calculation.domain.onetimecharge.policy.discount.ContractDiscount;

import java.time.LocalDate;
import java.util.List;

public interface ContractDiscountQueryPort {
    List<ContractDiscount> findContractDiscounts(List<Long> contractIds, LocalDate billingStartDate, LocalDate billingEndDate);
}