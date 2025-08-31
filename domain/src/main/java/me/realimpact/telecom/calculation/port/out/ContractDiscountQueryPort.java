package me.realimpact.telecom.calculation.port.out;

import me.realimpact.telecom.calculation.domain.discount.ContractDiscounts;

import java.time.LocalDate;
import java.util.List;

public interface ContractDiscountQueryPort {
    List<ContractDiscounts> findContractDiscounts(List<Long> contractIds, LocalDate billingStartDate, LocalDate billingEndDate);
}