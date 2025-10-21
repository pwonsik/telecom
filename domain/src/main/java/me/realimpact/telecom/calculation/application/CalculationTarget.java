package me.realimpact.telecom.calculation.application;

import java.util.Collections;
import java.util.List;
import java.util.Map;

import me.realimpact.telecom.calculation.domain.discount.Discount;
import me.realimpact.telecom.calculation.domain.monthlyfee.MonthlyChargeDomain;
import me.realimpact.telecom.calculation.domain.onetimecharge.OneTimeChargeDomain;

public record CalculationTarget(
	    Long contractId,
	    Map<Class<? extends MonthlyChargeDomain>, List<? extends MonthlyChargeDomain>> monthlyChargeData,
	    Map<Class<? extends OneTimeChargeDomain>, List<? extends OneTimeChargeDomain>> oneTimeChargeData,
	    List<Discount> discounts
	) {
	    
	    /**
	     * 특정 타입의 MonthlyCharge 데이터 조회
	     * @param type 조회할 데이터 타입
	     * @return 해당 타입의 데이터 목록
	     */
	    @SuppressWarnings("unchecked")
	    public <T extends MonthlyChargeDomain> List<T> getMonthlyChargeData(Class<T> type) {
	        List<? extends MonthlyChargeDomain> data = monthlyChargeData.getOrDefault(type, Collections.emptyList());
	        return (List<T>) data;
	    }

	    /**
	     * 특정 타입의 OneTimeCharge 데이터 조회
	     * @param type 조회할 데이터 타입
	     * @return 해당 타입의 데이터 목록
	     */
	    @SuppressWarnings("unchecked")
	    public <T extends OneTimeChargeDomain> List<T> getOneTimeChargeData(Class<T> type) {
	        List<? extends OneTimeChargeDomain> data = oneTimeChargeData.getOrDefault(type, Collections.emptyList());
	        return (List<T>) data;
	    }

	}
