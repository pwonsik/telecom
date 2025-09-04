package me.realimpact.telecom.calculation.infrastructure.adapter;

import lombok.RequiredArgsConstructor;
import me.realimpact.telecom.calculation.domain.monthlyfee.ContractWithProductsAndSuspensions;
import me.realimpact.telecom.calculation.infrastructure.adapter.mybatis.ProductQueryMapper;
import me.realimpact.telecom.calculation.infrastructure.converter.ContractDtoToDomainConverter;
import me.realimpact.telecom.calculation.infrastructure.dto.ContractProductsSuspensionsDto;
import me.realimpact.telecom.calculation.port.out.ProductQueryPort;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;

@Repository
@RequiredArgsConstructor
@Qualifier("default_product_query_repository")
public class ProductQueryRepository implements ProductQueryPort {
    private final ProductQueryMapper productQueryMapper;
    private final ContractDtoToDomainConverter converter;

    @Override
    public List<ContractWithProductsAndSuspensions> findContractsAndProductInventoriesByContractIds(
        List<Long> contractIds, LocalDate billingStartDate, LocalDate billingEndDate
    ) {
        List<ContractProductsSuspensionsDto> contractProductsSuspensionsDtos = productQueryMapper.findContractsAndProductInventoriesByContractIds(contractIds, billingStartDate, billingEndDate);
        return converter.convertToContracts(contractProductsSuspensionsDtos);
    }
}
