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
 * 요금 계산 관련 REST API 엔드포인트를 제공하는 컨트롤러 클래스.
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
     * 지정된 계약들에 대해 월요금, 일회성 요금, 할인, 부가세(VAT)를 포함한 통합 요금 계산을 수행한다.
     * 
     * @param request 계산 요청 정보를 담은 DTO. 계약 ID 목록, 청구 기간 등을 포함한다.
     * @return 각 계약별 계산 결과를 담은 `CalculationResultGroup` 리스트를 포함하는 ResponseEntity.
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
            // 요청 정보를 바탕으로 계산 컨텍스트를 생성한다.
            CalculationContext context = new CalculationContext(
                    request.billingStartDate(),
                    request.billingEndDate(), 
                    request.billingCalculationType(),
                    request.billingCalculationPeriod()
            );

            // 유스케이스를 실행하여 계산을 수행하고 결과를 받는다.
            List<CalculationResultGroup> response = calculationCommandUseCase.calculate(request.contractIds(), context);

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            log.error("계산 처리 중 오류 발생", e);
            throw e; // GlobalExceptionHandler에서 공통으로 예외를 처리한다.
        }
    }

}
