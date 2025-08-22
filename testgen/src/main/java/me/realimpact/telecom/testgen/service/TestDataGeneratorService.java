package me.realimpact.telecom.testgen.service;

import com.github.javafaker.Faker;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import me.realimpact.telecom.testgen.entity.ContractEntity;
import me.realimpact.telecom.testgen.entity.ProductEntity;
import me.realimpact.telecom.testgen.entity.SuspensionEntity;
import me.realimpact.telecom.testgen.entity.DeviceInstallmentMasterEntity;
import me.realimpact.telecom.testgen.entity.DeviceInstallmentDetailEntity;
import me.realimpact.telecom.testgen.entity.InstallationHistoryEntity;
import me.realimpact.telecom.testgen.mapper.TestDataMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
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
            
            // 테이블 데이터 삭제 (외래키 순서에 따라)
            testDataMapper.truncateSuspensions();
            log.info("suspension 테이블 초기화 완료");
            
            testDataMapper.truncateProducts();
            log.info("product 테이블 초기화 완료");
            
            testDataMapper.truncateDeviceInstallmentDetails();
            log.info("device_installment_detail 테이블 초기화 완료");
            
            testDataMapper.truncateDeviceInstallmentMasters();
            log.info("device_installment_master 테이블 초기화 완료");
            
            testDataMapper.truncateInstallationHistories();
            log.info("installation_history 테이블 초기화 완료");
            
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
        
        // 6. 단말할부 마스터 생성 및 배치 삽입
        List<DeviceInstallmentMasterEntity> allInstallmentMasters = new ArrayList<>();
        List<DeviceInstallmentDetailEntity> allInstallmentDetails = new ArrayList<>();
        for (ContractEntity contract : contracts) {
            List<DeviceInstallmentMasterEntity> masters = generateDeviceInstallmentMasters(contract);
            allInstallmentMasters.addAll(masters);
            
            for (DeviceInstallmentMasterEntity master : masters) {
                List<DeviceInstallmentDetailEntity> details = generateDeviceInstallmentDetails(master);
                allInstallmentDetails.addAll(details);
            }
        }
        if (!allInstallmentMasters.isEmpty()) {
            insertDeviceInstallmentMastersBatch(allInstallmentMasters);
            log.info("DeviceInstallmentMaster {} 건 삽입 완료", allInstallmentMasters.size());
            
            insertDeviceInstallmentDetailsBatch(allInstallmentDetails);
            log.info("DeviceInstallmentDetail {} 건 삽입 완료", allInstallmentDetails.size());
        }
        
        // 7. 설치 이력 생성 및 배치 삽입
        List<InstallationHistoryEntity> allInstallationHistories = new ArrayList<>();
        for (ContractEntity contract : contracts) {
            List<InstallationHistoryEntity> histories = generateInstallationHistories(contract);
            allInstallationHistories.addAll(histories);
        }
        if (!allInstallationHistories.isEmpty()) {
            insertInstallationHistoriesBatch(allInstallationHistories);
            log.info("InstallationHistory {} 건 삽입 완료", allInstallationHistories.size());
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
    
    /**
     * 단말할부 마스터 데이터 생성
     * 계약별로 최소 1개, 최대 2개 생성
     */
    private List<DeviceInstallmentMasterEntity> generateDeviceInstallmentMasters(ContractEntity contract) {
        List<DeviceInstallmentMasterEntity> masters = new ArrayList<>();
        
        int masterCount = random.nextInt(2) + 1; // 1~2개
        
        for (int i = 0; i < masterCount; i++) {
            Long installmentSequence = (long) (i + 1);
            
            // 할부 시작일: 계약일 이후 랜덤
            LocalDate installmentStartDate = contract.getSubscribedAt().plusDays(random.nextInt(90));
            
            // 할부 개월수: 12, 24, 36개월 중 랜덤
            Integer installmentMonths = List.of(12, 24, 36).get(random.nextInt(3));
            
            // 할부금 총액: 10만원~100만원, 1만원 단위
            BigDecimal totalAmount = BigDecimal.valueOf((random.nextInt(91) + 10) * 10000);
            
            // 청구된 회차 수: 0 ~ (installmentMonths - 1)
            Integer billedCount = random.nextInt(installmentMonths);
            
            DeviceInstallmentMasterEntity master = new DeviceInstallmentMasterEntity(
                contract.getContractId(),
                installmentSequence,
                installmentStartDate,
                totalAmount,
                installmentMonths,
                billedCount,
                LocalDateTime.now(),
                LocalDateTime.now()
            );
            
            masters.add(master);
        }
        
        return masters;
    }
    
    /**
     * 단말할부 상세 데이터 생성
     * installmentMonths 개수만큼 생성, billedCount까지는 청구완료 처리
     */
    private List<DeviceInstallmentDetailEntity> generateDeviceInstallmentDetails(DeviceInstallmentMasterEntity master) {
        List<DeviceInstallmentDetailEntity> details = new ArrayList<>();
        
        // 회차별 할부금 계산 (총액을 개월수로 나누고 마지막 회차에서 조정)
        BigDecimal monthlyAmount = master.getTotalInstallmentAmount().divide(
            BigDecimal.valueOf(master.getInstallmentMonths()), 0, BigDecimal.ROUND_DOWN);
        BigDecimal remainder = master.getTotalInstallmentAmount().subtract(
            monthlyAmount.multiply(BigDecimal.valueOf(master.getInstallmentMonths())));
        
        LocalDate currentBillingDate = master.getInstallmentStartDate();
        
        for (int round = 1; round <= master.getInstallmentMonths(); round++) {
            BigDecimal roundAmount = monthlyAmount;
            
            // 마지막 회차에 나머지 금액 추가
            if (round == master.getInstallmentMonths()) {
                roundAmount = roundAmount.add(remainder);
            }
            
            // 청구 완료 여부 결정
            LocalDate billingCompletedDate = null;
            if (round <= master.getBilledCount()) {
                billingCompletedDate = currentBillingDate;
                currentBillingDate = currentBillingDate.plusMonths(1);
            }
            
            DeviceInstallmentDetailEntity detail = new DeviceInstallmentDetailEntity(
                master.getContractId(),
                master.getInstallmentSequence(),
                round,
                roundAmount,
                billingCompletedDate,
                LocalDateTime.now(),
                LocalDateTime.now()
            );
            
            details.add(detail);
        }
        
        return details;
    }
    
    /**
     * 설치 이력 데이터 생성
     * 계약별로 최소 0개, 최대 2개 생성
     */
    private List<InstallationHistoryEntity> generateInstallationHistories(ContractEntity contract) {
        List<InstallationHistoryEntity> histories = new ArrayList<>();
        
        int historyCount = random.nextInt(3); // 0~2개
        
        for (int i = 0; i < historyCount; i++) {
            Long sequenceNumber = (long) (i + 1);
            
            // 설치일: 계약일 이후 랜덤
            LocalDate installationDate = contract.getSubscribedAt().plusDays(random.nextInt(60));
            
            // 설치비: 1만원~4만원, 5천원 단위
            BigDecimal installationFee = BigDecimal.valueOf((random.nextInt(7) + 2) * 5000);
            
            // 청구 여부: 70% 확률로 미청구
            String billedFlag = random.nextDouble() < 0.3 ? "Y" : "N";
            
            InstallationHistoryEntity history = new InstallationHistoryEntity(
                contract.getContractId(),
                sequenceNumber,
                installationDate,
                installationFee,
                billedFlag,
                LocalDateTime.now(),
                LocalDateTime.now()
            );
            
            histories.add(history);
        }
        
        return histories;
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
    
    private void insertDeviceInstallmentMastersBatch(List<DeviceInstallmentMasterEntity> masters) {
        int totalBatches = (masters.size() + BATCH_SIZE - 1) / BATCH_SIZE;
        log.info("DeviceInstallmentMaster 배치 삽입 시작: {} 건 ({} 배치)", masters.size(), totalBatches);
        
        for (int i = 0; i < masters.size(); i += BATCH_SIZE) {
            int endIndex = Math.min(i + BATCH_SIZE, masters.size());
            List<DeviceInstallmentMasterEntity> batch = masters.subList(i, endIndex);
            
            long batchStart = System.currentTimeMillis();
            testDataMapper.insertDeviceInstallmentMasters(batch);
            long batchEnd = System.currentTimeMillis();
            
            int currentBatch = (i / BATCH_SIZE) + 1;
            int progress = (int) ((currentBatch * 100.0) / totalBatches);
            log.info("DeviceInstallmentMaster 배치 {}/{} 완료 ({}%) - {} 건, {} ms", 
                     currentBatch, totalBatches, progress, batch.size(), (batchEnd - batchStart));
        }
    }
    
    private void insertDeviceInstallmentDetailsBatch(List<DeviceInstallmentDetailEntity> details) {
        int totalBatches = (details.size() + BATCH_SIZE - 1) / BATCH_SIZE;
        log.info("DeviceInstallmentDetail 배치 삽입 시작: {} 건 ({} 배치)", details.size(), totalBatches);
        
        for (int i = 0; i < details.size(); i += BATCH_SIZE) {
            int endIndex = Math.min(i + BATCH_SIZE, details.size());
            List<DeviceInstallmentDetailEntity> batch = details.subList(i, endIndex);
            
            long batchStart = System.currentTimeMillis();
            testDataMapper.insertDeviceInstallmentDetails(batch);
            long batchEnd = System.currentTimeMillis();
            
            int currentBatch = (i / BATCH_SIZE) + 1;
            int progress = (int) ((currentBatch * 100.0) / totalBatches);
            log.info("DeviceInstallmentDetail 배치 {}/{} 완료 ({}%) - {} 건, {} ms", 
                     currentBatch, totalBatches, progress, batch.size(), (batchEnd - batchStart));
        }
    }
    
    private void insertInstallationHistoriesBatch(List<InstallationHistoryEntity> histories) {
        int totalBatches = (histories.size() + BATCH_SIZE - 1) / BATCH_SIZE;
        log.info("InstallationHistory 배치 삽입 시작: {} 건 ({} 배치)", histories.size(), totalBatches);
        
        for (int i = 0; i < histories.size(); i += BATCH_SIZE) {
            int endIndex = Math.min(i + BATCH_SIZE, histories.size());
            List<InstallationHistoryEntity> batch = histories.subList(i, endIndex);
            
            long batchStart = System.currentTimeMillis();
            testDataMapper.insertInstallationHistories(batch);
            long batchEnd = System.currentTimeMillis();
            
            int currentBatch = (i / BATCH_SIZE) + 1;
            int progress = (int) ((currentBatch * 100.0) / totalBatches);
            log.info("InstallationHistory 배치 {}/{} 완료 ({}%) - {} 건, {} ms", 
                     currentBatch, totalBatches, progress, batch.size(), (batchEnd - batchStart));
        }
    }
}