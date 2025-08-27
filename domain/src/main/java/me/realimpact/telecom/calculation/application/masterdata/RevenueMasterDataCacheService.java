package me.realimpact.telecom.calculation.application.masterdata;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import me.realimpact.telecom.calculation.domain.masterdata.RevenueMasterData;
import me.realimpact.telecom.calculation.infrastructure.dto.RevenueMasterDataDto;
import me.realimpact.telecom.calculation.port.out.RevenueMasterDataQueryPort;
import org.springframework.stereotype.Service;

import jakarta.annotation.PostConstruct;
import java.time.LocalDate;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 수익 항목 마스터 데이터 캐시 서비스
 * 애플리케이션 시작 시 현재일 기준 유효한 수익 마스터 데이터를 메모리에 로드하여 관리
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class RevenueMasterDataCacheService {
    private final RevenueMasterDataQueryPort revenueMasterDataQueryPort;
    
    private Map<String, RevenueMasterData> cache = new ConcurrentHashMap<>();
    
    /**
     * 애플리케이션 시작 시 캐시 초기화
     */
    @PostConstruct
    public void initCache() {
        refreshCache();
    }
    
    /**
     * 캐시를 갱신한다 (현재일 기준)
     */
    public void refreshCache() {
        LocalDate today = LocalDate.now();
        cache = revenueMasterDataQueryPort.findRevenueMasterDataByBaseDate(today);
        log.info("RevenueMasterData cache initialized with {} items for date: {}", 
                 cache.size(), today);
    }

    /**
     * 특정 수익 항목 ID에 해당하는 수익 마스터 데이터를 반환한다
     * 
     * @param revenueItemId 수익 항목 ID
     * @return 수익 마스터 데이터 (없으면 null)
     */
    public RevenueMasterData getRevenueMasterData(String revenueItemId) {
        return cache.get(revenueItemId);
    }
}