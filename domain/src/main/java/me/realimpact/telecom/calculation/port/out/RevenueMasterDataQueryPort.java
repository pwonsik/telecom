package me.realimpact.telecom.calculation.port.out;

import me.realimpact.telecom.calculation.domain.masterdata.RevenueMasterData;
import me.realimpact.telecom.calculation.infrastructure.dto.RevenueMasterDataDto;

import java.time.LocalDate;
import java.util.Map;

/**
 * 수익 항목 마스터 데이터 조회 포트
 */
public interface RevenueMasterDataQueryPort {
    
    /**
     * 기준일 기준으로 유효한 수익 마스터 데이터를 조회한다
     * 
     * @param baseDate 기준일
     * @return revenueItemId를 key로 하는 수익 마스터 데이터 Map
     */
    Map<String, RevenueMasterData> findRevenueMasterDataByBaseDate(LocalDate baseDate);
}