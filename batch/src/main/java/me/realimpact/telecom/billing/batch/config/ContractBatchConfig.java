package me.realimpact.telecom.billing.batch.config;

import lombok.RequiredArgsConstructor;
import me.realimpact.telecom.billing.batch.processor.MonthlyFeeCalculationProcessor;
import me.realimpact.telecom.billing.batch.writer.MonthlyFeeCalculationResultWriter;
import me.realimpact.telecom.calculation.application.monthlyfee.BaseFeeCalculator;
import me.realimpact.telecom.calculation.infrastructure.adapter.CalculationResultMapper;
import me.realimpact.telecom.calculation.infrastructure.converter.DtoToDomainConverter;
import me.realimpact.telecom.calculation.infrastructure.converter.CalculationResultFlattener;
import me.realimpact.telecom.calculation.domain.monthlyfee.MonthlyFeeCalculationResult;
import me.realimpact.telecom.calculation.infrastructure.dto.ContractDto;
import org.apache.ibatis.session.SqlSessionFactory;
import org.mybatis.spring.batch.MyBatisBatchItemWriter;
import org.mybatis.spring.batch.MyBatisCursorItemReader;
import org.mybatis.spring.batch.builder.MyBatisCursorItemReaderBuilder;
import org.springframework.batch.item.support.SynchronizedItemStreamReader;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.configuration.annotation.JobScope;
import org.springframework.batch.core.configuration.annotation.StepScope;
import org.springframework.batch.core.job.builder.JobBuilder;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.step.builder.StepBuilder;
import org.springframework.batch.item.ItemWriter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.task.TaskExecutor;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
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
@RequiredArgsConstructor
public class ContractBatchConfig {

    private final SqlSessionFactory sqlSessionFactory;
    private final JobRepository jobRepository;
    private final PlatformTransactionManager transactionManager;
    private final BaseFeeCalculator monthlyFeeCalculatorService;
    private final DtoToDomainConverter dtoToDomainConverter;
    private final CalculationResultMapper calculationResultMapper;
    private final CalculationResultFlattener calculationResultFlattener;


    /**
     * 멀티쓰레드 처리를 위한 TaskExecutor 설정
     */
    @Bean
    @JobScope
    public TaskExecutor taskExecutor(@Value("#{jobParameters['threadCount'] ?: '8'}") String threadCountStr) {
        int threadCount = Integer.parseInt(threadCountStr);
        
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(threadCount);        // Job Parameter로 받은 쓰레드 수
        executor.setMaxPoolSize(threadCount * 2);     // 최대 쓰레드 수  
        executor.setQueueCapacity(1000);               // 대기 큐 크기
        executor.setThreadNamePrefix("batch-");
        executor.setWaitForTasksToCompleteOnShutdown(true);
        executor.setAwaitTerminationSeconds(60);
        executor.initialize();
        
        System.out.println("=== TaskExecutor 설정 ===");
        System.out.println("쓰레드 수: " + threadCount);
        System.out.println("최대 쓰레드 수: " + (threadCount * 2));
        
        return executor;
    }

    /**
     * MyBatisCursorItemReader 설정 (Step Parameter 기반)
     * 커서 기반 스트리밍으로 메모리 효율적 처리
     * contractId가 있으면 단건, 없으면 전체 조회
     */
    @Bean
    @StepScope
    public MyBatisCursorItemReader<ContractDto> contractCursorReader(
            @Value("#{jobParameters['billingStartDate']}") String billingStartDateStr,
            @Value("#{jobParameters['billingEndDate']}") String billingEndDateStr,
            @Value("#{jobParameters['contractId']}") String contractIdStr,
            @Value("#{jobParameters['parallelDegree'] ?: '4'}") String parallelDegreeStr) {
        
        System.out.println("=== ContractCursorReader 생성 (@StepScope) ===");
        System.out.println("billingStartDateStr: [" + billingStartDateStr + "]");
        System.out.println("billingEndDateStr: [" + billingEndDateStr + "]");
        System.out.println("contractIdStr: [" + contractIdStr + "]");
        System.out.println("parallelDegreeStr: [" + parallelDegreeStr + "]");
        
        // Job Parameter 유효성 검사
        if (billingStartDateStr == null || billingStartDateStr.trim().isEmpty() || 
            billingEndDateStr == null || billingEndDateStr.trim().isEmpty()) {
            throw new IllegalArgumentException("billingStartDate and billingEndDate are required job parameters");
        }
        
        // Job Parameter에서 받은 문자열을 LocalDate로 변환
        LocalDate billingStartDate = LocalDate.parse(billingStartDateStr);
        LocalDate billingEndDate = LocalDate.parse(billingEndDateStr);
        
        // contractId 처리 (null 또는 빈 문자열이면 전체 조회)
        Long contractId = (contractIdStr == null || contractIdStr.trim().isEmpty()) 
            ? null 
            : Long.parseLong(contractIdStr.trim());
            
        // parallelDegree 처리
        Integer parallelDegree = Integer.parseInt(parallelDegreeStr);

        // 파라미터 설정
        Map<String, Object> parameterValues = new HashMap<>();
        parameterValues.put("contractId", contractId);  // null이면 전체 조회, 값이 있으면 해당 계약만 조회
        parameterValues.put("billingStartDate", billingStartDate);
        parameterValues.put("billingEndDate", billingEndDate);
        parameterValues.put("parallelDegree", parallelDegree);  // MySQL 힌트용

        System.out.println("MyBatis 파라미터: " + parameterValues);

        return new MyBatisCursorItemReaderBuilder<ContractDto>()
                .sqlSessionFactory(sqlSessionFactory)
                .queryId("me.realimpact.telecom.calculation.infrastructure.adapter.ContractQueryMapper.findContractsWithProductsChargeItemsAndSuspensions")
                .parameterValues(parameterValues)
                .build();
    }
    
    /**
     * 멀티쓰레드 환경용 Thread-Safe Reader
     */
    @Bean
    @StepScope  
    public SynchronizedItemStreamReader<ContractDto> contractReader() {
        SynchronizedItemStreamReader<ContractDto> reader = new SynchronizedItemStreamReader<>();
        reader.setDelegate(contractCursorReader(null, null, null, null));  // @StepScope가 런타임에 실제 값 주입
        return reader;
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
     * Writer Bean 설정 (@StepScope) - 커스텀 Writer 사용
     */
    @Bean
    @StepScope
    public ItemWriter<MonthlyFeeCalculationResult> monthlyFeeCalculationWriter() {
        return new MonthlyFeeCalculationResultWriter(calculationResultMapper, calculationResultFlattener);
    }


    /**
     * Step 설정 - 멀티쓰레드 처리로 성능 최적화
     */
    @Bean
    public Step monthlyFeeCalculationStep() {
        return new StepBuilder("monthlyFeeCalculationStep", jobRepository)
                .<ContractDto, MonthlyFeeCalculationResult>chunk(100, transactionManager)  // 쓰레드당 100개씩 커밋
                .reader(contractReader())  // Thread-Safe Reader 사용
                .processor(monthlyFeeCalculationProcessor())  // @StepScope Processor 사용
                .writer(monthlyFeeCalculationWriter())        // @StepScope Writer 사용
                .taskExecutor(taskExecutor(null))             // 멀티쓰레드 실행 (@JobScope가 런타임에 실제 값 주입)
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