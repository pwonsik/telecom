package me.realimpact.telecom.calculation.infrastructure.converter;

import me.realimpact.telecom.calculation.domain.monthlyfee.ContractWithProductsAndSuspensions;
import me.realimpact.telecom.calculation.domain.monthlyfee.Product;
import me.realimpact.telecom.calculation.domain.monthlyfee.Suspension;
import me.realimpact.telecom.calculation.infrastructure.dto.ContractProductsSuspensionsDto;
import me.realimpact.telecom.calculation.infrastructure.dto.ChargeItemDto;
import me.realimpact.telecom.calculation.infrastructure.dto.ProductDto;
import me.realimpact.telecom.calculation.infrastructure.dto.SuspensionDto;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class ContractProductsSuspensionsDtoToDomainConverterTest {

    private ContractDtoToDomainConverter converter;

    @BeforeEach
    void setUp() {
        converter = new ContractDtoToDomainConverter();
    }

    @Test
    void convertToContract_중첩구조포함_성공() {
        // Given - Contract DTO with nested products and suspensions
        ChargeItemDto chargeItemDto = new ChargeItemDto();
        chargeItemDto.setProductOfferingId("PO001");
        chargeItemDto.setChargeItemId("CI001");
        chargeItemDto.setChargeItemName("기본요금");
        chargeItemDto.setRevenueItemId("REVENUE_001");
        chargeItemDto.setSuspensionChargeRatio(BigDecimal.valueOf(0.5));
        chargeItemDto.setCalculationMethodCode("FLAT");
        chargeItemDto.setCalculationMethodName("일반적인 정액 요율");
        chargeItemDto.setFlatRateAmount(BigDecimal.valueOf(10000));
        chargeItemDto.setPricingType("FLAT_RATE");

        ProductDto productDto = new ProductDto();
        productDto.setContractId(12345L);
        productDto.setProductOfferingId("PO001");
        productDto.setEffectiveStartDateTime(LocalDateTime.of(2024, 1, 1, 0, 0, 0));
        productDto.setEffectiveEndDateTime(LocalDateTime.of(2024, 12, 31, 23, 59, 59));
        productDto.setSubscribedAt(LocalDate.of(2024, 1, 1));
        productDto.setActivatedAt(LocalDate.of(2024, 1, 1));
        productDto.setTerminatedAt(LocalDate.of(2024, 12, 31));
        productDto.setProductOfferingName("기본상품");
        productDto.setChargeItems(List.of(chargeItemDto));

        SuspensionDto suspensionDto = new SuspensionDto();
        suspensionDto.setContractId(12345L);
        suspensionDto.setSuspensionTypeCode("F1");
        suspensionDto.setEffectiveStartDateTime(LocalDateTime.of(2024, 6, 1, 0, 0, 0));
        suspensionDto.setEffectiveEndDateTime(LocalDateTime.of(2024, 6, 30, 23, 59, 59));
        suspensionDto.setSuspensionTypeDescription("일시정지");

        ContractProductsSuspensionsDto dto = new ContractProductsSuspensionsDto();
        dto.setContractId(12345L);
        dto.setSubscribedAt(LocalDate.of(2024, 1, 1));
        dto.setInitiallySubscribedAt(LocalDate.of(2023, 12, 15));
        dto.setTerminatedAt(LocalDate.of(2024, 12, 31));
        dto.setPrefferedTerminationDate(LocalDate.of(2024, 12, 30));
        dto.setBillingStartDate(LocalDate.of(2024, 1, 1));
        dto.setBillingEndDate(LocalDate.of(2024, 1, 31));
        dto.setProducts(List.of(productDto));
        dto.setSuspensions(List.of(suspensionDto));

        // When
        ContractWithProductsAndSuspensions contractWithProductsAndSuspensions = converter.convertToContract(dto);

        // Then - Contract 기본 정보
        assertThat(contractWithProductsAndSuspensions.getContractId()).isEqualTo(12345L);
        assertThat(contractWithProductsAndSuspensions.getSubscribedAt()).isEqualTo(LocalDate.of(2024, 1, 1));
        assertThat(contractWithProductsAndSuspensions.getInitiallySubscribedAt()).isEqualTo(LocalDate.of(2023, 12, 15));
        assertThat(contractWithProductsAndSuspensions.getTerminatedAt()).contains(LocalDate.of(2024, 12, 31));
        assertThat(contractWithProductsAndSuspensions.getPrefferedTerminationDate()).contains(LocalDate.of(2024, 12, 30));
        
        // Then - Products 포함
        assertThat(contractWithProductsAndSuspensions.getProducts()).hasSize(1);
        assertThat(contractWithProductsAndSuspensions.getProducts().get(0).getProductOffering().getProductOfferingId()).isEqualTo("PO001");
        assertThat(contractWithProductsAndSuspensions.getProducts().get(0).getProductOffering().getProductOfferingName()).isEqualTo("기본상품");
        
        // Then - Suspensions 포함
        assertThat(contractWithProductsAndSuspensions.getSuspensions()).hasSize(1);
        assertThat(contractWithProductsAndSuspensions.getSuspensions().get(0).getSuspensionType()).isEqualTo(Suspension.SuspensionType.TEMPORARY_SUSPENSION);
    }

    @Test
    void convertToContract_빈_Products_Suspensions_처리() {
        // Given - Contract DTO without products and suspensions
        ContractProductsSuspensionsDto dto = new ContractProductsSuspensionsDto();
        dto.setContractId(12345L);
        dto.setSubscribedAt(LocalDate.of(2024, 1, 1));
        dto.setInitiallySubscribedAt(LocalDate.of(2023, 12, 15));
        dto.setTerminatedAt(null); // null 테스트
        dto.setPrefferedTerminationDate(null); // null 테스트
        dto.setBillingStartDate(LocalDate.of(2024, 1, 1));
        dto.setBillingEndDate(LocalDate.of(2024, 1, 31));
        dto.setProducts(null); // null 테스트
        dto.setSuspensions(null); // null 테스트

        // When
        ContractWithProductsAndSuspensions contractWithProductsAndSuspensions = converter.convertToContract(dto);

        // Then
        assertThat(contractWithProductsAndSuspensions.getContractId()).isEqualTo(12345L);
        assertThat(contractWithProductsAndSuspensions.getSubscribedAt()).isEqualTo(LocalDate.of(2024, 1, 1));
        assertThat(contractWithProductsAndSuspensions.getInitiallySubscribedAt()).isEqualTo(LocalDate.of(2023, 12, 15));
        assertThat(contractWithProductsAndSuspensions.getTerminatedAt()).isEmpty();
        assertThat(contractWithProductsAndSuspensions.getPrefferedTerminationDate()).isEmpty();
        assertThat(contractWithProductsAndSuspensions.getProducts()).isEmpty();
        assertThat(contractWithProductsAndSuspensions.getSuspensions()).isEmpty();
    }

    @Test
    void convertToProduct_상품정보변환_성공() {
        // Given
        ChargeItemDto chargeItemDto = new ChargeItemDto();
        chargeItemDto.setProductOfferingId("PO001");
        chargeItemDto.setChargeItemId("CI001");
        chargeItemDto.setChargeItemName("기본요금");
        chargeItemDto.setSuspensionChargeRatio(BigDecimal.valueOf(0.5));
        chargeItemDto.setCalculationMethodCode("FLAT");
        chargeItemDto.setCalculationMethodName("일반적인 정액 요율");
        chargeItemDto.setFlatRateAmount(BigDecimal.valueOf(10000));
        chargeItemDto.setPricingType("FLAT_RATE");

        ProductDto dto = new ProductDto();
        dto.setContractId(12345L);
        dto.setProductOfferingId("PO001");
        dto.setEffectiveStartDateTime(LocalDateTime.of(2024, 1, 1, 0, 0, 0));
        dto.setEffectiveEndDateTime(LocalDateTime.of(2024, 12, 31, 23, 59, 59));
        dto.setSubscribedAt(LocalDate.of(2024, 1, 1));
        dto.setActivatedAt(LocalDate.of(2024, 1, 1));
        dto.setTerminatedAt(LocalDate.of(2024, 12, 31));
        dto.setProductOfferingName("기본상품");
        dto.setChargeItems(List.of(chargeItemDto));

        // When
        Product product = converter.convertToProduct(dto);

        // Then
        assertThat(product.getProductOffering().getProductOfferingId()).isEqualTo("PO001");
        assertThat(product.getProductOffering().getProductOfferingName()).isEqualTo("기본상품");
        assertThat(product.getProductOffering().getChargeItems()).hasSize(1);
        assertThat(product.getProductOffering().getChargeItems().get(0).getChargeItemId()).isEqualTo("CI001");
    }

    @Test
    void convertToSuspension_정지정보변환_성공() {
        // Given
        SuspensionDto dto = new SuspensionDto();
        dto.setContractId(12345L);
        dto.setSuspensionTypeCode("F1");
        dto.setEffectiveStartDateTime(LocalDateTime.of(2024, 6, 1, 0, 0, 0));
        dto.setEffectiveEndDateTime(LocalDateTime.of(2024, 6, 30, 23, 59, 59));
        dto.setSuspensionTypeDescription("일시정지");

        // When
        Suspension suspension = converter.convertToSuspension(dto);

        // Then
        assertThat(suspension.getSuspensionType()).isEqualTo(Suspension.SuspensionType.TEMPORARY_SUSPENSION);
        assertThat(suspension.getStartDate()).isEqualTo(LocalDate.of(2024, 6, 1));
        assertThat(suspension.getEndDate()).isEqualTo(LocalDate.of(2024, 6, 30));
    }

    @Test
    void convertToSuspensions_복수정지정보변환_성공() {
        // Given
        SuspensionDto dto1 = new SuspensionDto();
        dto1.setContractId(12345L);
        dto1.setSuspensionTypeCode("F1");
        dto1.setEffectiveStartDateTime(LocalDateTime.of(2024, 6, 1, 0, 0, 0));
        dto1.setEffectiveEndDateTime(LocalDateTime.of(2024, 6, 30, 23, 59, 59));
        dto1.setSuspensionTypeDescription("일시정지");

        SuspensionDto dto2 = new SuspensionDto();
        dto2.setContractId(12345L);
        dto2.setSuspensionTypeCode("F3");
        dto2.setEffectiveStartDateTime(LocalDateTime.of(2024, 8, 1, 0, 0, 0));
        dto2.setEffectiveEndDateTime(LocalDateTime.of(2024, 8, 15, 23, 59, 59));
        dto2.setSuspensionTypeDescription("미납정지");

        List<SuspensionDto> dtos = Arrays.asList(dto1, dto2);

        // When
        List<Suspension> suspensions = converter.convertToSuspensions(dtos);

        // Then
        assertThat(suspensions).hasSize(2);
        assertThat(suspensions.get(0).getSuspensionType()).isEqualTo(Suspension.SuspensionType.TEMPORARY_SUSPENSION);
        assertThat(suspensions.get(1).getSuspensionType()).isEqualTo(Suspension.SuspensionType.NON_PAYMENT_SUSPENSION);
    }

    @Test
    void convertToContract_통합테스트_전체흐름검증() {
        // Given - 완전한 ContractDto with nested structures
        ChargeItemDto chargeItem1 = new ChargeItemDto();
        chargeItem1.setProductOfferingId("PO001");
        chargeItem1.setChargeItemId("CI001");
        chargeItem1.setChargeItemName("기본요금");
        chargeItem1.setSuspensionChargeRatio(BigDecimal.valueOf(0.5));
        chargeItem1.setCalculationMethodCode("FLAT");

        ChargeItemDto chargeItem2 = new ChargeItemDto();
        chargeItem2.setProductOfferingId("PO001");
        chargeItem2.setChargeItemId("CI002");
        chargeItem2.setChargeItemName("부가요금");
        chargeItem2.setSuspensionChargeRatio(BigDecimal.valueOf(1.0));
        chargeItem2.setCalculationMethodCode("MATCHING");
        
        ProductDto product1 = new ProductDto();
        product1.setContractId(12345L);
        product1.setProductOfferingId("PO001");
        product1.setEffectiveStartDateTime(LocalDateTime.of(2024, 1, 1, 0, 0, 0));
        product1.setEffectiveEndDateTime(LocalDateTime.of(2024, 6, 30, 23, 59, 59));
        product1.setSubscribedAt(LocalDate.of(2024, 1, 1));
        product1.setProductOfferingName("기본상품");
        product1.setChargeItems(Arrays.asList(chargeItem1, chargeItem2));
        
        ProductDto product2 = new ProductDto();
        product2.setContractId(12345L);
        product2.setProductOfferingId("PO002");
        product2.setEffectiveStartDateTime(LocalDateTime.of(2024, 7, 1, 0, 0, 0));
        product2.setEffectiveEndDateTime(LocalDateTime.of(2024, 12, 31, 23, 59, 59));
        product2.setSubscribedAt(LocalDate.of(2024, 7, 1));
        product2.setProductOfferingName("프리미엄상품");
        product2.setChargeItems(List.of(chargeItem1));
        
        SuspensionDto suspension1 = new SuspensionDto();
        suspension1.setContractId(12345L);
        suspension1.setSuspensionTypeCode("F1");
        suspension1.setEffectiveStartDateTime(LocalDateTime.of(2024, 3, 1, 0, 0, 0));
        suspension1.setEffectiveEndDateTime(LocalDateTime.of(2024, 3, 15, 23, 59, 59));
        
        SuspensionDto suspension2 = new SuspensionDto();
        suspension2.setContractId(12345L);
        suspension2.setSuspensionTypeCode("F3");
        suspension2.setEffectiveStartDateTime(LocalDateTime.of(2024, 9, 1, 0, 0, 0));
        suspension2.setEffectiveEndDateTime(LocalDateTime.of(2024, 9, 10, 23, 59, 59));
        
        ContractProductsSuspensionsDto contractProductsSuspensionsDto = new ContractProductsSuspensionsDto();
        contractProductsSuspensionsDto.setContractId(12345L);
        contractProductsSuspensionsDto.setSubscribedAt(LocalDate.of(2024, 1, 1));
        contractProductsSuspensionsDto.setInitiallySubscribedAt(LocalDate.of(2023, 12, 15));
        contractProductsSuspensionsDto.setTerminatedAt(LocalDate.of(2024, 12, 31));
        contractProductsSuspensionsDto.setBillingStartDate(LocalDate.of(2024, 1, 1));
        contractProductsSuspensionsDto.setBillingEndDate(LocalDate.of(2024, 1, 31));
        contractProductsSuspensionsDto.setProducts(Arrays.asList(product1, product2));
        contractProductsSuspensionsDto.setSuspensions(Arrays.asList(suspension1, suspension2));
        
        // When
        ContractWithProductsAndSuspensions contractWithProductsAndSuspensions = converter.convertToContract(contractProductsSuspensionsDto);
        
        // Then - Contract 기본 정보
        assertThat(contractWithProductsAndSuspensions.getContractId()).isEqualTo(12345L);
        
        // Then - Products 검증
        assertThat(contractWithProductsAndSuspensions.getProducts()).hasSize(2);
        assertThat(contractWithProductsAndSuspensions.getProducts().get(0).getProductOffering().getProductOfferingId()).isEqualTo("PO001");
        assertThat(contractWithProductsAndSuspensions.getProducts().get(0).getProductOffering().getChargeItems()).hasSize(2);
        assertThat(contractWithProductsAndSuspensions.getProducts().get(1).getProductOffering().getProductOfferingId()).isEqualTo("PO002");
        assertThat(contractWithProductsAndSuspensions.getProducts().get(1).getProductOffering().getChargeItems()).hasSize(1);
        
        // Then - Suspensions 검증
        assertThat(contractWithProductsAndSuspensions.getSuspensions()).hasSize(2);
        assertThat(contractWithProductsAndSuspensions.getSuspensions().get(0).getSuspensionType()).isEqualTo(Suspension.SuspensionType.TEMPORARY_SUSPENSION);
        assertThat(contractWithProductsAndSuspensions.getSuspensions().get(1).getSuspensionType()).isEqualTo(Suspension.SuspensionType.NON_PAYMENT_SUSPENSION);
    }
}
