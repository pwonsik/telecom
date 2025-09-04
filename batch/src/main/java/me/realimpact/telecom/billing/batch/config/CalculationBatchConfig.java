package me.realimpact.telecom.billing.batch.config;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import me.realimpact.telecom.billing.batch.CalculationParameters;
import me.realimpact.telecom.calculation.api.CalculationResultGroup;
import me.realimpact.telecom.billing.batch.processor.CalculationProcessor;
import me.realimpact.telecom.calculation.application.CalculationCommandService;
import me.realimpact.telecom.calculation.application.CalculationTarget;
import me.realimpact.telecom.billing.batch.reader.ChunkedContractReader;
import me.realimpact.telecom.billing.batch.tasklet.CalculationResultCleanupTasklet;
import me.realimpact.telecom.billing.batch.util.JsonLoggingHelper;
import me.realimpact.telecom.billing.batch.writer.CalculationWriter;
import me.realimpact.telecom.calculation.api.BillingCalculationPeriod;
import me.realimpact.telecom.calculation.api.BillingCalculationType;
import me.realimpact.telecom.calculation.application.discount.CalculationResultProrater;
import me.realimpact.telecom.calculation.application.monthlyfee.BaseFeeCalculator;
import me.realimpact.telecom.calculation.application.discount.DiscountCalculator;
import me.realimpact.telecom.calculation.application.onetimecharge.OneTimeChargeCalculator;
import me.realimpact.telecom.calculation.application.onetimecharge.OneTimeChargeDataLoader;
import me.realimpact.telecom.calculation.application.vat.VatCalculator;
import me.realimpact.telecom.calculation.domain.onetimecharge.OneTimeChargeDomain;
import me.realimpact.telecom.calculation.port.out.CalculationResultSavePort;
import org.apache.ibatis.session.SqlSessionFactory;
import org.mybatis.spring.annotation.MapperScan;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.configuration.annotation.JobScope;
import org.springframework.batch.core.configuration.annotation.StepScope;
import org.springframework.batch.core.job.builder.JobBuilder;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.step.builder.StepBuilder;
import org.springframework.batch.item.ItemProcessor;
import org.springframework.batch.item.ItemWriter;
import org.springframework.batch.item.support.SynchronizedItemStreamReader;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.task.TaskExecutor;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.transaction.PlatformTransactionManager;

import java.time.LocalDate;
import java.util.Arrays;
import java.util.List;

import static me.realimpact.telecom.billing.batch.config.BatchConstants.CHUNK_SIZE;

/**
 * Spring Batch 설정 예제
 * MyBatisPagingItemReader를 사용한 대용량 계약 데이터 처리
 */
@Configuration
@RequiredArgsConstructor
@MapperScan("me.realimpact.telecom.calculation.infrastructure.adapter")
@Slf4j
public class CalculationBatchConfig {

    private final SqlSessionFactory sqlSessionFactory;
    private final JobRepository jobRepository;
    private final PlatformTransactionManager transactionManager;

    private final CalculationCommandService calculationCommandService;

    private final JsonLoggingHelper jsonLoggingHelper;

    private final CalculationResultSavePort calculationResultSavePort;

    @Bean
    @StepScope
    public CalculationParameters calculationParameters(
        @Value("#{jobParameters['billingStartDate']}") String billingStartDateStr,
        @Value("#{jobParameters['billingEndDate']}") String billingEndDateStr,
        @Value("#{jobParameters['contractIds']}") String contractIdsStr,
        @Value("#{jobParameters['threadCount']}") String threadCountStr,
        @Value("#{jobParameters['billingCalculationType']}") String billingCalculationTypeStr,
        @Value("#{jobParameters['billingCalculationPeriod']}") String billingCalculationPeriodStr
    ) {
        LocalDate billingStartDate = LocalDate.parse(billingStartDateStr);
        LocalDate billingEndDate = LocalDate.parse(billingEndDateStr);

        List<Long> contractIds = contractIdsStr == null || contractIdsStr.trim().isEmpty()
            ? List.of()
            : Arrays.stream(contractIdsStr.split(","))
                .map(String::trim)
                .map(Long::parseLong)
                .toList();

        BillingCalculationType billingCalculationType = BillingCalculationType.fromCode(billingCalculationTypeStr);
        BillingCalculationPeriod billingCalculationPeriod = BillingCalculationPeriod.fromCode(billingCalculationPeriodStr);

        int threadCount = Integer.parseInt(threadCountStr);

        return new CalculationParameters(
            billingStartDate,
            billingEndDate,
            billingCalculationType,
            billingCalculationPeriod,
            threadCount,
            contractIds);
    }

