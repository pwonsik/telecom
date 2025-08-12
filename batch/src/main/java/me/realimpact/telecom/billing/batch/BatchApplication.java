
package me.realimpact.telecom.billing.batch;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication(scanBasePackages = {"me.realimpact.telecom.calculation", "me.realimpact.telecom.billing.batch"})
@MapperScan("me.realimpact.telecom.calculation.infrastructure.adapter")
public class BatchApplication {

    public static void main(String[] args) {
        SpringApplication.run(BatchApplication.class, args);
    }

}
