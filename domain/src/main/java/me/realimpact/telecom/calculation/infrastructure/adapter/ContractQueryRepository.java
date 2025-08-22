package me.realimpact.telecom.calculation.infrastructure.adapter;

import lombok.RequiredArgsConstructor;
import me.realimpact.telecom.calculation.domain.monthlyfee.ContractWithProductsAndSuspensions;
import me.realimpact.telecom.calculation.infrastructure.adapter.mybatis.ContractQueryMapper;
import me.realimpact.telecom.calculation.infrastructure.converter.ContractDtoToDomainConverter;
import me.realimpact.telecom.calculation.infrastructure.dto.ContractDto;
import me.realimpact.telecom.calculation.port.out.ContractQueryPort;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;

@Repository
@RequiredArgsConstructor
public class ContractQueryRepository implements ContractQueryPort {
    private final ContractQueryMapper contractQueryMapper;
    private final ContractDtoToDomainConverter converter;

    @Override
    public List<ContractWithProductsAndSuspensions> findContractsAndProductInventoriesByContractIds(List<Long> contractIds, LocalDate billingStartDate, LocalDate billingEndDate) {
        List<ContractDto> contractDtos = contractQueryMapper.findContractsAndProductInventoriesByContractIds(contractIds, billingStartDate, billingEndDate);
        return converter.convertToContracts(contractDtos);
    }
}
