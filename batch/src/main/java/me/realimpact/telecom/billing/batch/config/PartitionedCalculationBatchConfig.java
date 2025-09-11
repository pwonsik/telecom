package me.realimpact.telecom.billing.batch.config;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import me.realimpact.telecom.billing.batch.CalculationParameters;
import me.realimpact.telecom.billing.batch.CalculationResultGroup;
import me.realimpact.telecom.billing.batch.partitioner.ContractPartitioner;
import me.realimpact.telecom.billing.batch.processor.CalculationProcessor;
import me.realimpact.telecom.billing.batch.reader.CalculationTarget;
import me.realimpact.telecom.billing.batch.reader.PartitionedContractReader;
import me.realimpact.telecom.billing.batch.tasklet.CalculationResultCleanupTasklet;
import me.realimpact.telecom.billing.batch.writer.CalculationWriter;
import me.realimpact.telecom.calculation.api.BillingCalculationPeriod;
import me.realimpact.telecom.calculation.api.BillingCalculationType;
import me.realimpact.telecom.calculation.application.discount.CalculationResultProrater;
import me.realimpact.telecom.calculation.application.monthlyfee.BaseFeeCalculator;
import me.realimpact.telecom.calculation.application.discount.DiscountCalculator;
import me.realimpact.telecom.calculation.application.onetimecharge.policy.DeviceInstallmentCalculator;
import me.realimpact.telecom.calculation.application.onetimecharge.policy.InstallationFeeCalculator;
import me.realimpact.telecom.calculation.application.vat.VatCalculator;
import me.realimpact.telecom.calculation.port.out.CalculationResultSavePort;
import org.apache.ibatis.session.SqlSessionFactory;
import org.mybatis.spring.annotation.MapperScan;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.configuration.annotation.JobScope;
import org.springframework.batch.core.configuration.annotation.StepScope;
import org.springframework.batch.core.job.builder.JobBuilder;
import org.springframework.batch.core.partition.PartitionHandler;
import org.springframework.batch.core.partition.support.Partitioner;
import org.springframework.batch.core.partition.support.TaskExecutorPartitionHandler;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.step.builder.StepBuilder;
import org.springframework.batch.item.ItemProcessor;
import org.springframework.batch.item.ItemReader;
import org.springframework.batch.item.ItemWriter;
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
 * Partitioner 기반 Spring Batch 설정
 * 계약 ID를 파티션별로 분할하여 병렬 처리
 */
@Configuration
@RequiredArgsConstructor
@MapperScan("me.realimpact.telecom.calculation.infrastructure.adapter")
@Slf4j
public class PartitionedCalculationBatchConfig {

    private final SqlSessionFactory sqlSessionFactory;
    private final JobRepository jobRepository;
    private final PlatformTransactionManager transactionManager;

    private final BaseFeeCalculator baseFeeCalculator;
    private final DeviceInstallmentCalculator deviceInstallmentCalculator;
    private final InstallationFeeCalculator installationFeeCalculator;
    private final CalculationResultProrater calculationResultProrater;

    private final VatCalculator vatCalculator;
    private final DiscountCalculator discountCalculator;

    private final CalculationResultSavePort calculationResultSavePort;

