package me.realimpact.telecom.calculation.application.vat;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;

/**
 * VAT 계산 관련 설정 프로퍼티
 * application.yml의 billing.vat 설정을 바인딩
 */
@Component
@ConfigurationProperties(prefix = "billing.vat")
@Data
public class VatProperties {
    
    /**
     * VAT 세율 (기본값: 10%)
     */
    private BigDecimal vatRate = BigDecimal.valueOf(0.10);
    
    /**
     * VAT 계산 활성화 여부 (기본값: true)
     */
    private boolean enabled = true;
}