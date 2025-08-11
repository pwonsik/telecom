package me.realimpact.telecom.calculation.port.out;

import java.time.LocalDate;
import java.util.List;

import me.realimpact.telecom.calculation.domain.monthlyfee.Contract;
import me.realimpact.telecom.calculation.domain.monthlyfee.ExclusiveLineContractHistory;

public interface ContractQueryPort {
    Contract findContractWithProductsChargeItemsAndSuspensions(Long contractId, LocalDate billingStartDate, LocalDate billingEndDate);
    List<ExclusiveLineContractHistory> findExclusiveLineContractHistory(Long contractId);
}
