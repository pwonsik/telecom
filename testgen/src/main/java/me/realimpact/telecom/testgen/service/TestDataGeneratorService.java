package me.realimpact.telecom.testgen.service;

import com.github.javafaker.Faker;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import me.realimpact.telecom.testgen.entity.ContractEntity;
import me.realimpact.telecom.testgen.entity.ProductEntity;
import me.realimpact.telecom.testgen.entity.SuspensionEntity;
import me.realimpact.telecom.testgen.mapper.TestDataMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;
import java.util.concurrent.TimeUnit;

@Slf4j
@Service
@RequiredArgsConstructor
public class TestDataGeneratorService {

    private final TestDataMapper testDataMapper;
    private final Faker faker = new Faker();
    private final Random random = new Random();
    
    // 배치 처리 사이즈 (MySQL max_allowed_packet 고려)
    private static final int BATCH_SIZE = 1000;
    
    // 정지 유형 설정
    private static final String[][] SUSPENSION_TYPES = {
        {"F1", "일시정지"},
        {"F3", "미납정지"}
    };

    @Transactional
    public void generateTestData(int contractCount) {
        log.info("테스트 데이터 생성 시작: {} 건의 계약", contractCount);
        
        // 1. 기존 테스트 데이터 초기화 (외래키 제약조건 해결)
        log.info("기존 테스트 데이터 초기화 중...");
        try {
            // 외래키 체크 비활성화
            testDataMapper.disableForeignKeyChecks();
            log.info("외래키 제약조건 비활성화");
            
            // 테이블 데이터 삭제 (순서 상관없이 가능)
            testDataMapper.truncateSuspensions();
            log.info("suspension 테이블 초기화 완료");
            
            testDataMapper.truncateProducts();
            log.info("product 테이블 초기화 완료");
            
            testDataMapper.truncateContracts();
            log.info("contract 테이블 초기화 완료");
            
            // 외래키 체크 재활성화
            testDataMapper.enableForeignKeyChecks();
            log.info("외래키 제약조건 재활성화");
            
            log.info("모든 테스트 데이터 테이블 초기화 완료");
        } catch (Exception e) {
            log.error("테이블 초기화 중 오류 발생", e);
            // 오류 발생 시 외래키 체크를 다시 활성화
            try {
                testDataMapper.enableForeignKeyChecks();
            } catch (Exception ex) {
                log.warn("외래키 체크 재활성화 실패", ex);
            }
            throw new RuntimeException("테이블 초기화 실패", e);
        }
        
        // 2. Product Offering ID 목록 조회
        List<String> productOfferingIds = testDataMapper.selectProductOfferingIds();
        log.info("사용 가능한 Product Offering: {}", productOfferingIds);
        
        // Product Offering이 없으면 오류 발생
        if (productOfferingIds == null || productOfferingIds.isEmpty()) {
            log.error("Product Offering 데이터가 없습니다. sample_data.sql을 먼저 실행해주세요.");
            throw new RuntimeException("Product Offering 데이터가 없습니다. 기본 샘플 데이터를 확인해주세요.");
        }
        
        // 3. Contract 생성 및 배치 삽입
        List<ContractEntity> contracts = generateContracts(contractCount);
        insertContractsBatch(contracts);
        log.info("Contract {} 건 삽입 완료", contracts.size());
        
        // 4. Product 생성 및 배치 삽입
        List<ProductEntity> allProducts = new ArrayList<>();
        for (ContractEntity contract : contracts) {
            List<ProductEntity> products = generateProducts(contract, productOfferingIds);
            allProducts.addAll(products);
        }
        insertProductsBatch(allProducts);
        log.info("Product {} 건 삽입 완료", allProducts.size());
        
        // 5. Suspension 생성 및 배치 삽입
        List<SuspensionEntity> allSuspensions = new ArrayList<>();
        for (ContractEntity contract : contracts) {
            List<SuspensionEntity> suspensions = generateSuspensions(contract);
            allSuspensions.addAll(suspensions);
        }
        if (!allSuspensions.isEmpty()) {
            insertSuspensionsBatch(allSuspensions);
            log.info("Suspension {} 건 삽입 완료", allSuspensions.size());
        }
        
        log.info("테스트 데이터 생성 완료");
    }
    
