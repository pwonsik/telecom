package me.realimpact.telecom.calculation.domain.monthlyfee.policy;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

import me.realimpact.telecom.calculation.domain.model.BillingFactor;
import me.realimpact.telecom.calculation.domain.model.Charge;
import me.realimpact.telecom.calculation.domain.model.VatPolicy;
import me.realimpact.telecom.calculation.domain.monthlyfee.ProrationPeriod;

public class RangeFactorPolicy implements MonthlyChargingPolicy {

    private final List<RangeRule> rules;

    public RangeFactorPolicy(List<RangeRule> rules) {
        this.rules = rules;
    }

    @Override
    public List<Charge> calculate(ProrationPeriod calculationPeriod) {
        return rules.stream()
            .map(rule -> rule.findMatch(calculationPeriod.billingFactors()))
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

    public record RangeRule(String chargeName, String factorName, BigDecimal from, BigDecimal to, BigDecimal amountToCharge) {
        public ChargeInfo findMatch(Map<String, String> bf) {
            String factorValueStr = bf.get(factorName);
            if (factorValueStr == null) {
                return null;
            }

            try {
                BigDecimal factorValue = new BigDecimal(factorValueStr);
                if (factorValue.compareTo(from) >= 0 && factorValue.compareTo(to) < 0) {
                    return new ChargeInfo(chargeName, amountToCharge);
                }
            } catch (NumberFormatException e) {
                // Log error if necessary
                return null;
            }
            return null;
        }
    }
}
