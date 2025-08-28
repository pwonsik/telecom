package me.realimpact.telecom.calculation.infrastructure.dto;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.Accessors;

import java.time.LocalDate;

/**
 * 수익 항목 마스터 데이터 DTO
 * 
 * @param revenueItemId 수익 항목 ID
 * @param effectiveStartDate 유효 시작일
 * @param effectiveEndDate 유효 종료일
 * @param revenueItemName 수익 항목명
 * @param overdueChargeRevenueItemId 연체료 수익 항목 ID
 * @param vatRevenueItemId VAT 수익 항목 ID
 */
@Getter
@Setter
@Accessors(fluent = true)
@NoArgsConstructor
public class RevenueMasterDataDto {
    private String revenueItemId;
    private LocalDate effectiveStartDate;
    private LocalDate effectiveEndDate;
    private String revenueItemName;
    private String overdueChargeRevenueItemId;
    private String vatRevenueItemId;
}