package me.realimpact.telecom.calculation.port.out;

import me.realimpact.telecom.calculation.domain.monthlyfee.ContractWithProductsAndSuspensions;

import java.time.LocalDate;
import java.util.List;

public interface ContractQueryPort {
    List<ContractWithProductsAndSuspensions> findContractsAndProductInventoriesByContractIds(
        List<Long> contractIds, LocalDate billingStartDate, LocalDate billingEndDate
    );

    List<Long> findContractIds(Long contractId);
}
