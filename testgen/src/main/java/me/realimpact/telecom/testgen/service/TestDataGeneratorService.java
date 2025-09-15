package me.realimpact.telecom.testgen.service;

import com.github.javafaker.Faker;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import me.realimpact.telecom.testgen.entity.*;
import me.realimpact.telecom.testgen.mapper.TestDataMapper;
import me.realimpact.telecom.testgen.ContractDiscountDataGenerator;
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
    private final ContractDiscountDataGenerator contractDiscountDataGenerator;
    private final Faker faker = new Faker();
    private final Random random = new Random();

    // 배치 처리 사이즈 (MySQL max_allowed_packet 고려)
    private static final int BATCH_SIZE = 1000;
    // 블록 처리 사이즈 (메모리 안정성을 위한 10만 개 단위)
    private static final int BLOCK_SIZE = 100000;
    // 마이크로 청크 사이즈 (트랜잭션 단위 최적화를 위한 1000건 단위)
    private static final int MICRO_CHUNK_SIZE = 1000;

    // 정지 유형 설정
    private static final String[][] SUSPENSION_TYPES = {
            {"F1", "일시정지"},
            {"F3", "미납정지"}
    };

    public void generateTestData(int contractCount) {
        log.info("=== 테스트 데이터 생성 시작: {} 건의 계약 (10만 개 단위 분할 처리) ===", contractCount);

        // 1. 기존 테스트 데이터 초기화
        initializeTestData();

        // 2. Product Offering ID 목록 조회
        List<String> productOfferingIds = getProductOfferingIds();

        // 3. 10만 개 단위로 분할 처리
        int totalBlocks = (contractCount + BLOCK_SIZE - 1) / BLOCK_SIZE;
        log.info("총 {} 건을 {} 블록으로 분할하여 처리 (블록당 최대 {} 건)", contractCount, totalBlocks, BLOCK_SIZE);

        long overallStartTime = System.currentTimeMillis();
        int totalProcessed = 0;

        for (int blockIndex = 0; blockIndex < totalBlocks; blockIndex++) {
            int startIndex = blockIndex * BLOCK_SIZE;
            int endIndex = Math.min(startIndex + BLOCK_SIZE, contractCount);
            int currentBlockSize = endIndex - startIndex;

            log.info("");
            log.info("=== 블록 {}/{} 처리 시작 ===", blockIndex + 1, totalBlocks);
            log.info("계약 범위: {}~{} ({} 건)", startIndex + 1, endIndex, currentBlockSize);

            long blockStartTime = System.currentTimeMillis();

            try {
                generateTestDataBlock(startIndex, currentBlockSize, productOfferingIds);
                totalProcessed += currentBlockSize;

                long blockEndTime = System.currentTimeMillis();
                double blockDurationSec = (blockEndTime - blockStartTime) / 1000.0;

                // 메모리 상태 체크
                Runtime runtime = Runtime.getRuntime();
                long usedMemory = runtime.totalMemory() - runtime.freeMemory();
                long maxMemory = runtime.maxMemory();
                double memoryUsagePercent = (usedMemory * 100.0) / maxMemory;

                log.info("블록 {}/{} 완료 - 소요시간: {:.1f}초, 누적 처리: {} 건, 메모리 사용률: {:.1f}%",
                         blockIndex + 1, totalBlocks, blockDurationSec, totalProcessed, memoryUsagePercent);

                // 블록 간 메모리 정리 및 시스템 안정화
                performBlockCleanup(blockIndex + 1, totalBlocks);

            } catch (Exception e) {
                log.error("블록 {}/{} 처리 중 오류 발생 (범위: {}~{})", blockIndex + 1, totalBlocks, startIndex + 1, endIndex, e);
                throw new RuntimeException(String.format("블록 %d 처리 실패", blockIndex + 1), e);
            }
        }

        // Contract Discount 생성
        try {
            log.info("Contract Discount 데이터 생성 시작...");
            contractDiscountDataGenerator.generateContractDiscounts();
            log.info("Contract Discount 데이터 생성 완료");
        } catch (Exception e) {
            log.error("Contract Discount 생성 중 오류 발생", e);
            // 할인 데이터 생성 실패는 전체 프로세스를 중단하지 않음
        }

        long overallEndTime = System.currentTimeMillis();
        double totalDurationSec = (overallEndTime - overallStartTime) / 1000.0;
        double avgRate = totalProcessed / totalDurationSec;

        log.info("");
        log.info("=== 전체 테스트 데이터 생성 완료 ===");
        log.info("총 처리 건수: {} 건", totalProcessed);
        log.info("총 소요 시간: {:.1f}초", totalDurationSec);
        log.info("평균 처리 속도: {:.0f} 건/초", avgRate);
    }

    /**
     * 테스트 데이터 초기화
     */
    @Transactional
    private void initializeTestData() {
        log.info("=== 기존 테스트 데이터 초기화 시작 ===");

        // 초기화 전 데이터 상태 확인
        logTableCounts("초기화 전");

        try {
            // 외래키 체크 비활성화
            testDataMapper.disableForeignKeyChecks();
            log.info("✅ 외래키 제약조건 비활성화 완료");

            // 테이블 데이터 TRUNCATE (외래키 순서에 따라)
            testDataMapper.truncateSuspensions();
            verifyTableEmpty("suspension", testDataMapper.countSuspensions());

            testDataMapper.truncateProducts();
            verifyTableEmpty("product", testDataMapper.countProducts());

            testDataMapper.truncateDeviceInstallmentDetails();
            verifyTableEmpty("device_installment_detail", testDataMapper.countDeviceInstallmentDetails());

            testDataMapper.truncateDeviceInstallmentMasters();
            verifyTableEmpty("device_installment_master", testDataMapper.countDeviceInstallmentMasters());

            testDataMapper.truncateInstallationHistories();
            verifyTableEmpty("installation_history", testDataMapper.countInstallationHistories());

            testDataMapper.truncateContracts();
            verifyTableEmpty("contract", testDataMapper.countContracts());

            // 외래키 체크 재활성화
            testDataMapper.enableForeignKeyChecks();
            log.info("✅ 외래키 제약조건 재활성화 완료");

            // 최종 검증
            logTableCounts("초기화 후");
            log.info("=== 모든 테스트 데이터 테이블 초기화 성공 완료 ===");

        } catch (Exception e) {
            log.error("❌ 테이블 초기화 중 오류 발생", e);
            // 오류 발생 시 외래키 체크를 다시 활성화
            try {
                testDataMapper.enableForeignKeyChecks();
                log.warn("외래키 체크 재활성화 완료 (복구)");
            } catch (Exception ex) {
                log.error("❌ 외래키 체크 재활성화 실패 (심각한 오류)", ex);
            }
            throw new RuntimeException("테이블 초기화 실패", e);
        }
    }

    /**
     * 테이블이 완전히 비워졌는지 검증
     */
    private void verifyTableEmpty(String tableName, int count) {
        if (count == 0) {
            log.info("✅ {} 테이블 TRUNCATE 완료 (레코드 수: {})", tableName, count);
        } else {
            log.error("❌ {} 테이블 TRUNCATE 실패 - 남은 레코드: {}", tableName, count);
            throw new RuntimeException(tableName + " 테이블 초기화 실패: " + count + " 건 남음");
        }
    }

    /**
     * 모든 테이블의 레코드 수 로깅
     */
    private void logTableCounts(String stage) {
        log.info("=== {} 테이블 상태 ===", stage);
        log.info("suspension: {} 건", testDataMapper.countSuspensions());
        log.info("product: {} 건", testDataMapper.countProducts());
        log.info("device_installment_detail: {} 건", testDataMapper.countDeviceInstallmentDetails());
        log.info("device_installment_master: {} 건", testDataMapper.countDeviceInstallmentMasters());
        log.info("installation_history: {} 건", testDataMapper.countInstallationHistories());
        log.info("contract: {} 건", testDataMapper.countContracts());
    }

    /**
     * Product Offering ID 목록 조회
     */
    private List<String> getProductOfferingIds() {
        List<String> productOfferingIds = testDataMapper.selectProductOfferingIds();
        log.info("사용 가능한 Product Offering: {}", productOfferingIds);

        // Product Offering이 없으면 오류 발생
        if (productOfferingIds == null || productOfferingIds.isEmpty()) {
            log.error("Product Offering 데이터가 없습니다. sample_data.sql을 먼저 실행해주세요.");
            throw new RuntimeException("Product Offering 데이터가 없습니다. 기본 샘플 데이터를 확인해주세요.");
        }

        return productOfferingIds;
    }

    /**
     * 10만 개 이하 단위 블록 처리 (마이크로 청크로 분할)
     */
    private void generateTestDataBlock(int startIndex, int blockSize, List<String> productOfferingIds) {
        log.info("블록 데이터 생성 시작: {} 건 ({}건 마이크로 청크로 분할 처리)", blockSize, MICRO_CHUNK_SIZE);

        int totalMicroChunks = (blockSize + MICRO_CHUNK_SIZE - 1) / MICRO_CHUNK_SIZE;
        log.info("총 {} 건을 {} 마이크로 청크로 분할하여 처리", blockSize, totalMicroChunks);

        int processedInBlock = 0;

        for (int microChunkIndex = 0; microChunkIndex < totalMicroChunks; microChunkIndex++) {
            int microStartIndex = startIndex + (microChunkIndex * MICRO_CHUNK_SIZE);
            int microEndIndex = Math.min(microStartIndex + MICRO_CHUNK_SIZE, startIndex + blockSize);
            int microChunkSize = microEndIndex - microStartIndex;

            // 처리 전 메모리 상태 로깅
            Runtime runtime = Runtime.getRuntime();
            long memoryBeforeChunk = runtime.totalMemory() - runtime.freeMemory();

            log.info("마이크로 청크 {}/{} 처리 시작: {} 건 (범위: {}~{}), 메모리 사용량: {} MB",
                    microChunkIndex + 1, totalMicroChunks, microChunkSize,
                    microStartIndex + 1, microEndIndex, memoryBeforeChunk / (1024 * 1024));

            long microStartTime = System.currentTimeMillis();

            try {
                generateMicroChunk(microStartIndex, microChunkSize, productOfferingIds);
                processedInBlock += microChunkSize;

                long microEndTime = System.currentTimeMillis();

                // 처리 후 메모리 상태 로깅
                long memoryAfterChunk = runtime.totalMemory() - runtime.freeMemory();
                long memoryUsedInChunk = memoryAfterChunk - memoryBeforeChunk;

                log.info("마이크로 청크 {}/{} 완료: {} ms, 청크 메모리 사용량: {} MB, 현재 총 사용량: {} MB",
                        microChunkIndex + 1, totalMicroChunks, microEndTime - microStartTime,
                        memoryUsedInChunk / (1024 * 1024), memoryAfterChunk / (1024 * 1024));

                // 마이크로 청크 간 메모리 정리 (마지막 청크 제외)
                if (microChunkIndex < totalMicroChunks - 1) {
                    performMicroChunkCleanup();
                }

            } catch (Exception e) {
                log.error("마이크로 청크 {}/{} 처리 중 오류 발생", microChunkIndex + 1, totalMicroChunks, e);
                throw e;
            }
        }

        log.info("블록 데이터 생성 완료: {} 건 ({}개 마이크로 청크)", processedInBlock, totalMicroChunks);
    }

    /**
     * 1000건 단위 마이크로 청크 처리 (트랜잭션 단위)
     */
    @Transactional
    private void generateMicroChunk(int startIndex, int chunkSize, List<String> productOfferingIds) {
        log.debug("마이크로 청크 데이터 생성: {} 건 (시작 인덱스: {})", chunkSize, startIndex);

        // Contract 생성 및 배치 삽입
        List<ContractEntity> contracts = generateContracts(chunkSize, startIndex);
        insertContractsBatch(contracts);
        log.debug("Contract {} 건 삽입 완료", contracts.size());

        // Product 생성 및 배치 삽입
        List<ProductEntity> allProducts = new ArrayList<>();
        for (ContractEntity contract : contracts) {
            List<ProductEntity> products = generateProducts(contract, productOfferingIds);
            allProducts.addAll(products);
            // 개별 products 리스트 즉시 정리
            products.clear();
        }
        insertProductsBatch(allProducts);
        log.debug("Product {} 건 삽입 완료", allProducts.size());
        // allProducts 명시적 정리
        allProducts.clear();
        allProducts = null;

        // Suspension 생성 및 배치 삽입
        List<SuspensionEntity> allSuspensions = new ArrayList<>();
        for (ContractEntity contract : contracts) {
            List<SuspensionEntity> suspensions = generateSuspensions(contract);
            allSuspensions.addAll(suspensions);
            // 개별 suspensions 리스트 즉시 정리
            suspensions.clear();
        }
        if (!allSuspensions.isEmpty()) {
            insertSuspensionsBatch(allSuspensions);
            log.debug("Suspension {} 건 삽입 완료", allSuspensions.size());
        }
        // allSuspensions 명시적 정리
        allSuspensions.clear();
        allSuspensions = null;

        // 단말할부 마스터 생성 및 배치 삽입
        List<DeviceInstallmentMasterEntity> allInstallmentMasters = new ArrayList<>();
        List<DeviceInstallmentDetailEntity> allInstallmentDetails = new ArrayList<>();
        for (ContractEntity contract : contracts) {
            List<DeviceInstallmentMasterEntity> masters = generateDeviceInstallmentMasters(contract);
            allInstallmentMasters.addAll(masters);

            for (DeviceInstallmentMasterEntity master : masters) {
                List<DeviceInstallmentDetailEntity> details = generateDeviceInstallmentDetails(master);
                allInstallmentDetails.addAll(details);
                // 개별 details 리스트 즉시 정리
                details.clear();
            }
            // 개별 masters 리스트 즉시 정리
            masters.clear();
        }
        if (!allInstallmentMasters.isEmpty()) {
            insertDeviceInstallmentMastersBatch(allInstallmentMasters);
            log.debug("DeviceInstallmentMaster {} 건 삽입 완료", allInstallmentMasters.size());

            insertDeviceInstallmentDetailsBatch(allInstallmentDetails);
            log.debug("DeviceInstallmentDetail {} 건 삽입 완료", allInstallmentDetails.size());
        }
        // 할부 관련 리스트들 명시적 정리
        allInstallmentMasters.clear();
        allInstallmentMasters = null;
        allInstallmentDetails.clear();
        allInstallmentDetails = null;

        // 설치 이력 생성 및 배치 삽입
        List<InstallationHistoryEntity> allInstallationHistories = new ArrayList<>();
        for (ContractEntity contract : contracts) {
            List<InstallationHistoryEntity> histories = generateInstallationHistories(contract);
            allInstallationHistories.addAll(histories);
            // 개별 histories 리스트 즉시 정리
            histories.clear();
        }
        if (!allInstallationHistories.isEmpty()) {
            insertInstallationHistoriesBatch(allInstallationHistories);
            log.debug("InstallationHistory {} 건 삽입 완료", allInstallationHistories.size());
        }
        // allInstallationHistories 명시적 정리
        allInstallationHistories.clear();
        allInstallationHistories = null;

        // contracts 명시적 정리 (마지막에)
        contracts.clear();
        contracts = null;

        log.debug("마이크로 청크 데이터 생성 완료: {} 건 (메모리 정리 완료)", chunkSize);
    }

    /**
     * 마이크로 청크 간 메모리 정리 (강화된 GC 및 모니터링)
     */
    private void performMicroChunkCleanup() {
        Runtime runtime = Runtime.getRuntime();

        // GC 전 메모리 상태
        long memoryBeforeGC = runtime.totalMemory() - runtime.freeMemory();
        long totalMemoryBefore = runtime.totalMemory();

        try {
            // 여러 번 GC 시도 (최대 3회)
            for (int i = 0; i < 3; i++) {
                System.gc();
                Thread.sleep(200); // 각 GC 후 200ms 대기
            }

            // 최종 대기
            Thread.sleep(300);

            // GC 후 메모리 상태
            long memoryAfterGC = runtime.totalMemory() - runtime.freeMemory();
            long totalMemoryAfter = runtime.totalMemory();
            long freedMemory = memoryBeforeGC - memoryAfterGC;

            log.debug("마이크로 청크 메모리 정리 완료 - 해제된 메모리: {} MB, 사용 중: {} MB / {} MB",
                    freedMemory / (1024 * 1024),
                    memoryAfterGC / (1024 * 1024),
                    totalMemoryAfter / (1024 * 1024));

        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            log.warn("마이크로 청크 메모리 정리 중 인터럽트 발생", e);
        }
    }

    /**
     * 블록 간 메모리 정리 및 시스템 안정화
     */
    private void performBlockCleanup(int currentBlock, int totalBlocks) {
        // 메모리 정리
        Runtime runtime = Runtime.getRuntime();
        long memoryBeforeGC = runtime.totalMemory() - runtime.freeMemory();

        System.gc();

        // GC 완료 대기
        try {
            Thread.sleep(1000); // 1초 대기
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }

        long memoryAfterGC = runtime.totalMemory() - runtime.freeMemory();
        long freedMemory = memoryBeforeGC - memoryAfterGC;

        log.info("메모리 정리 완료 - 해제: {} MB, 현재 사용: {} MB",
                 freedMemory / (1024 * 1024), memoryAfterGC / (1024 * 1024));

        if (currentBlock < totalBlocks) {
            log.info("다음 블록 처리를 위해 2초 대기...");
            try {
                Thread.sleep(2000); // 2초 대기
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }
    }

    private List<ContractEntity> generateContracts(int count, int startIndex) {
        List<ContractEntity> contracts = new ArrayList<>();
        long baseContractId = System.currentTimeMillis() / 1000; // 유니크한 기본 ID

        for (int i = 0; i < count; i++) {
            LocalDate subscribedAt = faker.date().past(365, TimeUnit.DAYS).toInstant()
                    .atZone(java.time.ZoneId.systemDefault()).toLocalDate();
            LocalDate initiallySubscribedAt = subscribedAt.minusDays(random.nextInt(30));

            // 30% 확률로 해지된 계약
            LocalDate terminatedAt = random.nextDouble() < 0.3 ?
                    subscribedAt.plusDays(random.nextInt(200) + 30) : null;

            // startIndex를 고려한 고유 Contract ID 생성
            long contractId = baseContractId + startIndex + i;

            ContractEntity contract = ContractEntity.builder()
                    .contractId(contractId)
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

            // 할부 개월수: 1, 3, 6개월 중 랜덤 (성능 향상을 위해 단축)
            Integer installmentMonths = List.of(1, 3, 6).get(random.nextInt(3));

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