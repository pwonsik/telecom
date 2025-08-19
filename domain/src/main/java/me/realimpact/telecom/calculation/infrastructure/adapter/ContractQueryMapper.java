package me.realimpact.telecom.calculation.infrastructure.adapter;

import java.time.LocalDate;
import java.util.List;

import me.realimpact.telecom.calculation.infrastructure.dto.ContractDto;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface ContractQueryMapper {
    
    /**
     * 단건 조회: 특정 계약 ID로 조회
     */
    List<ContractDto> findContractWithProductsChargeItemsAndSuspensions(
        @Param("contractId") Long contractId,
        @Param("billingStartDate") LocalDate billingStartDate,
        @Param("billingEndDate") LocalDate billingEndDate
    );
    
    /**
     * 계약 ID 목록 조회: Spring Batch용
     * contractId가 null이면 전체 조회, 있으면 해당 계약만 조회
     */
    List<Long> findContractIds(
        @Param("contractId") Long contractId,
        @Param("billingStartDate") LocalDate billingStartDate,
        @Param("billingEndDate") LocalDate billingEndDate
    );
    
    /**
     * 계약 ID 목록으로 계약 데이터 조회: Spring Batch용 (IN 조건)
     */
    List<ContractDto> findContractsByIds(
        @Param("contractIds") List<Long> contractIds,
        @Param("billingStartDate") LocalDate billingStartDate,
        @Param("billingEndDate") LocalDate billingEndDate
    );
}