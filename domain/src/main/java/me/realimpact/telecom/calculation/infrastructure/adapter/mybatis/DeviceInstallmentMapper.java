package me.realimpact.telecom.calculation.infrastructure.adapter.mybatis;

import me.realimpact.telecom.calculation.infrastructure.dto.DeviceInstallmentDto;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.time.LocalDate;
import java.util.List;

/**
 * 단말할부내역 조회를 위한 MyBatis Mapper
 */
@Mapper
public interface DeviceInstallmentMapper {
    
    /**
     * 계약 ID 목록으로 단말할부내역을 조회한다
     * 
     * @param contractIds 계약 ID 목록
     * @param billingEndDate 청구 종료일
     * @return 단말할부내역 목록
     */
    List<DeviceInstallmentDto> findInstallmentsByContractIds(
        @Param("contractIds") List<Long> contractIds,
        @Param("billingEndDate") LocalDate billingEndDate
    );
}