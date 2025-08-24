package me.realimpact.telecom.billing.batch.config;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import me.realimpact.telecom.billing.batch.CalculationParameters;
import me.realimpact.telecom.billing.batch.CalculationResultGroup;
import me.realimpact.telecom.billing.batch.processor.CalculationProcessor;
import me.realimpact.telecom.billing.batch.reader.ChunkedContractReader;
import me.realimpact.telecom.billing.batch.writer.CalculationWriter;
import me.realimpact.telecom.calculation.api.BillingCalculationPeriod;
import me.realimpact.telecom.calculation.api.BillingCalculationType;
import me.realimpact.telecom.calculation.application.monthlyfee.BaseFeeCalculator;
import me.realimpact.telecom.calculation.application.onetimecharge.policy.DeviceInstallmentCalculator;
import me.realimpact.telecom.calculation.application.onetimecharge.policy.InstallationFeeCalculator;
import me.realimpact.telecom.calculation.infrastructure.adapter.mybatis.CalculationResultMapper;
import me.realimpact.telecom.calculation.infrastructure.adapter.mybatis.ContractQueryMapper;
import me.realimpact.telecom.calculation.infrastructure.adapter.mybatis.DeviceInstallmentMapper;
import me.realimpact.telecom.calculation.infrastructure.adapter.mybatis.InstallationHistoryMapper;
import me.realimpact.telecom.calculation.infrastructure.converter.CalculationResultFlattener;
import me.realimpact.telecom.calculation.infrastructure.converter.ContractDtoToDomainConverter;
import me.realimpact.telecom.calculation.infrastructure.converter.OneTimeChargeDtoConverter;
import me.realimpact.telecom.calculation.infrastructure.dto.ContractDto;
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
    private final BaseFeeCalculator monthlyFeeCalculatorService;
    private final ContractDtoToDomainConverter contractDtoToDomainConverter;
    private final OneTimeChargeDtoConverter oneTimeChargeDtoConverter;
    private final CalculationResultMapper calculationResultMapper;
    private final CalculationResultFlattener calculationResultFlattener;
    private final InstallationHistoryMapper installationHistoryMapper;
    private final DeviceInstallmentMapper deviceInstallmentMapper;
    private final ContractQueryMapper contractQueryMapper;
    private final DeviceInstallmentCalculator deviceInstallmentCalculator;
    private final InstallationFeeCalculator installationFeeCalculator;

    @Bean
    @StepScope
    public CalculationParameters calculationParameters(
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

        BillingCalculationType billingCalculationType = BillingCalculationType.valueOf(billingCalculationTypeStr);
        BillingCalculationPeriod billingCalculationPeriod = BillingCalculationPeriod.valueOf(billingCalculationPeriodStr);

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
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(calculationParameters.threadCount());        // Job Parameter로 받은 쓰레드 수
        executor.setMaxPoolSize(calculationParameters.threadCount() * 2);     // 최대 쓰레드 수
        executor.setQueueCapacity(1000);               // 대기 큐 크기
        executor.setThreadNamePrefix("batch-");
        executor.setWaitForTasksToCompleteOnShutdown(true);
        executor.setAwaitTerminationSeconds(60);
        executor.initialize();
        
        log.info("=== TaskExecutor 설정 ===");
        log.info("쓰레드 수: {}", calculationParameters.threadCount());
        log.info("최대 쓰레드 수: {}", calculationParameters.threadCount() * 2);
        
        return executor;
    }

    /**
     * ChunkedContractReader 설정 (Step Parameter 기반)
     * chunk size만큼 contract ID를 읽어서 bulk 조회로 ContractDto 생성
     * contractId가 있으면 단건, 없으면 전체 조회
     */
    @Bean
    @StepScope
    public ChunkedContractReader chunkedContractReader(CalculationParameters calculationParameters) {
        return new ChunkedContractReader(
            contractQueryMapper,
            installationHistoryMapper,
            deviceInstallmentMapper,
            sqlSessionFactory,
            calculationParameters
        );
    }
    
    /**
     * 멀티쓰레드 환경용 Thread-Safe Reader
     */
    @Bean
    @StepScope  
    public SynchronizedItemStreamReader<ContractDto> contractReader(CalculationParameters calculationParameters) {
        SynchronizedItemStreamReader<ContractDto> reader = new SynchronizedItemStreamReader<>();
        reader.setDelegate(chunkedContractReader(calculationParameters));  // ChunkedContractReader 사용
        return reader;
    }

    @Bean
    @StepScope
    public ItemProcessor<ContractDto, CalculationResultGroup> calculationProcessor(CalculationParameters calculationParameters) {
        return new CalculationProcessor(
            monthlyFeeCalculatorService,
            installationFeeCalculator,
            deviceInstallmentCalculator,
            contractDtoToDomainConverter,
            oneTimeChargeDtoConverter,
            calculationParameters
        );
    }

    /**
     * Writer Bean 설정 (@StepScope) - 커스텀 Writer 사용
     */
    @Bean
    @StepScope
    public ItemWriter<CalculationResultGroup> calculationWriter(CalculationParameters calculationParameters) {
        return new CalculationWriter(
            calculationResultMapper, calculationParameters);
    }


    /**
     * Step 설정 - 멀티쓰레드 처리로 성능 최적화
     */
    @Bean
    public Step monthlyFeeCalculationStep(CalculationParameters calculationParameters) {
        return new StepBuilder("monthlyFeeCalculationStep", jobRepository)
                .<ContractDto, CalculationResultGroup>chunk(CHUNK_SIZE, transactionManager)  // 상수화된 chunk size 사용
                .reader(contractReader(calculationParameters))  // Thread-Safe Reader 사용
                .processor(calculationProcessor(calculationParameters))  // @StepScope Processor 사용
                .writer(calculationWriter(calculationParameters))        // @StepScope Writer 사용
                .taskExecutor(taskExecutor(calculationParameters))             // 멀티쓰레드 실행 (@JobScope가 런타임에 실제 값 주입)
                .build();
    }

    /**
     * Job 설정
     */
    @Bean
    public Job monthlyFeeCalculationJob(CalculationParameters calculationParameters) {
        return new JobBuilder("monthlyFeeCalculationJob", jobRepository)
                .start(monthlyFeeCalculationStep(calculationParameters))
                .build();
    }
}