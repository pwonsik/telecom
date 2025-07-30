package me.realimpact.telecom.calculation.domain.monthlyfee.policy;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import me.realimpact.telecom.calculation.domain.monthlyfee.MonthlyChargingPolicy;
import me.realimpact.telecom.calculation.domain.monthlyfee.MonthlyFeeCalculationResult;
import me.realimpact.telecom.calculation.domain.monthlyfee.ProrationPeriod;

public class MatchingFactorPolicy implements MonthlyChargingPolicy {

    private final List<MatchingRule> rules;

    public MatchingFactorPolicy(List<MatchingRule> rules) {
        this.rules = rules;
    }

    @Override
    public Optional<MonthlyFeeCalculationResult> calculate(ProrationPeriod calculationPeriod) {
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
        return Optional.empty();
    }

    public record MatchingRule(String chargeName, Map<String, String> conditions, BigDecimal amountToCharge) {
        public boolean matches(Map<String, String> factors) {
            return conditions.entrySet().stream()
                    .allMatch(entry -> entry.getValue().equals(factors.get(entry.getKey())));
        }
    }
}
