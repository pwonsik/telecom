package me.realimpact.telecom.calculation.infrastructure.adapter;

import java.time.LocalDate;
import me.realimpact.telecom.calculation.infrastructure.dto.ContractDto;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface ContractQueryMapper {
    
    ContractDto findContractWithProductsChargeItemsAndSuspensions(
        @Param("contractId") Long contractId,
        @Param("billingStartDate") LocalDate billingStartDate,
        @Param("billingEndDate") LocalDate billingEndDate
    );
}