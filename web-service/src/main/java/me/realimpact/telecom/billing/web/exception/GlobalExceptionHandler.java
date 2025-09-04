package me.realimpact.telecom.billing.web.exception;

import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.BindException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.context.request.WebRequest;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 전역 예외 처리 핸들러
 */
@ControllerAdvice
@Slf4j
public class GlobalExceptionHandler {

    /**
     * 검증 실패 예외 처리 (@Valid 검증 실패)
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleValidationException(
            MethodArgumentNotValidException ex, WebRequest request) {
        
        log.warn("검증 실패: {}", ex.getMessage());
        
        Map<String, String> fieldErrors = ex.getBindingResult()
                .getFieldErrors()
                .stream()
                .collect(Collectors.toMap(
                    FieldError::getField,
                    FieldError::getDefaultMessage,
                    (existing, replacement) -> existing
                ));

        ErrorResponse errorResponse = new ErrorResponse(
                "VALIDATION_FAILED",
                "입력값 검증에 실패했습니다",
                fieldErrors,
                request.getDescription(false),
                LocalDateTime.now()
        );

        return ResponseEntity.badRequest().body(errorResponse);
    }

    /**
     * 바인딩 예외 처리
     */
    @ExceptionHandler(BindException.class)
    public ResponseEntity<ErrorResponse> handleBindException(
            BindException ex, WebRequest request) {
        
        log.warn("바인딩 실패: {}", ex.getMessage());
        
        Map<String, String> fieldErrors = ex.getFieldErrors()
                .stream()
                .collect(Collectors.toMap(
                    FieldError::getField,
                    FieldError::getDefaultMessage,
                    (existing, replacement) -> existing
                ));

        ErrorResponse errorResponse = new ErrorResponse(
                "BIND_FAILED",
                "요청 데이터 바인딩에 실패했습니다",
                fieldErrors,
                request.getDescription(false),
                LocalDateTime.now()
        );

        return ResponseEntity.badRequest().body(errorResponse);
    }

    /**
     * 비즈니스 로직 예외 처리
     */
    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ErrorResponse> handleIllegalArgumentException(
            IllegalArgumentException ex, WebRequest request) {
        
        log.warn("잘못된 인자: {}", ex.getMessage());
        
        ErrorResponse errorResponse = new ErrorResponse(
                "INVALID_ARGUMENT",
                ex.getMessage(),
                new HashMap<>(),
                request.getDescription(false),
                LocalDateTime.now()
        );

        return ResponseEntity.badRequest().body(errorResponse);
    }

    /**
     * 일반적인 예외 처리
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleGenericException(
            Exception ex, WebRequest request) {
        
        log.error("예상치 못한 오류 발생", ex);
        
        ErrorResponse errorResponse = new ErrorResponse(
                "INTERNAL_SERVER_ERROR",
                "서버 내부 오류가 발생했습니다",
                new HashMap<>(),
                request.getDescription(false),
                LocalDateTime.now()
        );

        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
    }

    /**
     * 에러 응답 DTO
     */
    public record ErrorResponse(
            String errorCode,
            String message,
            Map<String, String> fieldErrors,
            String path,
            LocalDateTime timestamp
    ) {}
}