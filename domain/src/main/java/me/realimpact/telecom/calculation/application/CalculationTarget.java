package me.realimpact.telecom.calculation.application;

import me.realimpact.telecom.calculation.domain.discount.Discount;
import me.realimpact.telecom.calculation.domain.monthlyfee.ContractWithProductsAndSuspensions;
import me.realimpact.telecom.calculation.domain.onetimecharge.OneTimeChargeDomain;
import me.realimpact.telecom.calculation.domain.onetimecharge.policy.installation.InstallationHistory;
import me.realimpact.telecom.calculation.domain.onetimecharge.policy.installment.DeviceInstallmentMaster;

import java.util.Collections;
import java.util.List;
import java.util.Map;

public record CalculationTarget(
    Long contractId,
    List<ContractWithProductsAndSuspensions> contractWithProductsAndSuspensions,
    Map<Class<? extends OneTimeChargeDomain>, List<OneTimeChargeDomain>> oneTimeChargeData,
    List<Discount> discounts
) {
    
    /**
     * 특정 타입의 OneTimeCharge 데이터 조회
     * @param type 조회할 데이터 타입
     * @return 해당 타입의 데이터 목록
     */
    @SuppressWarnings("unchecked")
    public <T extends OneTimeChargeDomain> List<T> getOneTimeChargeData(Class<T> type) {
        List<OneTimeChargeDomain> data = oneTimeChargeData.getOrDefault(type, Collections.emptyList());
        return (List<T>) data;
    }
    
    /**
     * 기존 호환성을 위한 InstallationHistory 조회 메서드
     * @return InstallationHistory 목록
     */
    public List<InstallationHistory> installationHistories() {
        return getOneTimeChargeData(InstallationHistory.class);
    }
    
    /**
     * 기존 호환성을 위한 DeviceInstallmentMaster 조회 메서드
     * @return DeviceInstallmentMaster 목록
     */
    public List<DeviceInstallmentMaster> deviceInstallmentMasters() {
        return getOneTimeChargeData(DeviceInstallmentMaster.class);
    }
}