    @Bean("partitionedCalculationParameters")
    @StepScope
    public CalculationParameters partitionedCalculationParameters(
        @Value("#{jobParameters['billingStartDate']}") String billingStartDateStr,
        @Value("#{jobParameters['billingEndDate']}") String billingEndDateStr,
        @Value("#{jobParameters['contractId']}") String contractIdsStr,
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
     * 파티션 기반 처리를 위한 TaskExecutor 설정
     * 기본 8개 스레드로 설정, Job Parameter로 동적 변경 불가
     */
    @Bean("partitionedTaskExecutor")
    public TaskExecutor partitionedTaskExecutor() {
        int threadCount = 8; // 기본값으로 고정
        int maxThreadCount = threadCount * 2;

        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(threadCount);        
        executor.setMaxPoolSize(maxThreadCount);      
        executor.setQueueCapacity(1000);              
        executor.setThreadNamePrefix("partitioned-batch-");
        executor.setWaitForTasksToCompleteOnShutdown(true);
        executor.setAwaitTerminationSeconds(60);
        executor.initialize();
        
        log.info("=== 파티션 TaskExecutor 설정 ===");
        log.info("쓰레드 수: {}", threadCount);
        log.info("최대 쓰레드 수: {}", maxThreadCount);
        
        return executor;
    }

    /**
     * Contract Partitioner Bean - Step 실행 시 동적으로 thread count 결정
     */
    @Bean("contractPartitioner")
    @StepScope
    public Partitioner contractPartitioner(@Value("#{jobParameters['threadCount']}") String threadCountStr) {
        int threadCount = threadCountStr != null ? Integer.parseInt(threadCountStr) : 8;
        log.info("파티션 수 설정: {}", threadCount);
        return new ContractPartitioner(threadCount);
    }

    /**
     * 파티션별 Contract Reader
     */
    @Bean("partitionedContractReader")
    @StepScope
    public ItemReader<CalculationTarget> partitionedContractReader() {
        return new PartitionedContractReader(
                baseFeeCalculator,
                installationFeeCalculator,
                deviceInstallmentCalculator,
                discountCalculator,
                sqlSessionFactory,
                partitionedCalculationParameters(null, null, null, null, null, null)
        );
    }

    @Bean("partitionedCalculationProcessor")
    @StepScope
    public ItemProcessor<CalculationTarget, CalculationResultGroup> partitionedCalculationProcessor() {
        return new CalculationProcessor(
                baseFeeCalculator,
                installationFeeCalculator,
                deviceInstallmentCalculator,
                calculationResultProrater,
                discountCalculator,
                vatCalculator,
                partitionedCalculationParameters(null, null, null, null, null, null)
        );
    }

    /**
     * 파티션별 Writer Bean
     */
    @Bean("partitionedCalculationWriter")
    @StepScope
    public ItemWriter<CalculationResultGroup> partitionedCalculationWriter() {
        return new CalculationWriter(
                calculationResultSavePort, 
                partitionedCalculationParameters(null, null, null, null, null, null)
        );
    }

    /**
     * Worker Step - 각 파티션에서 실행되는 실제 처리 Step
     */
    @Bean("partitionedWorkerStep")
    public Step partitionedWorkerStep() {
        return new StepBuilder("partitionedWorkerStep", jobRepository)
                .<CalculationTarget, CalculationResultGroup>chunk(CHUNK_SIZE, transactionManager)
                .reader(partitionedContractReader())
                .processor(partitionedCalculationProcessor())
                .writer(partitionedCalculationWriter())
                .build();
    }

    /**
     * Partition Handler - 파티션들을 관리하고 병렬 실행
     */
    @Bean("partitionHandler")
    @StepScope
    public PartitionHandler partitionHandler(@Value("#{jobParameters['threadCount']}") String threadCountStr) {
        int threadCount = threadCountStr != null ? Integer.parseInt(threadCountStr) : 8;
        
        TaskExecutorPartitionHandler partitionHandler = new TaskExecutorPartitionHandler();
        partitionHandler.setStep(partitionedWorkerStep());
        partitionHandler.setTaskExecutor(partitionedTaskExecutor());
        partitionHandler.setGridSize(threadCount);  // 파티션 수 설정
        
        log.info("=== PartitionHandler 설정 ===");
        log.info("Grid Size (파티션 수): {}", threadCount);
        
        return partitionHandler;
    }

    /**
     * Master Step - Partitioner를 이용해 파티션을 생성하고 Worker Step들을 병렬 실행
     */
    @Bean("partitionedMasterStep")
    @StepScope
    public Step partitionedMasterStep() {
        return new StepBuilder("partitionedMasterStep", jobRepository)
                .partitioner("partitionedWorkerStep", contractPartitioner(null))
                .partitionHandler(partitionHandler(null))
                .build();
    }

    /**
     * Cleanup Step 설정 - 기존 계산 결과 삭제 (파티션 Job용)
     */
    @Bean("partitionedCleanupCalculationResultStep")
    public Step partitionedCleanupCalculationResultStep(CalculationResultCleanupTasklet calculationResultCleanupTasklet) {
        return new StepBuilder("partitionedCleanupCalculationResultStep", jobRepository)
                .tasklet(calculationResultCleanupTasklet, transactionManager)
                .build();
    }

    /**
     * Partitioned Job - Cleanup → Master Step 순서로 실행
     */
    @Bean("partitionedMonthlyFeeCalculationJob")
    public Job partitionedMonthlyFeeCalculationJob(
            CalculationResultCleanupTasklet calculationResultCleanupTasklet) {
        return new JobBuilder("partitionedMonthlyFeeCalculationJob", jobRepository)
                .start(partitionedCleanupCalculationResultStep(calculationResultCleanupTasklet))  // 1. 기존 결과 삭제
                .next(partitionedMasterStep())                                                     // 2. 파티션 기반 계산 수행
                .build();
    }
}