    private List<ContractEntity> generateContracts(int count) {
        List<ContractEntity> contracts = new ArrayList<>();
        long startContractId = System.currentTimeMillis() / 1000; // 유니크한 시작 ID
        
        for (int i = 0; i < count; i++) {
            LocalDate subscribedAt = faker.date().past(365, TimeUnit.DAYS).toInstant()
                .atZone(java.time.ZoneId.systemDefault()).toLocalDate();
            LocalDate initiallySubscribedAt = subscribedAt.minusDays(random.nextInt(30));
            
            // 30% 확률로 해지된 계약
            LocalDate terminatedAt = random.nextDouble() < 0.3 ? 
                subscribedAt.plusDays(random.nextInt(200) + 30) : null;
            
            ContractEntity contract = ContractEntity.builder()
                .contractId(startContractId + i)
                .subscribedAt(subscribedAt)
                .initiallySubscribedAt(initiallySubscribedAt)
                .terminatedAt(terminatedAt)
                .prefferedTerminationDate(terminatedAt != null ? terminatedAt.plusDays(random.nextInt(5)) : null)
                .build();
            
            contracts.add(contract);
        }
        
        return contracts;
    }
    
    private List<ProductEntity> generateProducts(ContractEntity contract, List<String> productOfferingIds) {
        List<ProductEntity> products = new ArrayList<>();
        
        // 상품 패턴 결정 (1: 단일상품, 2: 서로다른상품, 3: 동일상품, 4: 혼합)
        int pattern = random.nextInt(4) + 1;
        
        switch (pattern) {
            case 1: // Product 1건
                products.add(createSingleProduct(contract, productOfferingIds));
                break;
                
            case 2: // 서로 다른 product 2건 이상
                products.addAll(createDifferentProducts(contract, productOfferingIds));
                break;
                
            case 3: // 동일 product 2건 (시간 중첩 없음)
                products.addAll(createSameProducts(contract, productOfferingIds));
                break;
                
            case 4: // 2,3항 혼합
                products.addAll(createMixedProducts(contract, productOfferingIds));
                break;
        }
        
        return products;
    }
    
    private ProductEntity createSingleProduct(ContractEntity contract, List<String> productOfferingIds) {
        if (productOfferingIds.isEmpty()) {
            throw new RuntimeException("Product Offering 데이터가 없어서 상품을 생성할 수 없습니다.");
        }
        String productOfferingId = productOfferingIds.get(random.nextInt(productOfferingIds.size()));
        
        LocalDateTime startDateTime = contract.getSubscribedAt().atTime(LocalTime.of(random.nextInt(24), random.nextInt(60)));
        LocalDateTime endDateTime = contract.getTerminatedAt() != null ? 
            contract.getTerminatedAt().atTime(23, 59, 59) :
            LocalDateTime.of(2999, 12, 31, 23, 59, 59);
            
        return ProductEntity.builder()
            .contractId(contract.getContractId())
            .productOfferingId(productOfferingId)
            .effectiveStartDateTime(startDateTime)
            .effectiveEndDateTime(endDateTime)
            .subscribedAt(contract.getSubscribedAt())
            .activatedAt(contract.getSubscribedAt())
            .terminatedAt(contract.getTerminatedAt())
            .build();
    }
    
    private List<ProductEntity> createDifferentProducts(ContractEntity contract, List<String> productOfferingIds) {
        List<ProductEntity> products = new ArrayList<>();
        if (productOfferingIds.isEmpty()) {
            throw new RuntimeException("Product Offering 데이터가 없어서 상품을 생성할 수 없습니다.");
        }
        
        int productCount = random.nextInt(3) + 2; // 2~4개
        
        List<String> selectedIds = new ArrayList<>(productOfferingIds);
        Collections.shuffle(selectedIds);
        
        LocalDateTime currentStart = contract.getSubscribedAt().atTime(0, 0, 0);
        
        for (int i = 0; i < Math.min(productCount, selectedIds.size()); i++) {
            LocalDateTime endTime = (i == productCount - 1 && contract.getTerminatedAt() != null) ?
                contract.getTerminatedAt().atTime(23, 59, 59) :
                currentStart.plusDays(random.nextInt(90) + 30);
                
            if (endTime.isAfter(LocalDateTime.of(2999, 12, 31, 23, 59, 59))) {
                endTime = LocalDateTime.of(2999, 12, 31, 23, 59, 59);
            }
            
            ProductEntity product = ProductEntity.builder()
                .contractId(contract.getContractId())
                .productOfferingId(selectedIds.get(i))
                .effectiveStartDateTime(currentStart)
                .effectiveEndDateTime(endTime)
                .subscribedAt(currentStart.toLocalDate())
                .activatedAt(currentStart.toLocalDate())
                .terminatedAt(i == productCount - 1 ? contract.getTerminatedAt() : endTime.toLocalDate())
                .build();
                
            products.add(product);
            currentStart = endTime.plusSeconds(1);
        }
        
        return products;
    }
    
