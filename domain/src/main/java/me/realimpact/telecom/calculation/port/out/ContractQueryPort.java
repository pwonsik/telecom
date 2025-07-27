package me.realimpact.telecom.calculation.port.out;

import java.util.List;

import me.realimpact.telecom.calculation.domain.monthlyfee.AdditionalBillingFactors;
import me.realimpact.telecom.calculation.domain.monthlyfee.Contract;
import me.realimpact.telecom.calculation.domain.monthlyfee.Suspension;

public interface ContractQueryPort {
    Contract findByContractId(Long contractId);
    List<Suspension> findSuspensionHistory(Long contractId);
    List<AdditionalBillingFactors> findAdditionalBillingFactors(Long contractId);
}
