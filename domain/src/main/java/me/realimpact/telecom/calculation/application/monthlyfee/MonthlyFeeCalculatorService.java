package me.realimpact.telecom.calculation.application.monthlyfee;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import org.springframework.stereotype.Service;

import lombok.RequiredArgsConstructor;
import me.realimpact.telecom.calculation.api.BillingCalculationPeriod;
import me.realimpact.telecom.calculation.api.CalculationRequest;
import me.realimpact.telecom.calculation.domain.monthlyfee.Contract;
import me.realimpact.telecom.calculation.domain.monthlyfee.DefaultPeriod;
import me.realimpact.telecom.calculation.domain.monthlyfee.MonthlyFeeCalculationResult;
import me.realimpact.telecom.calculation.domain.monthlyfee.ProratedPeriod;
import me.realimpact.telecom.calculation.port.out.ContractQueryPort;
import me.realimpact.telecom.calculation.port.out.CalculationResultSavePort;

@Service
@RequiredArgsConstructor
public class MonthlyFeeCalculatorService {

    private final ContractQueryPort contractQueryPort;
    private final CalculationResultSavePort calculationResultSavePort;

    /**
     * 전체 흐름 메서드 (기존 호환성 유지)
     * Read -> Process -> Write 를 모두 수행
     */
    public List<MonthlyFeeCalculationResult> calculate(CalculationRequest context) {
        // Read
        Contract contract = readContract(context);
        
        // Process  
        List<MonthlyFeeCalculationResult> results = processCalculation(contract, context);
        
        // Write
        writeResults(results, context);
        
        return results;
    }

    // ============= Reader 패턴 =============
    
    /**
     * Reader: 계약 데이터 읽기
     * Spring Batch의 ItemReader에서 활용 가능
     */
//    public Contract readContracts(CalculationRequest context) {
//        DefaultPeriod billingPeriod = createBillingPeriod(context);
//
//        return contractQueryPort.findContractsWithProductsChargeItemsAndSuspensions(
//                billingPeriod.getStartDate(),
//                billingPeriod.getEndDate()
//        );
//    }
//
    /**
     * Reader: 특정 계약 ID와 청구 기간으로 계약 데이터 읽기
     * Spring Batch에서 직접 사용 가능
     */
    public Contract readContract(CalculationRequest context) {
        DefaultPeriod billingPeriod = createBillingPeriod(context);
        return contractQueryPort.findContractWithProductsChargeItemsAndSuspensions(
                context.contractId(), billingPeriod.getStartDate(), billingPeriod.getEndDate()
        );
    }

    // ============= Processor 패턴 =============
    
    /**
     * Processor: 월정액 요금 계산 수행 (순수 계산 로직)
     * Spring Batch의 ItemProcessor에서 활용 가능
     */
    public List<MonthlyFeeCalculationResult> processCalculation(Contract contract, CalculationRequest context) {
        if (contract == null) {
            return List.of();
        }
        
        DefaultPeriod billingPeriod = createBillingPeriod(context);
        return processCalculation(contract, billingPeriod);
    }
    
    /**
     * Processor: 계약과 청구기간으로 월정액 요금 계산
     * Spring Batch에서 직접 사용 가능
     */
    public List<MonthlyFeeCalculationResult> processCalculation(Contract contract, DefaultPeriod billingPeriod) {
        if (contract == null) {
            return List.of();
        }
        
        // 구간 분리 - Contract가 직접 담당
        List<ProratedPeriod> proratedPeriods = contract.buildProratedPeriods(billingPeriod);

        // 계산 결과 생성
        return proratedPeriods.stream()
                .map(ProratedPeriod::calculate)
                .filter(Optional::isPresent)
                .map(Optional::get)
                .toList();
    }

    // ============= Writer 패턴 =============
    
    /**
     * Writer: 계산 결과 저장
     * Spring Batch의 ItemWriter에서 활용 가능
     */
    public void writeResults(List<MonthlyFeeCalculationResult> results, CalculationRequest context) {
        if (results == null || results.isEmpty()) {
            return;
        }
        
        DefaultPeriod billingPeriod = createBillingPeriod(context);
        calculationResultSavePort.batchSaveCalculationResults(results, billingPeriod);
    }
    
    /**
     * Writer: 계산 결과 저장 (직접 기간 지정)
     * Spring Batch에서 직접 사용 가능
     */
    public void writeResults(List<MonthlyFeeCalculationResult> results, DefaultPeriod billingPeriod) {
        if (results == null || results.isEmpty()) {
            return;
        }
        
        calculationResultSavePort.batchSaveCalculationResults(results, billingPeriod);
    }

    // ============= 유틸리티 메서드 =============
    
    /**
     * CalculationRequest로부터 청구 기간 생성
     */
    public DefaultPeriod createBillingPeriod(CalculationRequest context) {
        // 청구기간 말일까지 계산해야 하는 유형 (정기청구나 전당월의 전월. 미래요금조회 등)은 종료일에 하루를 더해준다.
        LocalDate billingEndDate = context.billingEndDate();
        if (context.billingCalculationType().includeBillingEndDate() ||
            context.billingCalculationPeriod() == BillingCalculationPeriod.PRE_BILLING_PREVIOUS_MONTH) {
            billingEndDate = context.billingEndDate().plusDays(1);
        }

        return DefaultPeriod.of(context.billingStartDate(), billingEndDate);
    }

}
