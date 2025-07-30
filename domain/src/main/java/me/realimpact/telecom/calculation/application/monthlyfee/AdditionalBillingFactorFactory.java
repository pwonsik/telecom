package me.realimpact.telecom.calculation.application.monthlyfee;

import java.util.List;

import org.springframework.stereotype.Component;

import lombok.RequiredArgsConstructor;
import me.realimpact.telecom.calculation.domain.monthlyfee.AdditionalBillingFactors;
import me.realimpact.telecom.calculation.domain.monthlyfee.Contract;
import me.realimpact.telecom.calculation.domain.monthlyfee.ExclusiveLineContractHistory;
import me.realimpact.telecom.calculation.domain.monthlyfee.ServiceCode;
import me.realimpact.telecom.calculation.port.out.ContractQueryPort;

@Component
@RequiredArgsConstructor
public class AdditionalBillingFactorFactory {
    private final ContractQueryPort contractQueryPort;

    public List<AdditionalBillingFactors> create(Contract contract) {
        if (contract.getServiceCode() == ServiceCode.EXCLUSIVE_LINE) {
            List<ExclusiveLineContractHistory> exclusiveLineContractHistories = 
                contractQueryPort.findExclusiveLineContractHistory(contract.getContractId());
            return exclusiveLineContractHistories.stream()
                .map(ExclusiveLineContractHistory::getAdditionalBillingFactors)
                .toList();
        } 
        return List.of();
    }
}
