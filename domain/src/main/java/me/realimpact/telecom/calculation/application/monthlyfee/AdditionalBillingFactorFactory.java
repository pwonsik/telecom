package me.realimpact.telecom.calculation.application.monthlyfee;

import lombok.RequiredArgsConstructor;
import me.realimpact.telecom.calculation.domain.monthlyfee.AdditionalBillingFactor;
import me.realimpact.telecom.calculation.domain.monthlyfee.ContractWithProductsAndSuspensions;
import me.realimpact.telecom.calculation.domain.monthlyfee.ExclusiveLineContractHistory;
import me.realimpact.telecom.calculation.domain.monthlyfee.ServiceCode;
import me.realimpact.telecom.calculation.port.out.ContractQueryPort;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@RequiredArgsConstructor
public class AdditionalBillingFactorFactory {
    private final ContractQueryPort contractQueryPort;

    public List<AdditionalBillingFactor> create(ContractWithProductsAndSuspensions contractWithProductsAndSuspensions) {
//        if (contractWithProductsAndSuspensions.getServiceCode() == ServiceCode.EXCLUSIVE_LINE) {
//            List<ExclusiveLineContractHistory> exclusiveLineContractHistories =
//                contractQueryPort.findExclusiveLineContractHistory(contractWithProductsAndSuspensions.getContractId());
//            return exclusiveLineContractHistories.stream()
//                .map(ExclusiveLineContractHistory::getAdditionalBillingFactors)
//                .toList();
//        }
        return List.of();
    }
}
