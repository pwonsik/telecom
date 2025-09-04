
package me.realimpact.telecom.billing.web;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication(scanBasePackages = {"me.realimpact.telecom.calculation", "me.realimpact.telecom.billing.web"})
@MapperScan("me.realimpact.telecom.calculation.infrastructure.adapter.mybatis")
public class WebServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(WebServiceApplication.class, args);
    }

}
