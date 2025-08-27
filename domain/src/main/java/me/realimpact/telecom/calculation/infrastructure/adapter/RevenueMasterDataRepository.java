package me.realimpact.telecom.calculation.infrastructure.adapter;

import lombok.RequiredArgsConstructor;
import me.realimpact.telecom.calculation.domain.masterdata.RevenueMasterData;
import me.realimpact.telecom.calculation.infrastructure.adapter.mybatis.RevenueMasterDataMapper;
import me.realimpact.telecom.calculation.infrastructure.converter.RevenueMasterDataConverter;
import me.realimpact.telecom.calculation.port.out.RevenueMasterDataQueryPort;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 수익 항목 마스터 데이터 Repository 구현체
 */
@Repository
@RequiredArgsConstructor
public class RevenueMasterDataRepository implements RevenueMasterDataQueryPort {
    private final RevenueMasterDataMapper revenueMasterDataMapper;
    private final RevenueMasterDataConverter revenueMasterDataConverter;

    @Override
    public Map<String, RevenueMasterData> findRevenueMasterDataByBaseDate(LocalDate baseDate) {
        return revenueMasterDataMapper.findByBaseDate(baseDate)
            .stream()
            .collect(Collectors.toMap(
                dto -> dto.revenueItemId(),
                dto -> revenueMasterDataConverter.convertToDomain(dto)
            ));
    }
}