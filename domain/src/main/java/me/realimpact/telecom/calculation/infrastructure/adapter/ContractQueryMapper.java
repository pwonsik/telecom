package me.realimpact.telecom.calculation.infrastructure.adapter;

import java.time.LocalDate;
import me.realimpact.telecom.calculation.infrastructure.dto.ContractDto;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface ContractQueryMapper {
    
    /**
     * 단건 조회: 특정 계약 ID로 조회
     */
    ContractDto findContractWithProductsChargeItemsAndSuspensions(
        @Param("contractId") Long contractId,
        @Param("billingStartDate") LocalDate billingStartDate,
        @Param("billingEndDate") LocalDate billingEndDate
    );
    
    /**
     * 전체 조회: Spring Batch MyBatisPagingItemReader용
     * contractId가 null이면 전체 조회, 있으면 해당 계약만 조회
     */
    ContractDto findContractsWithProductsChargeItemsAndSuspensions(
        @Param("billingStartDate") LocalDate billingStartDate,
        @Param("billingEndDate") LocalDate billingEndDate
    );
}