    private List<ProductEntity> createSameProducts(ContractEntity contract, List<String> productOfferingIds) {
        List<ProductEntity> products = new ArrayList<>();
        if (productOfferingIds.isEmpty()) {
            throw new RuntimeException("Product Offering 데이터가 없어서 상품을 생성할 수 없습니다.");
        }
        String productOfferingId = productOfferingIds.get(random.nextInt(productOfferingIds.size()));
        
        int productCount = random.nextInt(2) + 2; // 2~3개
        LocalDateTime currentStart = contract.getSubscribedAt().atTime(0, 0, 0);
        
        for (int i = 0; i < productCount; i++) {
            LocalDateTime endTime = (i == productCount - 1 && contract.getTerminatedAt() != null) ?
                contract.getTerminatedAt().atTime(23, 59, 59) :
                currentStart.plusDays(random.nextInt(60) + 30);
                
            if (endTime.isAfter(LocalDateTime.of(2999, 12, 31, 23, 59, 59))) {
                endTime = LocalDateTime.of(2999, 12, 31, 23, 59, 59);
            }
            
            ProductEntity product = ProductEntity.builder()
                .contractId(contract.getContractId())
                .productOfferingId(productOfferingId)
                .effectiveStartDateTime(currentStart)
                .effectiveEndDateTime(endTime)
                .subscribedAt(currentStart.toLocalDate())
                .activatedAt(currentStart.toLocalDate())
                .terminatedAt(i == productCount - 1 ? contract.getTerminatedAt() : endTime.toLocalDate())
                .build();
                
            products.add(product);
            currentStart = endTime.plusSeconds(1);
        }
        
        return products;
    }
    
    private List<ProductEntity> createMixedProducts(ContractEntity contract, List<String> productOfferingIds) {
        List<ProductEntity> products = new ArrayList<>();
        if (productOfferingIds.isEmpty()) {
            throw new RuntimeException("Product Offering 데이터가 없어서 상품을 생성할 수 없습니다.");
        }
        
        // 동일 상품 2건 + 서로 다른 상품 1건
        products.addAll(createSameProducts(contract, productOfferingIds));
        
        // 마지막 상품의 종료 시간 이후에 다른 상품 추가
        ProductEntity lastProduct = products.get(products.size() - 1);
        LocalDateTime nextStart = lastProduct.getEffectiveEndDateTime().plusSeconds(1);
        
        if (nextStart.isBefore(LocalDateTime.of(2999, 12, 31, 0, 0, 0))) {
            String differentProductId = productOfferingIds.stream()
                .filter(id -> !id.equals(lastProduct.getProductOfferingId()))
                .findFirst()
                .orElse(productOfferingIds.get(0));
                
            ProductEntity additionalProduct = ProductEntity.builder()
                .contractId(contract.getContractId())
                .productOfferingId(differentProductId)
                .effectiveStartDateTime(nextStart)
                .effectiveEndDateTime(contract.getTerminatedAt() != null ? 
                    contract.getTerminatedAt().atTime(23, 59, 59) :
                    LocalDateTime.of(2999, 12, 31, 23, 59, 59))
                .subscribedAt(nextStart.toLocalDate())
                .activatedAt(nextStart.toLocalDate())
                .terminatedAt(contract.getTerminatedAt())
                .build();
                
            products.add(additionalProduct);
        }
        
        return products;
    }
    
