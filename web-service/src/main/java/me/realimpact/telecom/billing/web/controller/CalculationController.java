package me.realimpact.telecom.billing.web.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import me.realimpact.telecom.calculation.api.*;
import me.realimpact.telecom.calculation.domain.CalculationContext;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.Valid;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 계산 관련 REST API Controller
 */
@Tag(name = "계산 API", description = "통신요금 계산 관련 API")
@RestController
@RequestMapping("/api/calculations")
@RequiredArgsConstructor
@Validated
@Slf4j
public class CalculationController {

    private final CalculationCommandUseCase calculationCommandUseCase;

    /**
     * 계약별 요금 계산
     * 
     * @param request 계산 요청 정보
     * @return 계약별 계산 결과
     */
    @Operation(
            summary = "계약별 요금 계산", 
            description = "지정된 계약들에 대해 월요금, 일회성 요금, 할인, VAT를 포함한 통합 요금 계산을 수행합니다."
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200", 
                    description = "계산 성공",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = CalculationResultGroup.class)
                    )
            ),
            @ApiResponse(
                    responseCode = "400", 
                    description = "잘못된 요청 파라미터",
                    content = @Content(mediaType = "application/json")
            ),
            @ApiResponse(
                    responseCode = "500", 
                    description = "서버 내부 오류",
                    content = @Content(mediaType = "application/json")
            )
    })
    @PostMapping
    public ResponseEntity<List<CalculationResultGroup>> calculateFees(
            @Parameter(description = "계산 요청 정보", required = true)
            @Valid @RequestBody CalculationRequest request) {
        
        log.info("계산 요청 수신: 계약 {} 건, 기간: {} ~ {}, 유형: {}", 
                request.contractIds().size(),
                request.billingStartDate(), 
                request.billingEndDate(),
                request.billingCalculationType());

        try {
            // CalculationContext 생성
            CalculationContext context = new CalculationContext(
                    request.billingStartDate(),
                    request.billingEndDate(), 
                    request.billingCalculationType(),
                    request.billingCalculationPeriod()
            );

            // Response 변환
            List<CalculationResultGroup> response = calculationCommandUseCase.calculate(request.contractIds(), context);

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            log.error("계산 처리 중 오류 발생", e);
            throw e; // GlobalExceptionHandler에서 처리
        }
    }

}