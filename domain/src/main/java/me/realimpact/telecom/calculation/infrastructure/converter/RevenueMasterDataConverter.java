package me.realimpact.telecom.calculation.infrastructure.converter;

import me.realimpact.telecom.calculation.domain.masterdata.RevenueMasterData;
import me.realimpact.telecom.calculation.infrastructure.dto.RevenueMasterDataDto;
import org.springframework.stereotype.Component;

/**
 * RevenueMasterData DTO와 Domain 객체 간 변환 로직
 */
@Component
public class RevenueMasterDataConverter {
    
    /**
     * DTO를 도메인 객체로 변환한다
     * 
     * @param dto RevenueMasterDataDto
     * @return RevenueMasterData 도메인 객체
     */
    public RevenueMasterData convertToDomain(RevenueMasterDataDto dto) {
        return new RevenueMasterData(
            dto.revenueItemId(),
            dto.effectiveStartDate(),
            dto.effectiveEndDate(),
            dto.revenueItemName(),
            dto.overdueChargeRevenueItemId(),
            dto.vatRevenueItemId()
        );
    }
}