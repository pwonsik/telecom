package me.realimpact.telecom.calculation.infrastructure.dto;

import lombok.Data;
import me.realimpact.telecom.calculation.domain.monthlyfee.Suspension;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * 평면화된 계산 결과 DTO
 * MonthlyFeeCalculationResult의 각 MonthlyFeeCalculationResultItem을 개별 레코드로 저장하기 위한 DTO
 */
@Data
public class FlatCalculationResultDto {
    
    // Contract 정보 (각 item 레코드마다 중복 저장)
    private Long contractId;
    private LocalDate billingStartDate;
    private LocalDate billingEndDate;
    
    // MonthlyFeeCalculationResultItem 정보
    private String productOfferingId;
    private String monthlyChargeItemId;
    private LocalDate effectiveStartDate;
    private LocalDate effectiveEndDate;
    private String suspensionType;  // Enum을 String으로 변환
    private BigDecimal fee;
    
    /**
     * suspensionType을 enum에서 문자열로 변환
     */
    public void setSuspensionType(Suspension.SuspensionType suspensionType) {
        this.suspensionType = suspensionType != null ? suspensionType.name() : null;
    }
    
    /**
     * suspensionType을 문자열에서 enum으로 변환
     */
    public Suspension.SuspensionType getSuspensionTypeAsEnum() {
        return suspensionType != null ? Suspension.SuspensionType.valueOf(suspensionType) : null;
    }
}