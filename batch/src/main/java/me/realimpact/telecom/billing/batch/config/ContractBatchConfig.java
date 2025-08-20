package me.realimpact.telecom.billing.batch.config;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import me.realimpact.telecom.billing.batch.processor.MonthlyFeeCalculationProcessor;
import me.realimpact.telecom.billing.batch.reader.ChunkedContractReader;
import me.realimpact.telecom.billing.batch.writer.MonthlyFeeCalculationResultWriter;
import me.realimpact.telecom.calculation.application.monthlyfee.BaseFeeCalculator;
import me.realimpact.telecom.calculation.infrastructure.adapter.CalculationResultMapper;
import me.realimpact.telecom.calculation.infrastructure.adapter.ContractQueryMapper;
import me.realimpact.telecom.calculation.infrastructure.converter.DtoToDomainConverter;
import me.realimpact.telecom.calculation.infrastructure.converter.CalculationResultFlattener;
import me.realimpact.telecom.calculation.domain.monthlyfee.MonthlyFeeCalculationResult;
import me.realimpact.telecom.calculation.infrastructure.dto.ContractDto;
import org.apache.ibatis.session.SqlSessionFactory;
import org.mybatis.spring.annotation.MapperScan;
import org.springframework.batch.item.support.SynchronizedItemStreamReader;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.configuration.annotation.JobScope;
import org.springframework.batch.core.configuration.annotation.StepScope;
import org.springframework.batch.core.job.builder.JobBuilder;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.step.builder.StepBuilder;
import org.springframework.batch.item.ItemWriter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.task.TaskExecutor;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.transaction.PlatformTransactionManager;

import java.time.LocalDate;

/**
 * Spring Batch 설정 예제
 * MyBatisPagingItemReader를 사용한 대용량 계약 데이터 처리
 */
@Configuration
@RequiredArgsConstructor
@MapperScan("me.realimpact.telecom.calculation.infrastructure.adapter")
@Slf4j
public class ContractBatchConfig {

    private final SqlSessionFactory sqlSessionFactory;
    private final JobRepository jobRepository;
    private final PlatformTransactionManager transactionManager;
    private final BaseFeeCalculator monthlyFeeCalculatorService;
    private final DtoToDomainConverter dtoToDomainConverter;
    private final CalculationResultMapper calculationResultMapper;
    private final CalculationResultFlattener calculationResultFlattener;
    private final ContractQueryMapper contractQueryMapper;


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
        
        log.info("=== TaskExecutor 설정 ===");
        log.info("쓰레드 수: {}", threadCount);
        log.info("최대 쓰레드 수: {}", threadCount * 2);
        
        return executor;
    }

    /**
     * ChunkedContractReader 설정 (Step Parameter 기반)
     * chunk size만큼 contract ID를 읽어서 bulk 조회로 ContractDto 생성
     * contractId가 있으면 단건, 없으면 전체 조회
     */
    @Bean
    @StepScope
    public ChunkedContractReader chunkedContractReader() {
        // @StepScope로 인해 런타임에 job parameter와 dependency가 자동 주입됨
        return new ChunkedContractReader(contractQueryMapper, sqlSessionFactory); // ContractQueryMapper는 런타임에 주입
    }
    
    /**
     * 멀티쓰레드 환경용 Thread-Safe Reader
     */
    @Bean
    @StepScope  
    public SynchronizedItemStreamReader<ContractDto> contractReader() {
        SynchronizedItemStreamReader<ContractDto> reader = new SynchronizedItemStreamReader<>();
        reader.setDelegate(chunkedContractReader());  // ChunkedContractReader 사용
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