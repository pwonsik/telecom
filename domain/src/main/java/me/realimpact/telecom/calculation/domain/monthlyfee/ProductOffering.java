package me.realimpact.telecom.calculation.domain.monthlyfee;

import java.util.List;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
@Getter
public class ProductOffering {
    private final String productOfferingId; 
    private final String productOfferingName; 
    private final List<MonthlyChargeItem> monthlyChargeItems;
}
