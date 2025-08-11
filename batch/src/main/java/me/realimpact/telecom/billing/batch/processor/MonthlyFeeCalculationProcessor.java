package me.realimpact.telecom.billing.batch.processor;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import me.realimpact.telecom.calculation.application.monthlyfee.MonthlyFeeCalculatorService;
import me.realimpact.telecom.calculation.domain.monthlyfee.Contract;
import me.realimpact.telecom.calculation.domain.monthlyfee.DefaultPeriod;
import me.realimpact.telecom.calculation.domain.monthlyfee.MonthlyFeeCalculationResult;
import me.realimpact.telecom.calculation.infrastructure.converter.DtoToDomainConverter;
import me.realimpact.telecom.calculation.infrastructure.dto.ContractDto;
import org.springframework.batch.core.configuration.annotation.StepScope;
import org.springframework.batch.item.ItemProcessor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.util.List;

/**
 * Spring Batch ItemProcessor 구현체
 * ContractDto를 받아서 월정액 계산을 수행하고 결과를 반환
 */
@StepScope
@RequiredArgsConstructor
@Slf4j
public class MonthlyFeeCalculationProcessor implements ItemProcessor<ContractDto, List<MonthlyFeeCalculationResult>> {

    private final MonthlyFeeCalculatorService monthlyFeeCalculatorService;
    private final DtoToDomainConverter dtoToDomainConverter;
    
    @Value("#{jobParameters['billingStartDate']}")
    private String billingStartDateStr;
    
    @Value("#{jobParameters['billingEndDate']}")
    private String billingEndDateStr;

    @Override
    public List<MonthlyFeeCalculationResult> process(ContractDto contractDto) throws Exception {
        try {
            log.debug("Processing contract calculation for contractId: {}", contractDto.getContractId());
            
            // 1. DTO를 도메인 객체로 변환
            Contract contract = dtoToDomainConverter.convertToContract(contractDto);
            
            // 2. 청구 기간 생성 (Job Parameters에서 가져옴)
            DefaultPeriod billingPeriod = createBillingPeriodFromJobParameters();
            
            // 3. 월정액 계산 수행 (순수 계산 로직만)
            List<MonthlyFeeCalculationResult> results = monthlyFeeCalculatorService.processCalculation(contract, billingPeriod);
            
            log.debug("Calculated {} results for contractId: {}", results.size(), contractDto.getContractId());
            
            return results;
            
        } catch (Exception e) {
            log.error("Failed to process contract calculation for contractId: {}", contractDto.getContractId(), e);
            throw e;
        }
    }
    
    /**
     * Job Parameters에서 청구 기간을 추출하여 DefaultPeriod 생성
     */
    private DefaultPeriod createBillingPeriodFromJobParameters() {
        LocalDate billingStartDate = LocalDate.parse(billingStartDateStr);
        LocalDate billingEndDate = LocalDate.parse(billingEndDateStr);
        
        return DefaultPeriod.of(billingStartDate, billingEndDate);
    }
}