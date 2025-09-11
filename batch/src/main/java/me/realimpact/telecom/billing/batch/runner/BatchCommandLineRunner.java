package me.realimpact.telecom.billing.batch.runner;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import me.realimpact.telecom.calculation.api.BillingCalculationPeriod;
import me.realimpact.telecom.calculation.api.BillingCalculationType;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.JobParameters;
import org.springframework.batch.core.JobParametersBuilder;
import org.springframework.batch.core.launch.JobLauncher;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

/**
 * 명령행에서 배치 작업을 실행하기 위한 CommandLineRunner
 * 
 * 사용법:
 * - Thread Pool 방식 (기본): java -jar batch.jar --billingStartDate=2024-03-01 --billingEndDate=2024-03-31
 * - Partitioner 방식: java -jar batch.jar --spring.batch.job.names=partitionedMonthlyFeeCalculationJob --billingStartDate=2024-03-01 --billingEndDate=2024-03-31
 * 
 * 주의: --spring.batch.job.names이 지정된 경우, 이 CommandLineRunner는 실행되지 않습니다.
 */
@Component
@Slf4j
public class BatchCommandLineRunner implements CommandLineRunner {

    private final JobLauncher jobLauncher;
    private final Job calculationJob;
    
    public BatchCommandLineRunner(JobLauncher jobLauncher, 
                                 @Qualifier("monthlyFeeCalculationJob") Job calculationJob) {
        this.jobLauncher = jobLauncher;
        this.calculationJob = calculationJob;
    }

    @Override
    public void run(String... args) throws Exception {
        // 디버깅을 위한 인수 출력
        log.info("받은 명령행 인수: {}", java.util.Arrays.toString(args));
        
        // 명령행 인수 파싱
        String billingStartDate = getArgumentValue(args, "billingStartDate");
        String billingEndDate = getArgumentValue(args, "billingEndDate");
        String contractIds = getArgumentValue(args, "contractIds");
        String threadCount = getArgumentValue(args, "threadCount");
        String billingCalculationType = getArgumentValue(args, "billingCalculationType");;
        String billingCalculationPeriod = getArgumentValue(args, "billingCalculationPeriod");;

        log.info("파싱된 파라미터 - billingStartDate: {}, billingEndDate: {}, contractId: {}, parallelDegree: {}, threadCount: {}, billingCalculationType: {}, billingCalculationPeriod: {}",
                billingStartDate, billingEndDate, contractIds, threadCount, billingCalculationType, billingCalculationPeriod);

        // 필수 파라미터 검증
        if (billingStartDate == null || billingEndDate == null) {
            log.error("필수 파라미터가 누락되었습니다.");
            log.info("사용법: java -jar batch.jar --billingStartDate=2024-03-01 --billingEndDate=2024-03-31 [--contractId=123] [--parallelDegree=4] [--threadCount=4]");
            System.exit(1);
            return;
        }

        log.info("=== 월정액 계산 배치 시작 ===");
        log.info("청구 시작일: {}", billingStartDate);
        log.info("청구 종료일: {}", billingEndDate);
        log.info("계약 ID: {}", contractIds != null ? contractIds : "전체");
        log.info("쓰레드 수: {}", threadCount != null ? threadCount : "4 (기본값)");

        try {
            // Job Parameters 생성 (null 값 방지)
            JobParametersBuilder builder = new JobParametersBuilder()
                    .addString("billingStartDate", billingStartDate)
                    .addString("billingEndDate", billingEndDate)
                    .addString("billingCalculationType", billingCalculationType)
                    .addString("billingCalculationPeriod", billingCalculationPeriod)
                    .addLong("timestamp", System.currentTimeMillis()); // 중복 실행 방지
            
            // contractId가 null이 아닌 경우에만 추가
            if (contractIds != null && !contractIds.trim().isEmpty()) {
                builder.addString("contractIds", contractIds);
            }

            // threadCount가 null이 아닌 경우에만 추가 (기본값은 Spring에서 처리)
            if (threadCount != null && !threadCount.trim().isEmpty()) {
                builder.addString("threadCount", threadCount);
            }
            
            JobParameters jobParameters = builder.toJobParameters();
            
            log.info("생성된 Job Parameters: {}", jobParameters);

            // 배치 작업 실행
            jobLauncher.run(calculationJob, jobParameters);
            
            log.info("=== 월정액 계산 배치 완료 ===");
            
        } catch (Exception e) {
            log.error("배치 작업 실행 중 오류 발생", e);
            e.printStackTrace();
            System.exit(1);
        }
    }

    /**
     * 명령행 인수에서 특정 파라미터 값을 추출
     */
    private String getArgumentValue(String[] args, String paramName) {
        String prefix = "--" + paramName + "=";
        for (String arg : args) {
            if (arg.startsWith(prefix)) {
                return arg.substring(prefix.length());
            }
        }
        return null;
    }
}