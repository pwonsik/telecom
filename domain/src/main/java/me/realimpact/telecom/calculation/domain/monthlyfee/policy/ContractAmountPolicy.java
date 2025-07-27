package me.realimpact.telecom.calculation.domain.monthlyfee.policy;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import me.realimpact.telecom.calculation.domain.model.Charge;
import me.realimpact.telecom.calculation.domain.model.VatPolicy;
import me.realimpact.telecom.calculation.domain.monthlyfee.ProrationPeriod;

public class ContractAmountPolicy implements MonthlyChargingPolicy {
    @Override
    public List<Charge> calculate(ProrationPeriod calculationPeriod) {
        String contractAmountStr = calculationPeriod.billingFactors().getOrDefault("ContractAmount","0");
        BigDecimal contractAmount = new BigDecimal(contractAmountStr);
        BigDecimal proratedAmount = calculationPeriod.getProratedAmount(contractAmount);

        return List.of(
            new Charge(
                "계약금액",
                proratedAmount,
                calculationPeriod.period(),
                calculationPeriod.productOffering(),
                calculationPeriod.contractStatus()
            )
        );
    }
}
