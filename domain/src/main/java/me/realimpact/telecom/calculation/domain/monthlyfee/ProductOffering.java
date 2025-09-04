package me.realimpact.telecom.calculation.domain.monthlyfee;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.ToString;

import java.util.List;

@RequiredArgsConstructor
@Getter
@ToString
public class ProductOffering {
    private final String productOfferingId; 
    private final String productOfferingName; 
    private final List<ChargeItem> chargeItems;
}
