package me.realimpact.telecom.calculation.infrastructure.converter;

import me.realimpact.telecom.calculation.domain.monthlyfee.*;
import me.realimpact.telecom.calculation.infrastructure.dto.*;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Component
public class DtoToDomainConverter {

    public Contract convertToContract(ContractDto dto) {
        // Products 변환
        List<Product> products = dto.getProducts() != null ? 
            convertToProducts(dto.getProducts()) : List.of();
            
        // Suspensions 변환
        List<Suspension> suspensions = dto.getSuspensions() != null ?
            convertToSuspensions(dto.getSuspensions()) : List.of();
        
        return new Contract(
            dto.getContractId(),
            dto.getSubscribedAt(),
            dto.getInitiallySubscribedAt(),
            Optional.ofNullable(dto.getTerminatedAt()),
            Optional.ofNullable(dto.getPrefferedTerminationDate()),
            products,
            suspensions
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
        List<MonthlyChargeItem> monthlyChargeItems = dto.getMonthlyChargeItems() != null ? 
            dto.getMonthlyChargeItems().stream()
                .map(this::convertToMonthlyChargeItem)
                .collect(Collectors.toList()) : 
            List.of();
                
        return new ProductOffering(
            dto.getProductOfferingId(),
            dto.getProductOfferingName(),
            monthlyChargeItems
        );
    }

    private MonthlyChargeItem convertToMonthlyChargeItem(MonthlyChargeItemDto dto) {
        CalculationMethod calculationMethod = getCalculationMethodFromCode(dto.getCalculationMethodCode());
        Pricing pricing = createPricing(dto);
        
        return new MonthlyChargeItem(
            dto.getChargeItemId(),
            dto.getChargeItemName(),
            dto.getSuspensionChargeRatio(),
            calculationMethod,
            pricing
        );
    }

    private CalculationMethod getCalculationMethodFromCode(String code) {
        return Arrays.stream(CalculationMethod.values())
                .filter(method -> method.name().equals(getCalculationMethodEnumName(code)))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Unknown calculation method code: " + code));
    }

    private String getCalculationMethodEnumName(String code) {
        return switch (code) {
            case "00" -> "FLAT_RATE";
            case "01" -> "MATCHING_FACTOR";
            case "02" -> "RANGE_FACTOR";
            case "03" -> "UNIT_PRICE_FACTOR";
            case "04" -> "STEP_FACTOR";
            case "05" -> "TIER_FACTOR";
            default -> throw new IllegalArgumentException("Unknown calculation method code: " + code);
        };
    }

    private Pricing createPricing(MonthlyChargeItemDto dto) {
        // TODO: 실제 Pricing 구현체 생성 로직
        // DTO의 pricingType에 따라 적절한 Pricing 구현체를 생성해야 함
        // 예: FlatRatePolicy, MatchingFactorPolicy, RangeFactorPolicy 등
        // 
        // 현재는 임시로 null을 반환하지만, 실제로는 MonthlyChargingPolicyFactory를 사용하여
        // 적절한 Pricing 구현체를 생성해야 함
        return null;
    }

    public List<Suspension> convertToSuspensions(List<SuspensionDto> dtos) {
        return dtos.stream()
                .map(this::convertToSuspension)
                .collect(Collectors.toList());
    }

    public Suspension convertToSuspension(SuspensionDto dto) {
        Suspension.SuspensionType suspensionType = convertToSuspensionType(dto.getSuspensionTypeCode());
        
        return new Suspension(
            dto.getEffectiveStartDateTime(),
            dto.getEffectiveEndDateTime(),
            suspensionType
        );
    }

    private Suspension.SuspensionType convertToSuspensionType(String code) {
        return Arrays.stream(Suspension.SuspensionType.values())
                .filter(type -> type.name().equals(getEnumNameFromCode(code)))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Unknown suspension type code: " + code));
    }

    private String getEnumNameFromCode(String code) {
        return switch (code) {
            case "F1" -> "TEMPORARY_SUSPENSION";
            case "F3" -> "NON_PAYMENT_SUSPENSION";
            default -> throw new IllegalArgumentException("Unknown suspension type code: " + code);
        };
    }
}
