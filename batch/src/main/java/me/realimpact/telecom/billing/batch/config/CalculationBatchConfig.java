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
import org.springframework.batch.item.support.SynchronizedItemReader;
import org.springframework.batch.item.support.SynchronizedItemStreamReader;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.*;
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
//@MapperScan("me.realimpact.telecom.calculation.infrastructure.adapter")
@ConditionalOnProperty(name = "spring.batch.job.names", havingValue = "monthlyFeeCalculationJob")
@Slf4j
public class CalculationBatchConfig {

    private final SqlSessionFactory sqlSessionFactory;
    private final JobRepository jobRepository;
    private final PlatformTransactionManager transactionManager;

    private final CalculationCommandService calculationCommandService;

    private final CalculationResultSavePort calculationResultSavePort;

    /**
     * Helper method to create CalculationParameters from individual parameters
     */
    private CalculationParameters createCalculationParameters(
            String billingStartDateStr,
            String billingEndDateStr,
            String contractIdsStr,
            Integer threadCount,
            String billingCalculationTypeStr,
            String billingCalculationPeriodStr
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

        return new CalculationParameters(
            billingStartDate,
            billingEndDate,
            billingCalculationType,
            billingCalculationPeriod,
            threadCount,
            contractIds
        );
    }

    /**
     * 멀티쓰레드 처리를 위한 TaskExecutor 설정
     */
    @Bean
    public TaskExecutor taskExecutor(
            @Value("${batch.thread-count:8}") Integer threadCount
    ) {
        log.info("=== TaskExecutor Bean 생성 시작 ===  threadCount: {}", threadCount);
        int maxThreadCount = threadCount * 2;

        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(threadCount);        // Application Property로 받은 쓰레드 수
        executor.setMaxPoolSize(maxThreadCount);     // 최대 쓰레드 수
        executor.setQueueCapacity(1000);               // 대기 큐 크기
        executor.setThreadNamePrefix("batch-");
        executor.setWaitForTasksToCompleteOnShutdown(true);
        executor.setAwaitTerminationSeconds(10);
        executor.initialize();

        log.info("=== TaskExecutor Bean 생성 완료 ===");
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
    public ChunkedContractReader chunkedContractReader(
            @Value("${billingStartDate}") String billingStartDateStr,
            @Value("${billingEndDate}") String billingEndDateStr,
            @Value("${contractIds:}") String contractIdsStr,
            @Value("${batch.thread-count}") Integer threadCount,
            @Value("${billingCalculationType}") String billingCalculationTypeStr,
            @Value("${billingCalculationPeriod}") String billingCalculationPeriodStr,
            CalculationCommandService calculationCommandService
    ) {
        log.info("=== ChunkedContractReader Bean 생성 시작 === billingStartDate: {}, billingEndDate: {}, threadCount: {}",
                billingStartDateStr, billingEndDateStr, threadCount);

        CalculationParameters params = createCalculationParameters(
                billingStartDateStr, billingEndDateStr, contractIdsStr,
                threadCount, billingCalculationTypeStr, billingCalculationPeriodStr
        );

        ChunkedContractReader reader = new ChunkedContractReader(
                calculationCommandService,
                sqlSessionFactory,
                params
        );

        log.info("=== ChunkedContractReader Bean 생성 완료 ===");
        return reader;
    }
    
    /**
     * 멀티쓰레드 환경용 Thread-Safe Reader
     */
    @Bean
    public SynchronizedItemStreamReader<CalculationTarget> contractReader(
            @Value("${billingStartDate}") String billingStartDateStr,
            @Value("${billingEndDate}") String billingEndDateStr,
            @Value("${contractIds:}") String contractIdsStr,
            @Value("${batch.thread-count}") Integer threadCount,
            @Value("${billingCalculationType}") String billingCalculationTypeStr,
            @Value("${billingCalculationPeriod}") String billingCalculationPeriodStr,
            CalculationCommandService calculationCommandService
    ) {
        log.info("=== SynchronizedItemStreamReader Bean 생성 시작 === billingStartDate: {}, threadCount: {}",
                billingStartDateStr, threadCount);

        SynchronizedItemStreamReader<CalculationTarget> reader = new SynchronizedItemStreamReader<>();

        reader.setDelegate(
                chunkedContractReader(
                        billingStartDateStr, billingEndDateStr, contractIdsStr,
                        threadCount, billingCalculationTypeStr, billingCalculationPeriodStr,
                        calculationCommandService
                )
        );

        log.info("=== SynchronizedItemStreamReader Bean 생성 완료 ===");
        return reader;
    }

    @Bean
    public ItemProcessor<CalculationTarget, CalculationResultGroup> calculationProcessor(
            @Value("${billingStartDate}") String billingStartDateStr,
            @Value("${billingEndDate}") String billingEndDateStr,
            @Value("${contractIds:}") String contractIdsStr,
            @Value("${batch.thread-count}") Integer threadCount,
            @Value("${billingCalculationType}") String billingCalculationTypeStr,
            @Value("${billingCalculationPeriod}") String billingCalculationPeriodStr,
            CalculationCommandService calculationCommandService
    ) {
        log.info("=== CalculationProcessor Bean 생성 시작 === billingStartDate: {}, threadCount: {}",
                billingStartDateStr, threadCount);

        CalculationParameters params = createCalculationParameters(
                billingStartDateStr, billingEndDateStr, contractIdsStr,
                threadCount, billingCalculationTypeStr, billingCalculationPeriodStr
        );

        CalculationProcessor processor = new CalculationProcessor(calculationCommandService, params);
        log.info("=== CalculationProcessor Bean 생성 완료 ===");

        return processor;
    }

    /**
     * Writer Bean 설정 (@StepScope) - 커스텀 Writer 사용
     */
    @Bean
    public ItemWriter<CalculationResultGroup> calculationWriter(
            @Value("${billingStartDate}") String billingStartDateStr,
            @Value("${billingEndDate}") String billingEndDateStr,
            @Value("${contractIds:}") String contractIdsStr,
            @Value("${batch.thread-count}") Integer threadCount,
            @Value("${billingCalculationType}") String billingCalculationTypeStr,
            @Value("${billingCalculationPeriod}") String billingCalculationPeriodStr
    ) {
        log.info("=== CalculationWriter Bean 생성 시작 === billingStartDate: {}, threadCount: {}",
                billingStartDateStr, threadCount);

        CalculationParameters params = createCalculationParameters(
                billingStartDateStr, billingEndDateStr, contractIdsStr,
                threadCount, billingCalculationTypeStr, billingCalculationPeriodStr
        );

        CalculationWriter writer = new CalculationWriter(calculationResultSavePort, params);
        log.info("=== CalculationWriter Bean 생성 완료 ===");

        return writer;
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
    public Step monthlyFeeCalculationStep() {
        return new StepBuilder("monthlyFeeCalculationStep", jobRepository)
                .<CalculationTarget, CalculationResultGroup>chunk(CHUNK_SIZE, transactionManager)  // 상수화된 chunk size 사용
                .reader(contractReader(null, null, null, null, null, null, null))  // Thread-Safe Reader 사용
                .processor(calculationProcessor(null, null, null, null, null, null, null))  // @JobScope Processor 사용
                .writer(calculationWriter(null, null, null, null, null, null))        // @JobScope Writer 사용
                .taskExecutor(taskExecutor(null))             // 멀티쓰레드 실행 (@JobScope가 런타임에 실제 값 주입)
                .build();
    }

    /**
     * Job 설정 - 삭제 Step 후 계산 Step 순서로 실행
     */
    @Bean
    public Job monthlyFeeCalculationJob(CalculationResultCleanupTasklet calculationResultCleanupTasklet) {
        return new JobBuilder("monthlyFeeCalculationJob", jobRepository)
                .start(cleanupCalculationResultStep(calculationResultCleanupTasklet))     // 1. 기존 결과 삭제
                .next(monthlyFeeCalculationStep())         // 2. 새로운 계산 수행
                .build();
    }
}