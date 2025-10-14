package me.realimpact.telecom.billing.batch.config;

import static me.realimpact.telecom.billing.batch.config.BatchConstants.CHUNK_SIZE;

import java.time.LocalDate;
import java.util.Arrays;
import java.util.List;

import org.apache.ibatis.session.SqlSessionFactory;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.configuration.annotation.StepScope;
import org.springframework.batch.core.job.builder.JobBuilder;
import org.springframework.batch.core.partition.PartitionHandler;
import org.springframework.batch.core.partition.support.Partitioner;
import org.springframework.batch.core.partition.support.TaskExecutorPartitionHandler;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.step.builder.StepBuilder;
import org.springframework.batch.item.ItemProcessor;
import org.springframework.batch.item.ItemStreamReader;
import org.springframework.batch.item.ItemWriter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.task.TaskExecutor;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.transaction.PlatformTransactionManager;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import me.realimpact.telecom.billing.batch.CalculationParameters;
import me.realimpact.telecom.billing.batch.partitioner.ContractPartitioner;
import me.realimpact.telecom.billing.batch.processor.CalculationProcessor;
import me.realimpact.telecom.billing.batch.reader.PartitionedContractReader;
import me.realimpact.telecom.billing.batch.tasklet.CalculationResultCleanupTasklet;
import me.realimpact.telecom.billing.batch.writer.CalculationWriter;
import me.realimpact.telecom.calculation.api.BillingCalculationPeriod;
import me.realimpact.telecom.calculation.api.BillingCalculationType;
import me.realimpact.telecom.calculation.api.CalculationResultGroup;
import me.realimpact.telecom.calculation.application.CalculationCommandService;
import me.realimpact.telecom.calculation.application.CalculationTarget;
import me.realimpact.telecom.calculation.application.CalculationTargetLoader;
import me.realimpact.telecom.calculation.port.out.CalculationResultSavePort;

/**
 * 파티셔닝을 사용하여 월별 요금 계산 배치를 설정하는 클래스.
 * `partitionedMonthlyFeeCalculationJob` 잡이 활성화될 때 이 설정이 사용된다.
 */
@Configuration
@RequiredArgsConstructor
@ConditionalOnProperty(name = "spring.batch.job.names", havingValue = "partitionedMonthlyFeeCalculationJob")
@Slf4j
public class PartitionedCalculationBatchConfig {

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
     * 파티셔닝 처리를 위한 TaskExecutor를 설정한다.
     * @param threadCount 스레드 개수
     * @return 설정된 TaskExecutor
     */
    @Bean("partitionedTaskExecutor")
    public TaskExecutor partitionedTaskExecutor(@Value("${batch.thread-count}") Integer threadCount) {
        log.info("=== PartitionedTaskExecutor Bean 생성 시작 === threadCount: {}", threadCount);

        int maxThreadCount = threadCount * 2;

        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(threadCount);
        executor.setMaxPoolSize(maxThreadCount);
        executor.setQueueCapacity(1000);
        executor.setThreadNamePrefix("partitioned-batch-");
        executor.setWaitForTasksToCompleteOnShutdown(true);
        executor.setAwaitTerminationSeconds(10);
        executor.initialize();

        log.info("=== PartitionedTaskExecutor Bean 생성 완료 ===");
        log.info("쓰레드 수: {}", threadCount);
        log.info("최대 쓰레드 수: {}", maxThreadCount);

        return executor;
    }

    /**
     * 계약 ID를 기준으로 파티션을 나누는 Partitioner를 생성한다.
     * @param threadCount 파티션 개수 (스레드 개수와 동일)
     * @return ContractPartitioner
     */
    @Bean("contractPartitioner")
    public Partitioner contractPartitioner(@Value("${batch.thread-count}") Integer threadCount) {
        log.info("=== ContractPartitioner Bean 생성 시작 === threadCount: {}", threadCount);

        ContractPartitioner partitioner = new ContractPartitioner(threadCount);

        log.info("=== ContractPartitioner Bean 생성 완료 === 파티션 수: {}", threadCount);
        return partitioner;
    }


    /**
     * 파티션별로 계약 데이터를 읽는 Reader를 생성한다.
     * @return PartitionedContractReader
     */
    @Bean("partitionedContractReader")
    @StepScope
    public ItemStreamReader<CalculationTarget> partitionedContractReader(
            @Value("${billingStartDate}") String billingStartDateStr,
            @Value("${billingEndDate}") String billingEndDateStr,
            @Value("${contractIds:}") String contractIdsStr,
            @Value("${batch.thread-count}") Integer threadCount,
            @Value("${billingCalculationType}") String billingCalculationTypeStr,
            @Value("${billingCalculationPeriod}") String billingCalculationPeriodStr,
            @Value("#{stepExecutionContext['partitionKey']}") Integer partitionKey,
            @Value("#{stepExecutionContext['partitionCount']}") Integer partitionCount
    ) {
        log.info("=== PartitionedContractReader Bean 생성 시작 === billingStartDate: {}, threadCount: {}, partitionKey: {}, partitionCount: {}",
                billingStartDateStr, threadCount, partitionKey, partitionCount);

        CalculationParameters params = createCalculationParameters(
                billingStartDateStr, billingEndDateStr, contractIdsStr,
                threadCount, billingCalculationTypeStr, billingCalculationPeriodStr
        );

        PartitionedContractReader reader = new PartitionedContractReader(
                calculationTargetLoader,
                sqlSessionFactory,
                params,
                partitionKey,
                partitionCount
        );

        log.info("=== PartitionedContractReader Bean 생성 완료 ===");
        return reader;
    }

