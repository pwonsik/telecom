package me.realimpact.telecom.calculation.infrastructure.adapter.mybatis;

import me.realimpact.telecom.calculation.infrastructure.dto.ContractProductsSuspensionsDto;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.time.LocalDate;
import java.util.List;

@Mapper
public interface ProductQueryMapper {
    /**
     * 계약 ID 목록으로 계약 데이터 조회: Spring Batch용 (IN 조건)
     */
    List<ContractProductsSuspensionsDto> findContractsAndProductInventoriesByContractIds(
        @Param("contractIds") List<Long> contractIds,
        @Param("billingStartDate") LocalDate billingStartDate,
        @Param("billingEndDate") LocalDate billingEndDate
    );
}