    /**
     * 멀티쓰레드 처리를 위한 TaskExecutor 설정
     */
    @Bean
    @JobScope
    public TaskExecutor taskExecutor(CalculationParameters calculationParameters) {
        int threadCount = calculationParameters.getThreadCount();
        int maxThreadCount = threadCount * 2;

        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(threadCount);        // Job Parameter로 받은 쓰레드 수
        executor.setMaxPoolSize(maxThreadCount);     // 최대 쓰레드 수
        executor.setQueueCapacity(1000);               // 대기 큐 크기
        executor.setThreadNamePrefix("batch-");
        executor.setWaitForTasksToCompleteOnShutdown(true);
        executor.setAwaitTerminationSeconds(60);
        executor.initialize();
        
        log.info("=== TaskExecutor 설정 ===");
        log.info("쓰레드 수: {}", threadCount);
        log.info("최대 쓰레드 수: {}", maxThreadCount);
        
        return executor;
    }

    /**
     * ChunkedContractReader 설정 (Step Parameter 기반)
     * chunk size만큼 contract ID를 읽어서 bulk 조회로 ContractDto 생성
     * contractId가 있으면 단건, 없으면 전체 조회
     */
    @Bean
    @StepScope
    public ChunkedContractReader chunkedContractReader(CalculationParameters calculationParameters, CalculationCommandService calculationCommandService) {
        return new ChunkedContractReader(
                calculationCommandService,
                sqlSessionFactory,
                calculationParameters,
                jsonLoggingHelper
        );
    }
    
    /**
     * 멀티쓰레드 환경용 Thread-Safe Reader
     */
    @Bean
    @StepScope  
    public SynchronizedItemStreamReader<CalculationTarget> contractReader(CalculationParameters calculationParameters) {
        SynchronizedItemStreamReader<CalculationTarget> reader = new SynchronizedItemStreamReader<>();
        reader.setDelegate(chunkedContractReader(calculationParameters, calculationCommandService));  // ChunkedContractReader 사용
        return reader;
    }

    @Bean
    @StepScope
    public ItemProcessor<CalculationTarget, CalculationResultGroup> calculationProcessor(CalculationParameters calculationParameters) {
        return new CalculationProcessor(
                calculationCommandService,
                jsonLoggingHelper,
                calculationParameters
        );
    }

    /**
     * Writer Bean 설정 (@StepScope) - 커스텀 Writer 사용
     */
    @Bean
    @StepScope
    public ItemWriter<CalculationResultGroup> calculationWriter(CalculationParameters calculationParameters) {
        return new CalculationWriter(calculationResultSavePort, calculationParameters);
    }


    /**
     * Cleanup Step 설정 - 기존 계산 결과 삭제
     */
    @Bean
    public Step cleanupCalculationResultStep(CalculationResultCleanupTasklet calculationResultCleanupTasklet) {
        return new StepBuilder("cleanupCalculationResultStep", jobRepository)
                .tasklet(calculationResultCleanupTasklet, transactionManager)
                .build();
    }

    /**
     * Step 설정 - 멀티쓰레드 처리로 성능 최적화
     */
    @Bean
    public Step monthlyFeeCalculationStep(CalculationParameters calculationParameters) {
        return new StepBuilder("monthlyFeeCalculationStep", jobRepository)
                .<CalculationTarget, CalculationResultGroup>chunk(CHUNK_SIZE, transactionManager)  // 상수화된 chunk size 사용
                .reader(contractReader(calculationParameters))  // Thread-Safe Reader 사용
                .processor(calculationProcessor(calculationParameters))  // @StepScope Processor 사용
                .writer(calculationWriter(calculationParameters))        // @StepScope Writer 사용
                .taskExecutor(taskExecutor(calculationParameters))             // 멀티쓰레드 실행 (@JobScope가 런타임에 실제 값 주입)
                .build();
    }

    /**
     * Job 설정 - 삭제 Step 후 계산 Step 순서로 실행
     */
    @Bean
    public Job monthlyFeeCalculationJob(CalculationParameters calculationParameters, CalculationResultCleanupTasklet calculationResultCleanupTasklet) {
        return new JobBuilder("monthlyFeeCalculationJob", jobRepository)
                .start(cleanupCalculationResultStep(calculationResultCleanupTasklet))     // 1. 기존 결과 삭제
                .next(monthlyFeeCalculationStep(calculationParameters))         // 2. 새로운 계산 수행
                .build();
    }
}