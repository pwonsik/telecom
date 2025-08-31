package me.realimpact.telecom.calculation.infrastructure.adapter.mybatis;

import me.realimpact.telecom.calculation.domain.CalculationResult;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.time.LocalDate;
import java.util.List;

/**
 * 계산 결과 저장을 위한 MyBatis Mapper
 * 평면화된 구조로 batch insert 처리
 */
@Mapper
public interface CalculationResultMapper {

    /**
     * 평면화된 계산 결과를 배치로 삽입
     * @param items 평면화된 계산 결과 목록
     * @return 삽입된 행 수
     */
    int batchInsertCalculationResults(
        @Param("items") List<CalculationResult<?>> items
    );

    /**
     * 모든 계산 결과를 삭제 (테스트 및 전체 초기화 용도)
     * @return 삭제된 행 수
     */
    int deleteAllCalculationResults();
}