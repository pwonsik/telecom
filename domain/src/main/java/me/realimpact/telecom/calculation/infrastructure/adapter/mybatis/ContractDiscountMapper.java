package me.realimpact.telecom.calculation.infrastructure.adapter.mybatis;

import me.realimpact.telecom.calculation.domain.discount.Discount;
import me.realimpact.telecom.calculation.infrastructure.dto.ContractDiscountDto;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.time.LocalDate;
import java.util.List;

/**
 * 계약 할인 내역 조회를 위한 MyBatis Mapper
 */
@Mapper
public interface ContractDiscountMapper {
    
    /**
     * 계약 ID 목록으로 할인 내역을 조회한다
     * 
     * @param contractIds 계약 ID 목록
     * @param billingStartDate 청구 시작일
     * @param billingEndDate 청구 종료일
     * @return 계약 할인 내역 목록
     */
    List<ContractDiscountDto> findDiscountsByContractIds(
        @Param("contractIds") List<Long> contractIds,
        @Param("billingStartDate") LocalDate billingStartDate,
        @Param("billingEndDate") LocalDate billingEndDate
    );

    void applyDiscount(Discount discount);
}