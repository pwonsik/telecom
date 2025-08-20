package me.realimpact.telecom.calculation.infrastructure.adapter;

import me.realimpact.telecom.calculation.infrastructure.dto.InstallationHistoryDto;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.time.LocalDate;
import java.util.List;

/**
 * 설치내역 조회를 위한 MyBatis Mapper
 */
@Mapper
public interface InstallationHistoryMapper {
    
    /**
     * 계약 ID 목록으로 설치내역을 조회한다
     * 
     * @param contractIds 계약 ID 목록
     * @param billingEndDate 청구 종료일
     * @return 설치내역 목록
     */
    List<InstallationHistoryDto> findInstallationsByContractIds(
        @Param("contractIds") List<Long> contractIds,
        @Param("billingEndDate") LocalDate billingEndDate
    );
}