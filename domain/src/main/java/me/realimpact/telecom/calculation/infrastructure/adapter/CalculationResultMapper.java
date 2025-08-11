package me.realimpact.telecom.calculation.infrastructure.adapter;

import me.realimpact.telecom.calculation.infrastructure.dto.CalculationResultDto;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 계산 결과 저장을 위한 MyBatis Mapper
 */
@Mapper
public interface CalculationResultMapper {

    /**
     * 대용량 배치 삽입 (Batch Insert)
     * 성능을 위해 여러 개의 결과를 한번에 저장
     */
    int batchInsertCalculationResults(@Param("results") List<CalculationResultDto> results);
    
    /**
     * 계약 ID와 청구 기간으로 계산 결과 조회
     */
    List<CalculationResultDto> selectCalculationResultsByContractAndPeriod(
        @Param("contractId") Long contractId,
        @Param("billingStartDate") String billingStartDate,
        @Param("billingEndDate") String billingEndDate
    );
}