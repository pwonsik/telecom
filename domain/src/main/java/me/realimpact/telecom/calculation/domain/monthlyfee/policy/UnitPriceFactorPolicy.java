package me.realimpact.telecom.calculation.domain.monthlyfee.policy;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

import me.realimpact.telecom.calculation.domain.model.BillingFactor;
import me.realimpact.telecom.calculation.domain.model.Charge;
import me.realimpact.telecom.calculation.domain.model.VatPolicy;
import me.realimpact.telecom.calculation.domain.monthlyfee.ProrationPeriod;

public class UnitPriceFactorPolicy implements MonthlyChargingPolicy {

    private final List<UnitPriceRule> rules;

    public UnitPriceFactorPolicy(List<UnitPriceRule> rules) {
        this.rules = rules;
    }

    @Override
    public List<Charge> calculate(ProrationPeriod calculationPeriod) {
        return rules.stream()
            .map(rule -> rule.calculateAmount(calculationPeriod.billingFactors()))
            .filter(Objects::nonNull)
            .map(chargeInfo -> {
                BigDecimal proratedAmount = calculationPeriod.getProratedAmount(chargeInfo.amountToCharge());
                return new Charge(
                    chargeInfo.chargeName(),
                    proratedAmount,
                    calculationPeriod.period(),
                    calculationPeriod.productOffering(),
                    calculationPeriod.contractStatus()
                );
            })
            .toList();
    }

    private record ChargeInfo(String chargeName, BigDecimal amountToCharge) {}

    public record UnitPriceRule(String chargeName, String factorName, BigDecimal unitPrice) {
        public ChargeInfo calculateAmount(Map<String, String> bf) {
            String factorValueStr = bf.get(factorName);
            if (factorValueStr == null) {
                return null;
            }

            try {
                BigDecimal quantity = new BigDecimal(factorValueStr);
                BigDecimal totalAmount = quantity.multiply(unitPrice);
                return new ChargeInfo(chargeName, totalAmount);
            } catch (NumberFormatException e) {
                // Log error if necessary
                return null;
            }
        }
    }
}
