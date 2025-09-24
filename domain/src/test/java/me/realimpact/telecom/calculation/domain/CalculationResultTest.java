package me.realimpact.telecom.calculation.domain;

import me.realimpact.telecom.calculation.api.BillingCalculationPeriod;
import me.realimpact.telecom.calculation.api.BillingCalculationType;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.assertj.core.api.Assertions.assertThat;

class CalculationResultTest {

    @Test
    void executePost_PostProcessorExists_ExecutesSuccessfully() {
        // given
        AtomicBoolean postProcessorExecuted = new AtomicBoolean(false);
        PostProcessor postProcessor = (ctx, result) -> {
            postProcessorExecuted.set(true);
        };
        
        CalculationResult<?> calculationResult = new CalculationResult<>(
            1L,
            LocalDate.of(2024, 3, 1),
            LocalDate.of(2024, 3, 31),
            "PO001",
            "CI001",
            "REV001",
            LocalDate.of(2024, 3, 1),
            LocalDate.of(2024, 3, 31),
            null,
            BigDecimal.valueOf(10000),
            BigDecimal.valueOf(10000),
            null,
            postProcessor
        );
        
        CalculationContext ctx = new CalculationContext(
            LocalDate.of(2024, 3, 1),
            LocalDate.of(2024, 3, 31),
            BillingCalculationType.REVENUE_CONFIRMATION,
            BillingCalculationPeriod.POST_BILLING_CURRENT_MONTH
        );
        
        // when
        calculationResult.executePost(ctx);
        
        // then
        assertThat(postProcessorExecuted.get()).isTrue();
    }
    
    @Test
    void executePost_PostProcessorIsNull_DoesNotThrowException() {
        // given
        CalculationResult<?> calculationResult = new CalculationResult<>(
            1L,
            LocalDate.of(2024, 3, 1),
            LocalDate.of(2024, 3, 31),
            "PO001",
            "CI001",
            "REV001",
            LocalDate.of(2024, 3, 1),
            LocalDate.of(2024, 3, 31),
            null,
            BigDecimal.valueOf(10000),
            BigDecimal.valueOf(10000),
            null,
            null // postProcessor is null
        );
        
        CalculationContext ctx = new CalculationContext(
            LocalDate.of(2024, 3, 1),
            LocalDate.of(2024, 3, 31),
            BillingCalculationType.REVENUE_CONFIRMATION,
            BillingCalculationPeriod.POST_BILLING_CURRENT_MONTH
        );
        
        // when & then (should not throw any exception)
        calculationResult.executePost(ctx);
    }
}