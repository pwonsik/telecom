package me.realimpact.telecom.billing.batch.processor;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import me.realimpact.telecom.calculation.application.monthlyfee.BaseFeeCalculator;
import me.realimpact.telecom.calculation.domain.monthlyfee.Contract;
import me.realimpact.telecom.calculation.domain.monthlyfee.MonthlyFeeCalculationResult;
import me.realimpact.telecom.calculation.infrastructure.converter.DtoToDomainConverter;
import me.realimpact.telecom.calculation.infrastructure.dto.ContractDto;
import org.springframework.batch.core.configuration.annotation.StepScope;
import org.springframework.batch.item.ItemProcessor;

/**
 * Spring Batch ItemProcessor 구현체
 * ContractDto를 받아서 월정액 계산을 수행하고 결과를 반환
 */
@StepScope
@RequiredArgsConstructor
@Slf4j
public class MonthlyFeeCalculationProcessor implements ItemProcessor<ContractDto, MonthlyFeeCalculationResult> {

    private final BaseFeeCalculator baseFeeCalculator;
    private final DtoToDomainConverter dtoToDomainConverter;

    @Override
    public MonthlyFeeCalculationResult process(ContractDto contractDto) throws Exception {
        try {
            log.debug("Processing contract calculation for contractId: {}", contractDto.getContractId());
            
            // 1. DTO를 도메인 객체로 변환
            Contract contract = dtoToDomainConverter.convertToContract(contractDto);

            // 2. 월정액 계산 수행 (순수 계산 로직만)
            MonthlyFeeCalculationResult result = baseFeeCalculator.process(contract);
            
            log.debug("Calculated {} results for contractId: {}", result.items().size(), contractDto.getContractId());
            
            return result;
            
        } catch (Exception e) {
            log.error("Failed to process contract calculation for contractId: {}", contractDto.getContractId(), e);
            throw e;
        }
    }
}