package me.realimpact.telecom.calculation.port.out;

import java.time.LocalDate;
import java.util.List;

import me.realimpact.telecom.calculation.domain.monthlyfee.Contract;
import me.realimpact.telecom.calculation.domain.monthlyfee.ExclusiveLineContractHistory;

public interface ContractQueryPort {
    List<Contract> findContractWithProductsChargeItemsAndSuspensions(Long contractId, LocalDate billingStartDate, LocalDate billingEndDate);

    // mybatis에는 mapper의 명칭을 바로 넣어줘야 해서 불필요할듯.
//    Contract findContractsWithProductsChargeItemsAndSuspensions(LocalDate billingStartDate, LocalDate billingEndDate)
    List<ExclusiveLineContractHistory> findExclusiveLineContractHistory(Long contractId);
}
