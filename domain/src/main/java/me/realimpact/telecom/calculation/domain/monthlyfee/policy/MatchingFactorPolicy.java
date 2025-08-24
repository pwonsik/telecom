package me.realimpact.telecom.calculation.domain.monthlyfee.policy;

import me.realimpact.telecom.calculation.domain.monthlyfee.AdditionalBillingFactor;
import me.realimpact.telecom.calculation.domain.monthlyfee.Pricing;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

public class MatchingFactorPolicy implements Pricing {

    private final List<MatchingRule> rules;

    public MatchingFactorPolicy(List<MatchingRule> rules) {
        this.rules = rules;
    }

    @Override
    public BigDecimal getPrice(List<AdditionalBillingFactor> additionalBillingFactors) {
        // return rules.stream()
        //     .filter(rule -> rule.matches(calculationPeriod.billingFactors()))
        //     .map(rule -> {
        //         BigDecimal proratedAmount = calculationPeriod.getProratedAmount(rule.amountToCharge());
        //         return new Charge(
        //                 rule.chargeName(),
        //                 proratedAmount,
        //                 calculationPeriod.period(),
        //                 calculationPeriod.productOffering(),
        //                 calculationPeriod.contractStatus()
        //         );
        //     })
        //     .toList();
        return BigDecimal.ZERO;
    }

    public static record MatchingRule(String chargeName, Map<String, String> conditions, BigDecimal amountToCharge) {
        public boolean matches(Map<String, String> factors) {
            return conditions.entrySet().stream()
                    .allMatch(entry -> entry.getValue().equals(factors.get(entry.getKey())));
        }
    }
}
