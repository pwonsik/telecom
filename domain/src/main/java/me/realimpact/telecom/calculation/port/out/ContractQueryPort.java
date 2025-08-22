package me.realimpact.telecom.calculation.port.out;

import java.time.LocalDate;
import java.util.List;

import me.realimpact.telecom.calculation.domain.monthlyfee.ContractWithProductsAndSuspensions;

public interface ContractQueryPort {
    List<ContractWithProductsAndSuspensions> findContractsAndProductInventoriesByContractIds(List<Long> contractIds, LocalDate billingStartDate, LocalDate billingEndDate);
}
