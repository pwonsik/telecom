package me.realimpact.telecom.billing.batch.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * JSON 로깅을 위한 ObjectMapper 설정
 */
@Configuration
public class JsonLoggingConfig {
    
    @Bean
    public ObjectMapper objectMapper() {
        ObjectMapper mapper = new ObjectMapper();
        
        // Java 8 시간 타입 지원
        mapper.registerModule(new JavaTimeModule());
        
        // 날짜를 timestamp 대신 ISO 형식으로 출력
        mapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
        
        // null 값이 있는 필드는 출력하지 않음
        mapper.setDefaultPropertyInclusion(
            com.fasterxml.jackson.annotation.JsonInclude.Value.construct(
                com.fasterxml.jackson.annotation.JsonInclude.Include.NON_NULL,
                com.fasterxml.jackson.annotation.JsonInclude.Include.NON_NULL
            )
        );
        
        // 예쁜 JSON 출력 (로그 가독성 향상)
        mapper.enable(SerializationFeature.INDENT_OUTPUT);
        
        return mapper;
    }
}