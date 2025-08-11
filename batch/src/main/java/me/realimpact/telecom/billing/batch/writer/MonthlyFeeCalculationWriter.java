package me.realimpact.telecom.billing.batch.writer;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import me.realimpact.telecom.calculation.application.monthlyfee.MonthlyFeeCalculatorService;
import me.realimpact.telecom.calculation.domain.monthlyfee.DefaultPeriod;
import me.realimpact.telecom.calculation.domain.monthlyfee.MonthlyFeeCalculationResult;
import org.springframework.batch.core.configuration.annotation.StepScope;
import org.springframework.batch.item.Chunk;
import org.springframework.batch.item.ItemWriter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.util.List;

/**
 * Spring Batch ItemWriter 구현체
 * 월정액 계산 결과를 데이터베이스에 저장
 */
@StepScope
@RequiredArgsConstructor
@Slf4j
public class MonthlyFeeCalculationWriter implements ItemWriter<List<MonthlyFeeCalculationResult>> {

    private final MonthlyFeeCalculatorService monthlyFeeCalculatorService;
    
    @Value("#{jobParameters['billingStartDate']}")
    private String billingStartDateStr;
    
    @Value("#{jobParameters['billingEndDate']}")
    private String billingEndDateStr;

    @Override
    public void write(Chunk<? extends List<MonthlyFeeCalculationResult>> chunk) throws Exception {
        try {
            // 모든 계산 결과를 하나의 리스트로 플래튼
            List<MonthlyFeeCalculationResult> allResults = chunk.getItems().stream()
                    .flatMap(List::stream)
                    .toList();
            
            if (allResults.isEmpty()) {
                log.debug("No calculation results to write");
                return;
            }
            
            log.info("Writing {} calculation results to database", allResults.size());
            
            // 청구 기간 생성 (Job Parameters에서 가져옴)
            DefaultPeriod billingPeriod = createBillingPeriodFromJobParameters();
            
            // 배치로 저장
            monthlyFeeCalculatorService.writeResults(allResults, billingPeriod);
            
            log.info("Successfully wrote {} calculation results", allResults.size());
            
        } catch (Exception e) {
            log.error("Failed to write calculation results", e);
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