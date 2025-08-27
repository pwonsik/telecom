package me.realimpact.telecom.calculation.infrastructure.adapter.mybatis;

import me.realimpact.telecom.calculation.infrastructure.dto.RevenueMasterDataDto;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.time.LocalDate;
import java.util.List;

/**
 * 수익 항목 마스터 데이터 조회를 위한 MyBatis Mapper
 */
@Mapper
public interface RevenueMasterDataMapper {
    
    /**
     * 기준일 기준으로 유효한 수익 마스터 데이터를 조회한다
     * 
     * @param baseDate 기준일
     * @return 수익 마스터 데이터 목록
     */
    List<RevenueMasterDataDto> findByBaseDate(@Param("baseDate") LocalDate baseDate);
}