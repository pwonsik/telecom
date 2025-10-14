package me.realimpact.telecom.billing.batch;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Spring Batch 애플리케이션의 메인 클래스.
 * 애플리케이션을 시작하는 진입점 역할을 한다.
 */
@SpringBootApplication(scanBasePackages = {"me.realimpact.telecom.calculation", "me.realimpact.telecom.billing.batch"})
@MapperScan("me.realimpact.telecom.calculation.infrastructure.adapter.mybatis")
public class BatchApplication {

    /**
     * 애플리케이션의 메인 메서드.
     * @param args 커맨드 라인 인자
     */
    public static void main(String[] args) {
        SpringApplication.run(BatchApplication.class, args);
    }

}