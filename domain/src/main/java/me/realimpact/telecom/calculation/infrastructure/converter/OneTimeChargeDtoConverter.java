package me.realimpact.telecom.calculation.infrastructure.converter;

import me.realimpact.telecom.calculation.domain.onetimecharge.policy.installation.InstallationHistory;
import me.realimpact.telecom.calculation.domain.onetimecharge.policy.installment.DeviceInstallmentDetail;
import me.realimpact.telecom.calculation.domain.onetimecharge.policy.installment.DeviceInstallmentMaster;
import me.realimpact.telecom.calculation.infrastructure.dto.DeviceInstallmentDetailDto;
import me.realimpact.telecom.calculation.infrastructure.dto.DeviceInstallmentDto;
import me.realimpact.telecom.calculation.infrastructure.dto.InstallationHistoryDto;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * 일회성 과금 도메인 객체와 DTO 간의 변환을 담당하는 컨버터
 */
@Component
public class OneTimeChargeDtoConverter {
    
    /**
     * InstallationHistoryDto를 InstallationHistory 도메인 객체로 변환
     */
    public InstallationHistory convertToInstallationHistory(InstallationHistoryDto dto) {
        return new InstallationHistory(
            dto.contractId(),
            dto.sequenceNumber(),
            dto.installationDate(),
            dto.installationFee(),
            dto.billedFlag()
        );
    }
    
    /**
     * InstallationHistoryDto 리스트를 InstallationHistory 도메인 객체 리스트로 변환
     */
    public List<InstallationHistory> convertToInstallationHistories(List<InstallationHistoryDto> dtos) {
        return dtos.stream()
            .map(this::convertToInstallationHistory)
            .toList();
    }

    /**
     * DeviceInstallmentDetailDto를 DeviceInstallmentDetail 도메인 객체로 변환
     */
    public DeviceInstallmentDetail convertToDeviceInstallmentDetail(DeviceInstallmentDetailDto dto) {
        return new DeviceInstallmentDetail(
            dto.installmentRound(),
            dto.installmentAmount()
        );
    }
    
    /**
     * DeviceInstallmentDetail 도메인 객체를 DeviceInstallmentDetailDto로 변환
     */
    public DeviceInstallmentDetailDto convertToDeviceInstallmentDetailDto(DeviceInstallmentDetail domain) {
        return new DeviceInstallmentDetailDto(
            domain.installmentRound(),
            domain.installmentAmount(),
            null // billingCompletedDate는 domain에 없으므로 null
        );
    }
    
    /**
     * DeviceInstallmentDto를 DeviceInstallmentMaster 도메인 객체로 변환
     */
    public DeviceInstallmentMaster convertToDeviceInstallmentMaster(DeviceInstallmentDto dto) {
        List<DeviceInstallmentDetail> details = dto.details() != null ?
            dto.details().stream()
                .map(this::convertToDeviceInstallmentDetail)
                .toList() : List.of();
                
        return new DeviceInstallmentMaster(
            dto.contractId(),
            dto.installmentSequence(),
            dto.installmentStartDate(),
            dto.totalInstallmentAmount(),
            dto.installmentMonths(),
            dto.billedCount(),
            details
        );
    }
    
    /**
     * DeviceInstallmentMaster 도메인 객체를 DeviceInstallmentDto로 변환
     */
    public DeviceInstallmentDto convertToDeviceInstallmentDto(DeviceInstallmentMaster domain) {
        List<DeviceInstallmentDetailDto> detailDtos = domain.deviceInstallmentDetailList().stream()
            .map(this::convertToDeviceInstallmentDetailDto)
            .toList();
            
        return new DeviceInstallmentDto(
            domain.contractId(),
            domain.installmentSequence(),
            domain.installmentStartDate(),
            domain.totalInstallmentAmount(),
            domain.installmentMonths(),
            domain.billedCount(),
            detailDtos
        );
    }

    
    /**
     * DeviceInstallmentDto 리스트를 DeviceInstallmentMaster 도메인 객체 리스트로 변환
     */
    public List<DeviceInstallmentMaster> convertToDeviceInstallmentMasters(List<DeviceInstallmentDto> dtos) {
        return dtos.stream()
            .map(this::convertToDeviceInstallmentMaster)
            .toList();
    }
}