package me.realimpact.telecom.testgen;

import com.github.javafaker.Faker;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.ibatis.session.SqlSession;
import org.apache.ibatis.session.SqlSessionFactory;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.*;

/**
 * Contract Discount 테스트 데이터 생성기
 * 기존 contract와 product 데이터를 기반으로 현실적인 할인 데이터 생성
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class ContractDiscountDataGenerator {

    private final SqlSessionFactory sqlSessionFactory;
    private final Faker faker = new Faker(Locale.KOREA);

    public void generateContractDiscounts() throws Exception {
        log.info("=== Contract Discount Data Generation Started ===");
        
        try (SqlSession session = sqlSessionFactory.openSession()) {
            // 기존 데이터 삭제
            int deleted = session.delete("deleteAllContractDiscounts");
            log.info("Deleted {} existing contract discount records", deleted);
            
            // 계약별 상품 정보 조회
            List<ContractProduct> contractProducts = session.selectList("selectContractProducts");
            log.info("Found {} contract-product combinations", contractProducts.size());
            
            if (contractProducts.isEmpty()) {
                log.warn("No contract-product combinations found. Cannot generate discounts.");
                return;
            }
            
            // 계약별로 그룹화
            Map<Long, List<ContractProduct>> contractProductMap = new HashMap<>();
            for (ContractProduct cp : contractProducts) {
                contractProductMap.computeIfAbsent(cp.contractId, k -> new ArrayList<>()).add(cp);
            }
            
            log.info("Contracts with products: {}", contractProductMap.size());
            if (log.isDebugEnabled()) {
                contractProductMap.forEach((contractId, products) -> 
                    log.debug("Contract {} has {} products: {}", 
                        contractId, products.size(), 
                        products.stream().map(p -> p.productOfferingId).toList()));
            }
            
            List<ContractDiscountData> discounts = new ArrayList<>();
            
            // 각 계약에 대해 할인 데이터 생성
            for (Map.Entry<Long, List<ContractProduct>> entry : contractProductMap.entrySet()) {
                Long contractId = entry.getKey();
                List<ContractProduct> products = entry.getValue();
                
                // 계약별 할인 생성 (30% 확률로 할인 없음, 70% 확률로 1-3개 할인)
                if (faker.random().nextDouble() < 0.3) {
                    continue; // 할인 없음
                }
                
                int discountCount = faker.random().nextInt(1, 4); // 1-3개 할인
                Set<String> usedDiscountIds = new HashSet<>();
                
                for (int i = 0; i < discountCount; i++) {
                    ContractDiscountData discount = generateDiscount(contractId, products, usedDiscountIds);
                    if (discount != null) {
                        discounts.add(discount);
                    }
                }
            }
            
            // Bulk 삽입
            if (!discounts.isEmpty()) {
                session.insert("insertContractDiscounts", discounts);
                session.commit();
                log.info("Generated {} contract discount records", discounts.size());
            }
            
        } catch (Exception e) {
            log.error("Failed to generate contract discount data", e);
            throw e;
        }
        
        log.info("=== Contract Discount Data Generation Completed ===");
    }
    
    /**
     * 개별 할인 데이터 생성
     */
    private ContractDiscountData generateDiscount(Long contractId, List<ContractProduct> products, Set<String> usedDiscountIds) {
        // 할인 ID 생성 (중복 방지)
        String discountId;
        int attempts = 0;
        do {
            discountId = generateDiscountId();
            attempts++;
        } while (usedDiscountIds.contains(discountId) && attempts < 10);
        
        if (attempts >= 10) {
            return null; // 할인 ID 생성 실패
        }
        usedDiscountIds.add(discountId);
        
        // 할인 기간 생성
        DiscountPeriod period = generateDiscountPeriod();
        
        // 할인 대상 상품 선택 (항상 특정 상품 선택)
        if (products.isEmpty()) {
            return null; // 상품이 없으면 할인 생성 불가
        }
        
        ContractProduct selectedProduct = products.get(faker.random().nextInt(products.size()));
        String productOfferingId = selectedProduct.productOfferingId;
        
        // 할인 유형 및 값 생성
        DiscountValue discountValue = generateDiscountValue();
        
        ContractDiscountData discount = new ContractDiscountData();
        discount.contractId = contractId;
        discount.discountId = discountId;
        discount.discountStartDate = period.startDate;
        discount.discountEndDate = period.endDate;
        discount.productOfferingId = productOfferingId;
        discount.discountAplyUnit = discountValue.unit;
        discount.discountAmt = discountValue.amount;
        discount.discountRate = discountValue.rate;
        discount.discountAppliedAmount = BigDecimal.ZERO; // 초기값
        
        return discount;
    }
    
    /**
     * 할인 ID 생성
     */
    private String generateDiscountId() {
        String[] prefixes = {"DISC", "PROMO", "EVENT", "SPECIAL", "LOYALTY"};
        String prefix = prefixes[faker.random().nextInt(prefixes.length)];
        int number = faker.random().nextInt(1, 1000);
        return String.format("%s%03d", prefix, number);
    }
    
    /**
     * 할인 기간 생성
     */
    private DiscountPeriod generateDiscountPeriod() {
        LocalDate baseDate = LocalDate.of(2024, 1, 1);
        
        // 할인 시작일 (2024년 전체에서 랜덤)
        LocalDate startDate = baseDate.plusDays(faker.random().nextInt(365));
        
        // 할인 기간 패턴 선택
        LocalDate endDate;
        double periodType = faker.random().nextDouble();
        
        if (periodType < 0.3) {
            // 단기 할인 (1-2개월)
            endDate = startDate.plusMonths(faker.random().nextInt(1, 3));
        } else if (periodType < 0.7) {
            // 중기 할인 (3-6개월)
            endDate = startDate.plusMonths(faker.random().nextInt(3, 7));
        } else {
            // 장기 할인 (6-12개월)
            endDate = startDate.plusMonths(faker.random().nextInt(6, 13));
        }
        
        return new DiscountPeriod(startDate, endDate);
    }
    
    /**
     * 할인 값 생성
     */
    private DiscountValue generateDiscountValue() {
        boolean isAmount = faker.random().nextBoolean();
        
        if (isAmount) {
            // 금액 할인: 1000원 단위 (1000 ~ 50000)
            int amountValue = faker.random().nextInt(1, 51) * 1000;
            return new DiscountValue("AMOUNT", BigDecimal.valueOf(amountValue), null);
        } else {
            // 비율 할인: 10% 단위 (10% ~ 100%)
            int rateValue = faker.random().nextInt(1, 11) * 10;
            return new DiscountValue("RATE", null, BigDecimal.valueOf(rateValue));
        }
    }
    
    // 내부 데이터 클래스들
    public static class ContractProduct {
        public Long contractId;
        public String productOfferingId;
    }
    
    public static class ContractDiscountData {
        public Long contractId;
        public String discountId;
        public LocalDate discountStartDate;
        public LocalDate discountEndDate;
        public String productOfferingId;
        public String discountAplyUnit;
        public BigDecimal discountAmt;
        public BigDecimal discountRate;
        public BigDecimal discountAppliedAmount;
    }
    
    private static class DiscountPeriod {
        final LocalDate startDate;
        final LocalDate endDate;
        
        DiscountPeriod(LocalDate startDate, LocalDate endDate) {
            this.startDate = startDate;
            this.endDate = endDate;
        }
    }
    
    private static class DiscountValue {
        final String unit;
        final BigDecimal amount;
        final BigDecimal rate;
        
        DiscountValue(String unit, BigDecimal amount, BigDecimal rate) {
            this.unit = unit;
            this.amount = amount;
            this.rate = rate;
        }
    }
}