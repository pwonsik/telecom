package me.realimpact.telecom.billing.batch.util;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * JSON 로깅을 위한 유틸리티 클래스
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class JsonLoggingHelper {
    
    private final ObjectMapper objectMapper;
    
    /**
     * 객체를 JSON 문자열로 변환
     */
    public String toJson(Object obj) {
        try {
            return objectMapper.writeValueAsString(obj);
        } catch (JsonProcessingException e) {
            log.warn("JSON 변환 실패: {}", e.getMessage());
            return obj.toString();
        }
    }
    
    /**
     * 객체를 압축된 JSON 문자열로 변환 (한줄 출력)
     */
    public String toCompactJson(Object obj) {
        try {
            ObjectMapper compactMapper = objectMapper.copy();
            compactMapper.disable(com.fasterxml.jackson.databind.SerializationFeature.INDENT_OUTPUT);
            return compactMapper.writeValueAsString(obj);
        } catch (JsonProcessingException e) {
            log.warn("JSON 변환 실패: {}", e.getMessage());
            return obj.toString();
        }
    }
    
    /**
     * 로그 메시지와 함께 객체를 JSON으로 출력
     */
    public void logJson(String message, Object obj) {
        log.info("{}", message);
        log.info("{}", toJson(obj));
    }
    
    /**
     * 로그 메시지와 함께 객체를 압축된 JSON으로 출력
     */
    public void logCompactJson(String message, Object obj) {
        log.info("{}: {}", message, toCompactJson(obj));
    }
    
    /**
     * DEBUG 레벨로 JSON 로그 출력
     */
    public void debugJson(String message, Object obj) {
        if (log.isDebugEnabled()) {
            log.debug("{}", message);
            log.debug("{}", toJson(obj));
        }
    }
    
    /**
     * ERROR 레벨로 JSON 로그 출력
     */
    public void errorJson(String message, Object obj) {
        log.error("{}", message);
        log.error("{}", toJson(obj));
    }
}