package me.realimpact.telecom.calculation.infrastructure.converter;

import me.realimpact.telecom.calculation.domain.monthlyfee.*;
import me.realimpact.telecom.calculation.domain.monthlyfee.policy.FlatRatePolicy;
import me.realimpact.telecom.calculation.infrastructure.dto.ChargeItemDto;
import me.realimpact.telecom.calculation.infrastructure.dto.ContractProductsSuspensionsDto;
import me.realimpact.telecom.calculation.infrastructure.dto.ProductDto;
import me.realimpact.telecom.calculation.infrastructure.dto.SuspensionDto;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;

@Component
public class ContractDtoToDomainConverter {
    public List<ContractWithProductsAndSuspensions> convertToContracts(List<ContractProductsSuspensionsDto> dto) {
        return dto.stream()
            .map(this::convertToContract)
            .toList();
    }

    public ContractWithProductsAndSuspensions convertToContract(ContractProductsSuspensionsDto dto) {
        // Products 변환
        List<Product> products = dto.getProducts() != null ? 
            convertToProducts(dto.getProducts()) : List.of();
            
        // Suspensions 변환
        List<Suspension> suspensions = dto.getSuspensions() != null ?
            convertToSuspensions(dto.getSuspensions()) : List.of();
        
        return new ContractWithProductsAndSuspensions(
            dto.getContractId(),
            dto.getSubscribedAt(),
            dto.getInitiallySubscribedAt(),
            Optional.ofNullable(dto.getTerminatedAt()),
            Optional.ofNullable(dto.getPrefferedTerminationDate()),
            dto.getBillingStartDate(),
            dto.getBillingEndDate(),
            products,
            suspensions,
            List.of()
        );
    }

    public List<Product> convertToProducts(List<ProductDto> dtos) {
        return dtos.stream()
                .map(this::convertToProduct)
                .collect(Collectors.toList());
    }

    public Product convertToProduct(ProductDto dto) {
        ProductOffering productOffering = convertToProductOffering(dto);
        
        return new Product(
            dto.getContractId(),
            productOffering,
            dto.getEffectiveStartDateTime(),
            dto.getEffectiveEndDateTime(),
            dto.getSubscribedAt(),
            Optional.ofNullable(dto.getActivatedAt()),
            Optional.ofNullable(dto.getTerminatedAt())
        );
    }

    private ProductOffering convertToProductOffering(ProductDto dto) {
        List<ChargeItem> chargeItems = dto.getChargeItems() != null ? 
            dto.getChargeItems().stream()
                .map(this::convertToChargeItem)
                .collect(Collectors.toList()) : 
            List.of();
                
        return new ProductOffering(
            dto.getProductOfferingId(),
            dto.getProductOfferingName(),
            chargeItems
        );
    }

    private ChargeItem convertToChargeItem(ChargeItemDto dto) {
        CalculationMethod calculationMethod = CalculationMethod.fromCode(dto.getCalculationMethodCode());
        Pricing pricing = createPricing(dto);
        
        return new ChargeItem(
            dto.getChargeItemId(),
            dto.getChargeItemName(),
            dto.getRevenueItemId(),
            dto.getSuspensionChargeRatio(),
            calculationMethod,
            pricing
        );
    }

    private Pricing createPricing(ChargeItemDto dto) {
        // 임시로 CalculationMethod에 따른 기본 Pricing 생성
        // 실제 구현에서는 MonthlyChargingPolicyFactory를 사용해야 함
//        return switch (dto.getCalculationMethodCode()) {
//            // flat
//            case "FLAT" -> new me.realimpact.telecom.calculation.domain.monthlyfee.policy.FlatRatePolicy(
//                    dto.getFlatRateAmount()
//            );
//            case "01" -> new me.realimpact.telecom.calculation.domain.monthlyfee.policy.MatchingFactorPolicy(
//                java.util.List.of() // 빈 매칭 규칙 리스트
//            );
//            case "02" -> new me.realimpact.telecom.calculation.domain.monthlyfee.policy.RangeFactorPolicy(
//                "default", // 임시 기본값
//                java.util.List.of() // 빈 범위 규칙 리스트
//            );
//            case "03" -> new me.realimpact.telecom.calculation.domain.monthlyfee.policy.UnitPriceFactorPolicy(
//                "count", // 임시 기본값
//                    dto.getFlatRateAmount()
//            );
//            case "04" -> new me.realimpact.telecom.calculation.domain.monthlyfee.policy.StepFactorPolicy(
//                "count", // 임시 기본값
//                java.util.List.of() // 빈 구간 규칙 리스트
//            );
//            case "05" -> new me.realimpact.telecom.calculation.domain.monthlyfee.policy.TierFactorPolicy(
//                "count", // 임시 기본값
//                java.util.List.of() // 빈 구간 규칙 리스트
//            );
//            default -> new me.realimpact.telecom.calculation.domain.monthlyfee.policy.FlatRatePolicy(
//                java.math.BigDecimal.ZERO // 알 수 없는 경우 0원으로 기본값 설정
//            );
//        };
        return new FlatRatePolicy(dto.getFlatRateAmount());
    }

    public List<Suspension> convertToSuspensions(List<SuspensionDto> dtos) {
        return dtos.stream()
                .map(this::convertToSuspension)
                .filter(Objects::nonNull)
                .collect(Collectors.toList());
    }

    public Suspension convertToSuspension(SuspensionDto dto) {
        if (dto.getSuspensionTypeCode() == null) {
            return null;
        }
        return new Suspension(
                dto.getEffectiveStartDateTime(),
                dto.getEffectiveEndDateTime(),
                Suspension.SuspensionType.fromCode(dto.getSuspensionTypeCode())
        );
    }

}