    /**
     * 읽어온 데이터를 처리하는 Processor를 생성한다.
     * @return CalculationProcessor
     */
    @Bean("partitionedCalculationProcessor")
    @StepScope
    public ItemProcessor<CalculationTarget, CalculationResultGroup> partitionedCalculationProcessor(
            @Value("${billingStartDate}") String billingStartDateStr,
            @Value("${billingEndDate}") String billingEndDateStr,
            @Value("${contractIds:}") String contractIdsStr,
            @Value("${batch.thread-count}") Integer threadCount,
            @Value("${billingCalculationType}") String billingCalculationTypeStr,
            @Value("${billingCalculationPeriod}") String billingCalculationPeriodStr
    ) {
        log.info("=== PartitionedCalculationProcessor Bean 생성 시작 === billingStartDate: {}, threadCount: {}",
                billingStartDateStr, threadCount);

        CalculationParameters params = createCalculationParameters(
                billingStartDateStr, billingEndDateStr, contractIdsStr,
                threadCount, billingCalculationTypeStr, billingCalculationPeriodStr
        );

        CalculationProcessor processor = new CalculationProcessor(calculationCommandService, params);
        log.info("=== PartitionedCalculationProcessor Bean 생성 완료 ===");

        return processor;
    }

    /**
     * 처리된 데이터를 저장하는 Writer를 생성한다.
     * @return CalculationWriter
     */
    @Bean("partitionedCalculationWriter")
    @StepScope
    public ItemWriter<CalculationResultGroup> partitionedCalculationWriter(
            @Value("${billingStartDate}") String billingStartDateStr,
            @Value("${billingEndDate}") String billingEndDateStr,
            @Value("${contractIds:}") String contractIdsStr,
            @Value("${batch.thread-count}") Integer threadCount,
            @Value("${billingCalculationType}") String billingCalculationTypeStr,
            @Value("${billingCalculationPeriod}") String billingCalculationPeriodStr
    ) {
        log.info("=== PartitionedCalculationWriter Bean 생성 시작 === billingStartDate: {}, threadCount: {}",
                billingStartDateStr, threadCount);

        CalculationParameters params = createCalculationParameters(
                billingStartDateStr, billingEndDateStr, contractIdsStr,
                threadCount, billingCalculationTypeStr, billingCalculationPeriodStr
        );

        CalculationWriter writer = new CalculationWriter(calculationResultSavePort, params);
        log.info("=== PartitionedCalculationWriter Bean 생성 완료 ===");

        return writer;
    }

    /**
     * 각 파티션에서 실행될 Worker Step을 정의한다.
     * @return Worker Step
     */
    @Bean("partitionedWorkerStep")
    public Step partitionedWorkerStep() {
        return new StepBuilder("partitionedWorkerStep", jobRepository)
                .<CalculationTarget, CalculationResultGroup>chunk(CHUNK_SIZE, transactionManager)
                .reader(partitionedContractReader(null, null, null, null, null, null, null, null))
                .processor(partitionedCalculationProcessor(null, null, null, null, null, null))
                .writer(partitionedCalculationWriter(null, null, null, null, null, null))
                .build();
    }

    /**
     * 파티션을 관리하고 병렬 실행하는 PartitionHandler를 설정한다.
     * @param threadCount 스레드 개수
     * @return PartitionHandler
     */
    @Bean("partitionHandler")
    public PartitionHandler partitionHandler(@Value("${batch.thread-count:8}") Integer threadCount) {
        log.info("=== PartitionHandler Bean 생성 시작 === threadCount: {}", threadCount);

        TaskExecutorPartitionHandler partitionHandler = new TaskExecutorPartitionHandler();
        partitionHandler.setStep(partitionedWorkerStep());
        partitionHandler.setTaskExecutor(partitionedTaskExecutor(threadCount));
        partitionHandler.setGridSize(threadCount);

        log.info("=== PartitionHandler Bean 생성 완료 === Grid Size (파티션 수): {}", threadCount);

        return partitionHandler;
    }

    /**
     * Partitioner를 사용하여 파티션을 생성하고 Worker Step들을 병렬로 실행하는 Master Step을 정의한다.
     * @return Master Step
     */
    @Bean("partitionedMasterStep")
    public Step partitionedMasterStep() {
        return new StepBuilder("partitionedMasterStep", jobRepository)
                .partitioner("partitionedWorkerStep", contractPartitioner(null))
                .partitionHandler(partitionHandler(null))
                .build();
    }

    /**
     * 잡 실행 전, 이전 계산 결과를 삭제하는 Cleanup Step을 정의한다.
     * @param calculationResultCleanupTasklet
     * @return Cleanup Step
     */
    @Bean("partitionedCleanupCalculationResultStep")
    public Step partitionedCleanupCalculationResultStep(CalculationResultCleanupTasklet calculationResultCleanupTasklet) {
        return new StepBuilder("partitionedCleanupCalculationResultStep", jobRepository)
                .tasklet(calculationResultCleanupTasklet, transactionManager)
                .build();
    }

    /**
     * 파티셔닝을 사용하는 월별 요금 계산 잡을 정의한다.
     * @param calculationResultCleanupTasklet
     * @return partitionedMonthlyFeeCalculationJob
     */
    @Bean("partitionedMonthlyFeeCalculationJob")
    public Job partitionedMonthlyFeeCalculationJob(CalculationResultCleanupTasklet calculationResultCleanupTasklet) {
        return new JobBuilder("partitionedMonthlyFeeCalculationJob", jobRepository)
                .start(partitionedCleanupCalculationResultStep(calculationResultCleanupTasklet))
                .next(partitionedMasterStep())
                .build();
    }

}