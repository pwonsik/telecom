package me.realimpact.telecom.calculation.infrastructure.adapter;

import lombok.RequiredArgsConstructor;
import me.realimpact.telecom.calculation.domain.monthlyfee.Contract;
import me.realimpact.telecom.calculation.domain.monthlyfee.ExclusiveLineContractHistory;
import me.realimpact.telecom.calculation.infrastructure.converter.DtoToDomainConverter;
import me.realimpact.telecom.calculation.infrastructure.dto.ContractDto;
import me.realimpact.telecom.calculation.port.out.ContractQueryPort;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;

@Repository
@RequiredArgsConstructor
public class ContractQueryRepository implements ContractQueryPort {
    private final ContractQueryMapper contractQueryMapper;
    private final DtoToDomainConverter converter;

    @Override
    public List<Contract> findContractWithProductsChargeItemsAndSuspensions(Long contractId, LocalDate billingStartDate, LocalDate billingEndDate) {
        List<ContractDto> contractDtos = contractQueryMapper.findContractWithProductsChargeItemsAndSuspensions(contractId, billingStartDate, billingEndDate);

        // DTO를 도메인 객체로 변환 (products와 suspensions 포함)
        return converter.convertToContracts(contractDtos);
    }

    /**
     * 전체 또는 조건부 계약 조회 (Spring Batch용)
     * contractId가 null이면 전체 조회, 있으면 해당 계약만 조회
     */
//    @Override
//    public Contract findContractsWithProductsChargeItemsAndSuspensions(LocalDate billingStartDate, LocalDate billingEndDate) {
//        ContractDto contractDto = contractQueryMapper.findContractsWithProductsChargeItemsAndSuspensions(billingStartDate, billingEndDate);
//        if (contractDto == null) {
//            return null;
//        }
//
//        // DTO를 도메인 객체로 변환 (products와 suspensions 포함)
//        return converter.convertToContract(contractDto);
//    }

    @Override
    public List<ExclusiveLineContractHistory> findExclusiveLineContractHistory(Long contractId) {
        // TODO: ExclusiveLineContractHistory 조회 로직 구현 필요
        return List.of();
    }
}
