package me.realimpact.telecom.calculation.domain.monthlyfee.policy;

import jakarta.annotation.PostConstruct;
import me.realimpact.telecom.calculation.domain.monthlyfee.CalculationMethod;
import me.realimpact.telecom.calculation.domain.monthlyfee.Pricing;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;

@Component
public class DefaultMonthlyChargingPolicyFactory implements MonthlyChargingPolicyFactory {

    private final Map<CalculationMethod, Pricing> policies;

    public DefaultMonthlyChargingPolicyFactory() {
        this.policies = new HashMap<>();
    }

    @PostConstruct
    public void init() {
        // MonthlyChargingPolicyKey key = new MonthlyChargingPolicyKey(ProductOffering.B2B_CONTRACT_AMOUNT, CalculationMethod.FLAT_RATE);
        // policies.put(CalculationMethod.FLAT_RATE, new FlatRatePolicy());
        // List<MatchingFactorPolicy.MatchingRule> matchingRules = List.of(
        //                 new MatchingFactorPolicy.MatchingRule("그룹A 요금", Map.of("group", "A", "level", "1"), BigDecimal.valueOf(50000)),
        //                 new MatchingFactorPolicy.MatchingRule("그룹B 요금", Map.of("group", "B"), BigDecimal.valueOf(40000))
        //         );
        // policies.put(CalculationMethod.MATCHING_FACTOR, new MatchingFactorPolicy(matchingRules));

        // List<RangeFactorPolicy.RangeRule> rangeRules = List.of(
        //                 new RangeFactorPolicy.RangeRule("데이터 10G 미만", "dataUsage", BigDecimal.ZERO, BigDecimal.TEN, BigDecimal.valueOf(30000)),
        //                 new RangeFactorPolicy.RangeRule("데이터 100G 미만", "dataUsage", BigDecimal.TEN, BigDecimal.valueOf(100), BigDecimal.valueOf(50000))
        //         );
        // policies.put(CalculationMethod.RANGE_FACTOR, new RangeFactorPolicy(rangeRules));

        // List<UnitPriceFactorPolicy.UnitPriceRule> unitPriceRules = List.of(
        //                 new UnitPriceFactorPolicy.UnitPriceRule("API 호출료", "apiCalls", BigDecimal.valueOf(10))
        //         );
        // policies.put(CalculationMethod.UNIT_PRICE_FACTOR, new UnitPriceFactorPolicy(unitPriceRules));
    }

    @Override
    public Pricing getPolicy(CalculationMethod calculationMethod) {
        return policies.get(calculationMethod);
    }
}
