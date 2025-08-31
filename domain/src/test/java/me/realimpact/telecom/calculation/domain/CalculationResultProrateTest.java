package me.realimpact.telecom.calculation.domain;

import me.realimpact.telecom.calculation.domain.monthlyfee.DefaultPeriod;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;

class CalculationResultProrateTest {

    @Test
    void prorate_OverlappingPeriods_ReturnsCorrectProratedResults() {
        // given
        LocalDate effectiveStart = LocalDate.of(2024, 3, 1);
        LocalDate effectiveEnd = LocalDate.of(2024, 3, 31);
        BigDecimal originalFee = BigDecimal.valueOf(31000); // 31일 * 1000원
        
        CalculationResult<?> original = new CalculationResult<>(
            1L,
            LocalDate.of(2024, 3, 1),
            LocalDate.of(2024, 3, 31),
            "PRODUCT001",
            "CHARGE001", 
            "REVENUE001",
            effectiveStart,
            effectiveEnd,
            null,
            originalFee,
            originalFee,
            null,
            null
        );
        
        LocalDate newStart = LocalDate.of(2024, 3, 15);
        LocalDate newEnd = LocalDate.of(2024, 3, 25);
        
        // when
        var proratedResults = original.prorate(List.of(DefaultPeriod.of(newStart, newEnd)));
        
        // then
        assertThat(proratedResults).hasSize(1);
        
        CalculationResult<?> proratedResult = proratedResults.get(0);
        assertThat(proratedResult.getEffectiveStartDate()).isEqualTo(newStart);
        assertThat(proratedResult.getEffectiveEndDate()).isEqualTo(newEnd);
        
        // 11일간의 일할 계산: 31000 * (11/31) ≈ 11000 (약간의 반올림 오차 허용)
        BigDecimal expectedFee = BigDecimal.valueOf(11000.00);
        assertThat(proratedResult.getFee()).isCloseTo(expectedFee, within(BigDecimal.valueOf(0.1)));
        
        // 기존 속성들이 그대로 유지되는지 확인
        assertThat(proratedResult.getContractId()).isEqualTo(1L);
        assertThat(proratedResult.getProductOfferingId()).isEqualTo("PRODUCT001");
        assertThat(proratedResult.getChargeItemId()).isEqualTo("CHARGE001");
    }

    @Test
    void prorate_PartialOverlap_ReturnsPartialPeriod() {
        // given
        CalculationResult<?> original = new CalculationResult<>(
            1L,
            LocalDate.of(2024, 3, 1),
            LocalDate.of(2024, 3, 31),
            "PRODUCT001",
            "CHARGE001",
            "REVENUE001", 
            LocalDate.of(2024, 3, 10),  // 기존: 3/10 ~ 3/20
            LocalDate.of(2024, 3, 20),
            null,
            BigDecimal.valueOf(11000), // 11일 * 1000원
            BigDecimal.valueOf(11000), // 11일 * 1000원
            null,
            null
        );
        
        // when: 3/15 ~ 3/25로 prorate (겹치는 구간: 3/15 ~ 3/20)
        var results = original.prorate(
            List.of(DefaultPeriod.of(LocalDate.of(2024, 3, 15), LocalDate.of(2024, 3, 25)))
        );
        
        // then
        assertThat(results).hasSize(1);
        assertThat(results.get(0).getEffectiveStartDate()).isEqualTo(LocalDate.of(2024, 3, 15));
        assertThat(results.get(0).getEffectiveEndDate()).isEqualTo(LocalDate.of(2024, 3, 20));
        
        // 6일 / 11일 * 11000 ≈ 6000 (약간의 반올림 오차 허용)
        BigDecimal expectedFee = BigDecimal.valueOf(6000.00);
        assertThat(results.get(0).getFee()).isCloseTo(expectedFee, within(BigDecimal.valueOf(0.1)));
    }

    @Test
    void prorate_NoOverlap_ReturnsEmptyList() {
        // given
        CalculationResult<?> original = new CalculationResult<>(
            1L,
            LocalDate.of(2024, 3, 1),
            LocalDate.of(2024, 3, 31),
            "PRODUCT001",
            "CHARGE001",
            "REVENUE001",
            LocalDate.of(2024, 3, 1),
            LocalDate.of(2024, 3, 10),
            null,
            BigDecimal.valueOf(10000),
            BigDecimal.valueOf(11000), // 11일 * 1000원
            null,
            null
        );
        
        // when: 겹치지 않는 기간
        var results = original.prorate(
            List.of(DefaultPeriod.of(LocalDate.of(2024, 3, 15), LocalDate.of(2024, 3, 25)))
        );
        
        // then
        assertThat(results).isEmpty();
    }
    
    @Test
    void prorate_InvalidInput_ReturnsEmptyList() {
        // given
        CalculationResult<?> original = new CalculationResult<>(
            1L,
            LocalDate.of(2024, 3, 1),
            LocalDate.of(2024, 3, 31),
            "PRODUCT001",
            "CHARGE001",
            "REVENUE001",
            LocalDate.of(2024, 3, 1),
            LocalDate.of(2024, 3, 31),
            null,
            BigDecimal.valueOf(31000),
            BigDecimal.valueOf(31000), // 11일 * 1000원
            null,
            null
        );
        
        // when & then: null 입력
        assertThat(original.prorate(null)).isEmpty();
        assertThat(original.prorate(List.of())).isEmpty();
        
        // when & then: 잘못된 날짜 순서
        assertThat(original.prorate(
            List.of(DefaultPeriod.of(LocalDate.of(2024, 3, 31), LocalDate.of(2024, 3, 1)))
        )).isEmpty();
    }
}