    private List<SuspensionEntity> generateSuspensions(ContractEntity contract) {
        List<SuspensionEntity> suspensions = new ArrayList<>();
        
        // 70% 확률로 정지 이력 없음
        if (random.nextDouble() < 0.7) {
            return suspensions;
        }
        
        int suspensionCount = random.nextInt(3) + 1; // 1~3건
        LocalDateTime contractStart = contract.getSubscribedAt().atTime(0, 0, 0);
        LocalDateTime contractEnd = contract.getTerminatedAt() != null ?
            contract.getTerminatedAt().atTime(23, 59, 59) :
            LocalDateTime.now();
            
        LocalDateTime currentTime = contractStart.plusDays(random.nextInt(30) + 1);
        
        for (int i = 0; i < suspensionCount && currentTime.isBefore(contractEnd.minusDays(1)); i++) {
            String[] suspensionType = SUSPENSION_TYPES[random.nextInt(SUSPENSION_TYPES.length)];
            
            LocalDateTime suspensionStart = currentTime;
            LocalDateTime suspensionEnd = suspensionStart.plusDays(random.nextInt(15) + 1);
            
            // 계약 종료일을 넘지 않도록 조정
            if (suspensionEnd.isAfter(contractEnd)) {
                suspensionEnd = contractEnd.minusSeconds(1);
            }
            
            SuspensionEntity suspension = SuspensionEntity.builder()
                .contractId(contract.getContractId())
                .suspensionTypeCode(suspensionType[0])
                .effectiveStartDateTime(suspensionStart)
                .effectiveEndDateTime(suspensionEnd)
                .suspensionTypeDescription(suspensionType[1])
                .build();
                
            suspensions.add(suspension);
            
            // 다음 정지는 최소 7일 후
            currentTime = suspensionEnd.plusDays(random.nextInt(30) + 7);
        }
        
        return suspensions;
    }
    
    // 배치 처리 메서드들
    private void insertContractsBatch(List<ContractEntity> contracts) {
        int totalBatches = (contracts.size() + BATCH_SIZE - 1) / BATCH_SIZE;
        log.info("Contract 배치 삽입 시작: {} 건 ({} 배치)", contracts.size(), totalBatches);
        
        for (int i = 0; i < contracts.size(); i += BATCH_SIZE) {
            int endIndex = Math.min(i + BATCH_SIZE, contracts.size());
            List<ContractEntity> batch = contracts.subList(i, endIndex);
            
            long batchStart = System.currentTimeMillis();
            testDataMapper.insertContracts(batch);
            long batchEnd = System.currentTimeMillis();
            
            int currentBatch = (i / BATCH_SIZE) + 1;
            int progress = (int) ((currentBatch * 100.0) / totalBatches);
            log.info("Contract 배치 {}/{} 완료 ({}%) - {} 건, {} ms", 
                     currentBatch, totalBatches, progress, batch.size(), (batchEnd - batchStart));
        }
    }
    
    private void insertProductsBatch(List<ProductEntity> products) {
        int totalBatches = (products.size() + BATCH_SIZE - 1) / BATCH_SIZE;
        log.info("Product 배치 삽입 시작: {} 건 ({} 배치)", products.size(), totalBatches);
        
        for (int i = 0; i < products.size(); i += BATCH_SIZE) {
            int endIndex = Math.min(i + BATCH_SIZE, products.size());
            List<ProductEntity> batch = products.subList(i, endIndex);
            
            long batchStart = System.currentTimeMillis();
            testDataMapper.insertProducts(batch);
            long batchEnd = System.currentTimeMillis();
            
            int currentBatch = (i / BATCH_SIZE) + 1;
            int progress = (int) ((currentBatch * 100.0) / totalBatches);
            log.info("Product 배치 {}/{} 완료 ({}%) - {} 건, {} ms", 
                     currentBatch, totalBatches, progress, batch.size(), (batchEnd - batchStart));
        }
    }
    
    private void insertSuspensionsBatch(List<SuspensionEntity> suspensions) {
        int totalBatches = (suspensions.size() + BATCH_SIZE - 1) / BATCH_SIZE;
        log.info("Suspension 배치 삽입 시작: {} 건 ({} 배치)", suspensions.size(), totalBatches);
        
        for (int i = 0; i < suspensions.size(); i += BATCH_SIZE) {
            int endIndex = Math.min(i + BATCH_SIZE, suspensions.size());
            List<SuspensionEntity> batch = suspensions.subList(i, endIndex);
            
            long batchStart = System.currentTimeMillis();
            testDataMapper.insertSuspensions(batch);
            long batchEnd = System.currentTimeMillis();
            
            int currentBatch = (i / BATCH_SIZE) + 1;
            int progress = (int) ((currentBatch * 100.0) / totalBatches);
            log.info("Suspension 배치 {}/{} 완료 ({}%) - {} 건, {} ms", 
                     currentBatch, totalBatches, progress, batch.size(), (batchEnd - batchStart));
        }
    }
}