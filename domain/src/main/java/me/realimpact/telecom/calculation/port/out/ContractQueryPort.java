package me.realimpact.telecom.calculation.port.out;

import java.util.List;


import me.realimpact.telecom.calculation.domain.monthlyfee.Contract;
import me.realimpact.telecom.calculation.domain.monthlyfee.ExclusiveLineContractHistory;
import me.realimpact.telecom.calculation.domain.monthlyfee.Suspension;

public interface ContractQueryPort {
    Contract findByContractId(Long contractId);
    List<Suspension> findSuspensionHistory(Long contractId);
    List<ExclusiveLineContractHistory> findExclusiveLineContractHistory(Long contractId);
}
