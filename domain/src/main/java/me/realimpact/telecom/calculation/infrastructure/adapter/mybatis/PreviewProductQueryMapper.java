package me.realimpact.telecom.calculation.infrastructure.adapter.mybatis;

import me.realimpact.telecom.calculation.infrastructure.dto.ContractProductsSuspensionsDto;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.time.LocalDate;
import java.util.List;

// 이름 잘못 지었다.. 현재 유요한 상품만 조회하는 녀석이다.

@Mapper
public interface PreviewProductQueryMapper {
    List<ContractProductsSuspensionsDto> findContractsAndProductInventoriesByContractIds(
            @Param("contractIds") List<Long> contractIds,
            @Param("baseDate") LocalDate baseDate
    );
}
