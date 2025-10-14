package me.realimpact.telecom.billing.batch.config;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import me.realimpact.telecom.billing.batch.CalculationParameters;
import me.realimpact.telecom.calculation.api.CalculationResultGroup;
import me.realimpact.telecom.billing.batch.processor.CalculationProcessor;
import me.realimpact.telecom.calculation.application.CalculationCommandService;
import me.realimpact.telecom.calculation.application.CalculationTarget;
import me.realimpact.telecom.calculation.application.CalculationTargetLoader;
import me.realimpact.telecom.billing.batch.reader.ChunkedContractReader;
import me.realimpact.telecom.billing.batch.tasklet.CalculationResultCleanupTasklet;
import me.realimpact.telecom.billing.batch.writer.CalculationWriter;
import me.realimpact.telecom.calculation.api.BillingCalculationPeriod;
import me.realimpact.telecom.calculation.api.BillingCalculationType;
import me.realimpact.telecom.calculation.port.out.CalculationResultSavePort;
import org.apache.ibatis.session.SqlSessionFactory;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.job.builder.JobBuilder;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.step.builder.StepBuilder;
import org.springframework.batch.item.ItemProcessor;
import org.springframework.batch.item.ItemWriter;
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
 * 월별 요금 계산 배치를 설정하는 클래스.
 * `monthlyFeeCalculationJob` 잡이 활성화될 때 이 설정이 사용된다.
 */
@Configuration
@RequiredArgsConstructor
@ConditionalOnProperty(name = "spring.batch.job.names", havingValue = "monthlyFeeCalculationJob")
@Slf4j
public class CalculationBatchConfig {

    private final SqlSessionFactory sqlSessionFactory;
    private final JobRepository jobRepository;
    private final PlatformTransactionManager transactionManager;

    private final CalculationCommandService calculationCommandService;
    private final CalculationTargetLoader calculationTargetLoader;
    private final CalculationResultSavePort calculationResultSavePort;

    /**
     * 잡 파라미터를 파싱하여 CalculationParameters 객체를 생성한다.
     * @return CalculationParameters 객체
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
     * 멀티스레드 처리를 위한 TaskExecutor를 설정한다.
     * @param threadCount 스레드 개수
     * @return 설정된 TaskExecutor
     */
    @Bean
    public TaskExecutor taskExecutor(
            @Value("${batch.thread-count:8}") Integer threadCount
    ) {
        log.info("=== TaskExecutor Bean 생성 시작 ===  threadCount: {}", threadCount);
        int maxThreadCount = threadCount * 2;

        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(threadCount);
        executor.setMaxPoolSize(maxThreadCount);
        executor.setQueueCapacity(1000);
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
     * 청크 기반으로 계약 데이터를 읽는 Reader를 생성한다.
     * @return ChunkedContractReader
     */
    @Bean
    public ChunkedContractReader chunkedContractReader(
            @Value("${billingStartDate}") String billingStartDateStr,
            @Value("${billingEndDate}") String billingEndDateStr,
            @Value("${contractIds:}") String contractIdsStr,
            @Value("${batch.thread-count}") Integer threadCount,
            @Value("${billingCalculationType}") String billingCalculationTypeStr,
            @Value("${billingCalculationPeriod}") String billingCalculationPeriodStr
    ) {
        log.info("=== ChunkedContractReader Bean 생성 시작 === billingStartDate: {}, billingEndDate: {}, threadCount: {}",
                billingStartDateStr, billingEndDateStr, threadCount);

        CalculationParameters params = createCalculationParameters(
                billingStartDateStr, billingEndDateStr, contractIdsStr,
                threadCount, billingCalculationTypeStr, billingCalculationPeriodStr
        );

        ChunkedContractReader reader = new ChunkedContractReader(
                calculationTargetLoader,
                sqlSessionFactory,
                params
        );

        log.info("=== ChunkedContractReader Bean 생성 완료 ===");
        return reader;
    }
    
    /**
     * 멀티스레드 환경에서 안전하게 아이템을 읽도록 동기화된 Reader를 생성한다.
     * @return 동기화된 ItemStreamReader
     */
    @Bean
    public SynchronizedItemStreamReader<CalculationTarget> contractReader(
            @Value("${billingStartDate}") String billingStartDateStr,
            @Value("${billingEndDate}") String billingEndDateStr,
            @Value("${contractIds:}") String contractIdsStr,
            @Value("${batch.thread-count}") Integer threadCount,
            @Value("${billingCalculationType}") String billingCalculationTypeStr,
            @Value("${billingCalculationPeriod}") String billingCalculationPeriodStr
    ) {
        log.info("=== SynchronizedItemStreamReader Bean 생성 시작 === billingStartDate: {}, threadCount: {}",
                billingStartDateStr, threadCount);

        SynchronizedItemStreamReader<CalculationTarget> reader = new SynchronizedItemStreamReader<>();

        reader.setDelegate(
                chunkedContractReader(
                        billingStartDateStr, billingEndDateStr, contractIdsStr,
                        threadCount, billingCalculationTypeStr, billingCalculationPeriodStr
                )
        );

        log.info("=== SynchronizedItemStreamReader Bean 생성 완료 ===");
        return reader;
    }

    /**
     * 읽어온 데이터를 처리하는 Processor를 생성한다.
     * @return CalculationProcessor
     */
    @Bean
    public ItemProcessor<CalculationTarget, CalculationResultGroup> calculationProcessor(
            @Value("${billingStartDate}") String billingStartDateStr,
            @Value("${billingEndDate}") String billingEndDateStr,
            @Value("${contractIds:}") String contractIdsStr,
            @Value("${batch.thread-count}") Integer threadCount,
            @Value("${billingCalculationType}") String billingCalculationTypeStr,
            @Value("${billingCalculationPeriod}") String billingCalculationPeriodStr
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
     * 처리된 데이터를 저장하는 Writer를 생성한다.
     * @return CalculationWriter
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
     * 잡 실행 전, 이전 계산 결과를 삭제하는 Cleanup Step을 정의한다.
     * @param calculationResultCleanupTasklet
     * @return Cleanup Step
     */
    @Bean
    public Step cleanupCalculationResultStep(CalculationResultCleanupTasklet calculationResultCleanupTasklet) {
        return new StepBuilder("cleanupCalculationResultStep", jobRepository)
                .tasklet(calculationResultCleanupTasklet, transactionManager)
                .build();
    }

    /**
     * 월별 요금 계산을 수행하는 메인 Step을 정의한다.
     * 멀티스레드로 동작하며, 청크 단위로 데이터를 처리한다.
     * @return 계산 Step
     */
    @Bean
    public Step monthlyFeeCalculationStep() {
        return new StepBuilder("monthlyFeeCalculationStep", jobRepository)
                .<CalculationTarget, CalculationResultGroup>chunk(CHUNK_SIZE, transactionManager)
                .reader(contractReader(null, null, null, null, null, null))
                .processor(calculationProcessor(null, null, null, null, null, null))
                .writer(calculationWriter(null, null, null, null, null, null))
                .taskExecutor(taskExecutor(null))
                .build();
    }

    /**
     * 월별 요금 계산 잡을 정의한다.
     * @param calculationResultCleanupTasklet
     * @return monthlyFeeCalculationJob
     */
    @Bean
    public Job monthlyFeeCalculationJob(CalculationResultCleanupTasklet calculationResultCleanupTasklet) {
        return new JobBuilder("monthlyFeeCalculationJob", jobRepository)
                .start(cleanupCalculationResultStep(calculationResultCleanupTasklet))
                .next(monthlyFeeCalculationStep())
                .build();
    }

}