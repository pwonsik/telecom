package me.realimpact.telecom.billing.batch.runner;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.JobParameters;
import org.springframework.batch.core.JobParametersBuilder;
import org.springframework.batch.core.launch.JobLauncher;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.core.task.TaskExecutor;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.stereotype.Component;

/**
 * Spring Batch Job을 수동으로 실행하는 CommandLineRunner
 * spring.batch.job.names 파라미터를 기반으로 실행할 Job을 선택
 * Job 완료 후 애플리케이션을 정상 종료시킴
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class BatchCommandLineRunner implements CommandLineRunner {

    private final JobLauncher jobLauncher;
    private final ConfigurableApplicationContext applicationContext;

    @Value("${spring.batch.job.names:}")
    private String jobNames;

    @Override
    public void run(String... args) throws Exception {
        boolean hasFailure = false;

        try {
            if (jobNames == null || jobNames.trim().isEmpty()) {
                log.info("실행할 Job이 지정되지 않았습니다. spring.batch.job.names 파라미터를 설정해주세요.");
                return;
            }

            String[] jobNameArray = jobNames.split(",");

            for (String jobName : jobNameArray) {
                jobName = jobName.trim();
                log.info("=== Job 실행 시작: {} ===", jobName);

                try {
                    // ApplicationContext에서 Job Bean 찾기
                    Job job = applicationContext.getBean(jobName, Job.class);

                    // Job Parameters 생성 (중복 실행 방지를 위한 timestamp 추가)
                    JobParameters jobParameters = new JobParametersBuilder()
                            .addLong("timestamp", System.currentTimeMillis())
                            .toJobParameters();

                    // Job 실행
                    var jobExecution = jobLauncher.run(job, jobParameters);

                    log.info("=== Job 실행 완료: {} === 상태: {}", jobName, jobExecution.getStatus());

                    if (!jobExecution.getStatus().isUnsuccessful()) {
                        log.info("Job이 성공적으로 완료되었습니다: {}", jobName);
                    } else {
                        log.error("Job 실행 실패: {}, 상태: {}", jobName, jobExecution.getStatus());
                        hasFailure = true;
                    }

                } catch (Exception e) {
                    log.error("Job 실행 중 오류 발생: {}", jobName, e);
                    hasFailure = true;
                }
            }

            log.info("=== 모든 Job 실행 완료 ===");

        } finally {
            // TaskExecutor들을 찾아서 shutdown 호출
            shutdownTaskExecutors();

            // 애플리케이션 컨텍스트 종료
            log.info("=== 애플리케이션 종료 중... ===");

            // Exit code 설정 (0: 성공, 1: 실패)
            int exitCode = hasFailure ? 1 : 0;

            // Spring Context 정리 후 JVM 종료
            applicationContext.close();
            System.exit(exitCode);
        }
    }

    /**
     * 등록된 TaskExecutor들을 찾아서 shutdown 호출
     */
    private void shutdownTaskExecutors() {
        try {
            // TaskExecutor Bean들을 찾아서 shutdown
            applicationContext.getBeansOfType(ThreadPoolTaskExecutor.class)
                    .forEach((name, executor) -> {
                        log.info("TaskExecutor shutdown: {}", name);
                        executor.shutdown();
                    });

            // 일반 TaskExecutor도 확인
            applicationContext.getBeansOfType(TaskExecutor.class)
                    .forEach((name, executor) -> {
                        if (executor instanceof ThreadPoolTaskExecutor threadPoolExecutor) {
                            log.info("TaskExecutor shutdown: {}", name);
                            threadPoolExecutor.shutdown();
                        }
                    });

        } catch (Exception e) {
            log.warn("TaskExecutor shutdown 중 오류 발생", e);
        }
    }
}