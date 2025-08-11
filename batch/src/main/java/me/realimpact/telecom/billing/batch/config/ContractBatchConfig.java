package me.realimpact.telecom.billing.batch.config;

import me.realimpact.telecom.billing.batch.processor.MonthlyFeeCalculationProcessor;
import me.realimpact.telecom.billing.batch.writer.MonthlyFeeCalculationWriter;
import me.realimpact.telecom.calculation.application.monthlyfee.MonthlyFeeCalculatorService;
import me.realimpact.telecom.calculation.infrastructure.converter.DtoToDomainConverter;
import me.realimpact.telecom.calculation.domain.monthlyfee.MonthlyFeeCalculationResult;
import me.realimpact.telecom.calculation.infrastructure.dto.ContractDto;
import org.apache.ibatis.session.SqlSessionFactory;
import org.mybatis.spring.batch.MyBatisPagingItemReader;
import org.mybatis.spring.batch.builder.MyBatisPagingItemReaderBuilder;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.configuration.annotation.JobScope;
import org.springframework.batch.core.configuration.annotation.StepScope;
import org.springframework.batch.core.job.builder.JobBuilder;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.step.builder.StepBuilder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.transaction.PlatformTransactionManager;

import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Spring Batch 설정 예제
 * MyBatisPagingItemReader를 사용한 대용량 계약 데이터 처리
 */
@Configuration
public class ContractBatchConfig {

    @Autowired
    private SqlSessionFactory sqlSessionFactory;

    @Autowired
    private JobRepository jobRepository;

    @Autowired
    private PlatformTransactionManager transactionManager;
    
    @Autowired
    private MonthlyFeeCalculatorService monthlyFeeCalculatorService;
    
    @Autowired
    private DtoToDomainConverter dtoToDomainConverter;
    

    /**
     * MyBatisPagingItemReader 설정 (Job Parameter 기반)
     * contractId가 있으면 단건, 없으면 전체 조회
     */
    @Bean
    @StepScope
    public MyBatisPagingItemReader<ContractDto> contractReader(
            @Value("#{jobParameters['billingStartDate']}") String billingStartDateStr,
            @Value("#{jobParameters['billingEndDate']}") String billingEndDateStr,
            @Value("#{jobParameters['contractId']}") String contractIdStr) {
        
        // Job Parameter에서 받은 문자열을 LocalDate로 변환
        LocalDate billingStartDate = LocalDate.parse(billingStartDateStr);
        LocalDate billingEndDate = LocalDate.parse(billingEndDateStr);
        
        // contractId 파라미터 처리 (null 또는 빈 문자열이면 전체 조회)
        Long contractId = (contractIdStr == null || contractIdStr.trim().isEmpty()) 
            ? null 
            : Long.parseLong(contractIdStr);
        
        // 파라미터 설정
        Map<String, Object> parameterValues = new HashMap<>();
        parameterValues.put("contractId", contractId);  // null이면 전체 조회, 값이 있으면 해당 계약만 조회
        parameterValues.put("billingStartDate", billingStartDate);
        parameterValues.put("billingEndDate", billingEndDate);

        return new MyBatisPagingItemReaderBuilder<ContractDto>()
                .sqlSessionFactory(sqlSessionFactory)
                .queryId("me.realimpact.telecom.calculation.infrastructure.adapter.ContractQueryMapper.findContractsWithProductsChargeItemsAndSuspensions")
                .parameterValues(parameterValues)
                .pageSize(100)  // 한 번에 1000개씩 읽기
                .build();
    }

    /**
     * Processor Bean 설정 (@StepScope)
     */
    @Bean
    @StepScope
    public MonthlyFeeCalculationProcessor monthlyFeeCalculationProcessor() {
        return new MonthlyFeeCalculationProcessor(monthlyFeeCalculatorService, dtoToDomainConverter);
    }
    
    /**
     * Writer Bean 설정 (@StepScope)
     */
    @Bean
    @StepScope
    public MonthlyFeeCalculationWriter monthlyFeeCalculationWriter() {
        return new MonthlyFeeCalculationWriter(monthlyFeeCalculatorService);
    }

    /**
     * Step 설정 - 리팩토링된 Reader-Processor-Writer 사용
     */
    @Bean
    public Step monthlyFeeCalculationStep() {
        return new StepBuilder("monthlyFeeCalculationStep", jobRepository)
                .<ContractDto, List<MonthlyFeeCalculationResult>>chunk(100, transactionManager)  // 100개씩 커밋
                .reader(contractReader(null, null, null))
                .processor(monthlyFeeCalculationProcessor())  // @StepScope Processor 사용
                .writer(monthlyFeeCalculationWriter())        // @StepScope Writer 사용
                .build();
    }

    /**
     * Job 설정
     */
    @Bean
    public Job monthlyFeeCalculationJob() {
        return new JobBuilder("monthlyFeeCalculationJob", jobRepository)
                .start(monthlyFeeCalculationStep())
                .build();
    }
}