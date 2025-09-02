package me.realimpact.telecom.calculation.application;

import me.realimpact.telecom.calculation.api.CalculationCommandUseCase;
import me.realimpact.telecom.calculation.api.CalculationRequest;
import me.realimpact.telecom.calculation.api.CalculationResponse;
import me.realimpact.telecom.calculation.application.monthlyfee.BaseFeeCalculator;
import me.realimpact.telecom.calculation.application.vat.VatCalculator;
import me.realimpact.telecom.calculation.domain.CalculationResult;
import me.realimpact.telecom.calculation.domain.CalculationContext;
import me.realimpact.telecom.calculation.domain.onetimecharge.OneTimeChargeCalculator;
import me.realimpact.telecom.calculation.domain.onetimecharge.OneTimeChargeDataLoader;
import me.realimpact.telecom.calculation.domain.onetimecharge.OneTimeChargeDomain;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class CalculationCommandService implements CalculationCommandUseCase {

    private final BaseFeeCalculator baseFeeCalculator;
    private final VatCalculator vatCalculator;
    
    // Calculator와 DataLoader Map으로 관리
    private final Map<Class<? extends OneTimeChargeDomain>, OneTimeChargeCalculator<? extends OneTimeChargeDomain>> 
            calculatorMap;
    private final Map<Class<? extends OneTimeChargeDomain>, OneTimeChargeDataLoader<? extends OneTimeChargeDomain>> 
            dataLoaderMap;
    
    /**
     * 생성자에서 List를 Map으로 변환
     */
    public CalculationCommandService(
            BaseFeeCalculator baseFeeCalculator,
            VatCalculator vatCalculator,
            List<OneTimeChargeCalculator<? extends OneTimeChargeDomain>> calculators,
            List<OneTimeChargeDataLoader<? extends OneTimeChargeDomain>> dataLoaders) {
        
        this.baseFeeCalculator = baseFeeCalculator;
        this.vatCalculator = vatCalculator;
        
        // Calculator List를 Map으로 변환
        this.calculatorMap = calculators.stream()
            .collect(Collectors.toMap(
                OneTimeChargeCalculator::getInputType,
                calculator -> calculator
            ));
            
        // DataLoader List를 Map으로 변환
        this.dataLoaderMap = dataLoaders.stream()
            .collect(Collectors.toMap(
                OneTimeChargeDataLoader::getDataType,
                loader -> loader
            ));
    }

    @Transactional
    @Override
    public List<CalculationResponse> calculate(CalculationRequest calculationRequest) {
        CalculationContext ctx = new CalculationContext(
            calculationRequest.billingStartDate(),
            calculationRequest.billingEndDate(),
            calculationRequest.billingCalculationType(),
            calculationRequest.billingCalculationPeriod()
        );
        List<Long> contractIds = calculationRequest.contractIds();
        
        // 각 계산기 실행
        List<CalculationResult<?>> results = new ArrayList<>();
        
        // 월정액 계산 (기존 방식)
        results.addAll(baseFeeCalculator.execute(ctx, contractIds));
        
        // OneTimeCharge 계산 - Map 기반 자동 실행
        results.addAll(calculateOneTimeCharges(ctx, contractIds));
        
        // VAT 계산 (기존 결과 기반)
        List<CalculationResult<?>> vatResults = vatCalculator.calculateVat(ctx, results);
        results.addAll(vatResults);
        
        return results.stream()
            .map(calculationResult ->
                new CalculationResponse(calculationResult.getContractId(), calculationResult.getFee().longValue())
            )
            .toList();
    }
    
    /**
     * OneTimeCharge 계산 - 조건문 완전 제거
     */
    private List<CalculationResult<?>> calculateOneTimeCharges(CalculationContext context, List<Long> contractIds) {
        List<CalculationResult<?>> results = new ArrayList<>();
        
        // 모든 등록된 Calculator에 대해 실행
        for (Map.Entry<Class<? extends OneTimeChargeDomain>, OneTimeChargeCalculator<? extends OneTimeChargeDomain>> 
                entry : calculatorMap.entrySet()) {
            
            Class<? extends OneTimeChargeDomain> dataType = entry.getKey();
            OneTimeChargeCalculator<? extends OneTimeChargeDomain> calculator = entry.getValue();
            
            // 해당 타입의 DataLoader 찾기
            OneTimeChargeDataLoader<? extends OneTimeChargeDomain> dataLoader = dataLoaderMap.get(dataType);
            
            if (dataLoader != null) {
                // 데이터 로딩
                List<? extends OneTimeChargeDomain> data = executeDataLoader(dataLoader, contractIds, context);
                
                if (!data.isEmpty()) {
                    // 계산 실행
                    List<CalculationResult<?>> calculationResults = executeCalculator(calculator, context, data);
                    results.addAll(calculationResults);
                }
            }
        }
        
        return results;
    }
    
    /**
     * DataLoader 실행 (타입 안전)
     */
    @SuppressWarnings("unchecked")
    private <T extends OneTimeChargeDomain> List<T> executeDataLoader(
            OneTimeChargeDataLoader<T> loader, 
            List<Long> contractIds,
            CalculationContext context) {
        return loader.loadData(contractIds, context);
    }
    
    /**
     * Calculator 실행 (타입 안전)
     */
    @SuppressWarnings("unchecked")
    private <T extends OneTimeChargeDomain> List<CalculationResult<?>> executeCalculator(
            OneTimeChargeCalculator<T> calculator,
            CalculationContext context,
            List<? extends OneTimeChargeDomain> data) {
        
        return calculator.calculate(context, (List<T>) data);
    }

}
