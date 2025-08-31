package me.realimpact.telecom.calculation.application.vat;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import me.realimpact.telecom.calculation.application.masterdata.RevenueMasterDataCacheService;
import me.realimpact.telecom.calculation.domain.CalculationContext;
import me.realimpact.telecom.calculation.domain.CalculationResult;
import me.realimpact.telecom.calculation.domain.masterdata.RevenueMasterData;
import me.realimpact.telecom.calculation.port.out.CalculationResultSavePort;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.List;
import java.util.Objects;

/**
 * VAT 계산기
 * 기존 CalculationResult들을 기반으로 VAT CalculationResult를 생성한다.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class VatCalculator {
    
    private final VatProperties vatProperties;
    private final RevenueMasterDataCacheService revenueCacheService;
    
    /**
     * 기존 CalculationResult들을 기반으로 VAT CalculationResult 생성
     * 
     * @param ctx 계산 컨텍스트
     * @param existingResults 기존 과금 계산 결과 목록
     * @return VAT 계산 결과 목록
     */
    public List<CalculationResult<?>> calculateVat(
            CalculationContext ctx, 
            List<CalculationResult<?>> existingResults) {
        
        if (!vatProperties.isEnabled()) {
            log.debug("VAT calculation is disabled");
            return List.of();
        }
        
        log.debug("Starting VAT calculation for {} results with VAT rate: {}", 
                  existingResults.size(), vatProperties.getVatRate());
        
        List<CalculationResult<?>> vatResults = existingResults.stream()
                .filter(this::isVatApplicable)
                .map(result -> createVatCalculationResult(ctx, result))
                .filter(Objects::nonNull)
                .<CalculationResult<?>>map(result -> result)
                .toList();
        
        log.debug("Generated {} VAT calculation results", vatResults.size());
        return vatResults;
    }
    
    /**
     * VAT 계산 대상인지 확인
     * RevenueMasterData에 vatRevenueItemId가 설정된 경우만 VAT 계산 대상
     */
    private boolean isVatApplicable(CalculationResult<?> result) {
        if (result.getRevenueItemId() == null) {
            return false;
        }
        
        RevenueMasterData masterData = revenueCacheService.getRevenueMasterData(result.getRevenueItemId());
        boolean applicable = masterData != null && masterData.vatRevenueItemId() != null;
        
        if (applicable) {
            log.trace("VAT applicable for revenueItemId: {} -> vatRevenueItemId: {}", 
                     result.getRevenueItemId(), masterData.vatRevenueItemId());
        }
        
        return applicable;
    }
    
    /**
     * 기존 CalculationResult를 기반으로 VAT CalculationResult 생성
     */
    private CalculationResult<?> createVatCalculationResult(CalculationContext ctx, CalculationResult<?> originalResult) {
        try {
            RevenueMasterData masterData = revenueCacheService.getRevenueMasterData(originalResult.getRevenueItemId());
            if (masterData == null || masterData.vatRevenueItemId() == null) {
                return null;
            }
            
            BigDecimal vatAmount = calculateVatAmount(originalResult.getFee());
            
            return new CalculationResult<>(
                originalResult.getContractId(),
                originalResult.getBillingStartDate(),
                originalResult.getBillingEndDate(),
                originalResult.getProductOfferingId(),
                originalResult.getChargeItemId(),
                masterData.vatRevenueItemId(), // VAT 전용 수익항목 ID 사용
                originalResult.getEffectiveStartDate(),
                originalResult.getEffectiveEndDate(),
                originalResult.getSuspensionType(),
                vatAmount,
                BigDecimal.ZERO,
                null,
                null // VAT 계산은 후처리가 필요 없음
            );
        } catch (Exception e) {
            log.error("Error creating VAT calculation result for contractId: {}, revenueItemId: {}", 
                     originalResult.getContractId(), originalResult.getRevenueItemId(), e);
            return null;
        }
    }
    
    /**
     * VAT 금액 계산
     * 
     * @param taxableAmount 과세 대상 금액
     * @return VAT 금액
     */
    private BigDecimal calculateVatAmount(BigDecimal taxableAmount) {
        if (taxableAmount == null || taxableAmount.compareTo(BigDecimal.ZERO) == 0) {
            return BigDecimal.ZERO;
        }
        
        return taxableAmount.multiply(vatProperties.getVatRate());
    }
}