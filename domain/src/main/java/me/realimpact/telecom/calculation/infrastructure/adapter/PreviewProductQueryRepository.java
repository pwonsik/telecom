package me.realimpact.telecom.calculation.infrastructure.adapter;

import lombok.RequiredArgsConstructor;
import me.realimpact.telecom.calculation.domain.monthlyfee.ContractWithProductsAndSuspensions;
import me.realimpact.telecom.calculation.infrastructure.adapter.mybatis.PreviewProductQueryMapper;
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
@Qualifier("preview_product_query_repository")
public class PreviewProductQueryRepository implements ProductQueryPort {
    private final PreviewProductQueryMapper previewProductQueryMapper;
    private final ContractDtoToDomainConverter converter;

    @Override
    public List<ContractWithProductsAndSuspensions> findContractsAndProductInventoriesByContractIds(
            List<Long> contractIds, LocalDate billingStartDate, LocalDate billingEndDate
    ) {
        List<ContractProductsSuspensionsDto> contractProductsSuspensionsDtos =
                previewProductQueryMapper.findContractsAndProductInventoriesByContractIds(contractIds, billingEndDate);
        return converter.convertToContracts(contractProductsSuspensionsDtos);
    }
}
