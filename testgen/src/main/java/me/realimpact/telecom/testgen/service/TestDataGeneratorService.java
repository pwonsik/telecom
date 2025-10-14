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

/**
 * 대규모 테스트 데이터를 생성하는 서비스 클래스.
 * 메모리 문제를 피하고 안정적으로 대량의 데이터를 생성하기 위해 여러 단계의 청크 처리 방식을 사용한다.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class TestDataGeneratorService {

    private final TestDataMapper testDataMapper;
    private final ContractDiscountDataGenerator contractDiscountDataGenerator;
    private final Faker faker = new Faker();
    private final Random random = new Random();

    // DB의 max_allowed_packet 설정을 고려한 MyBatis 배치 삽입 단위
    private static final int BATCH_SIZE = 1000;
    // 메모리 사용량을 관리하기 위한 대규모 처리 단위 (10만 건)
    private static final int BLOCK_SIZE = 100000;
    // 트랜잭션 및 메모리 관리를 최적화하기 위한 중간 처리 단위 (1000 건)
    private static final int MICRO_CHUNK_SIZE = 1000;

    // 사용될 정지 유형 정의
    private static final String[][] SUSPENSION_TYPES = {
            {"F1", "일시정지"},
            {"F3", "미납정지"}
    };

    /**
     * 지정된 수의 계약에 대한 테스트 데이터 생성을 시작한다.
     * @param contractCount 생성할 총 계약 수
     */
    public void generateTestData(int contractCount) {
        log.info("=== 테스트 데이터 생성 시작: {} 건의 계약 (10만 개 단위 분할 처리) ===", contractCount);

        // 1. 기존 데이터 초기화
        initializeTestData();

        // 2. 사용 가능한 상품 오퍼링 ID 목록 조회
        List<String> productOfferingIds = getProductOfferingIds();

        // 3. 대규모 데이터를 안정적으로 처리하기 위해 블록 단위로 분할
        int totalBlocks = (contractCount + BLOCK_SIZE - 1) / BLOCK_SIZE;
        log.info("총 {} 건을 {} 블록으로 분할하여 처리 (블록당 최대 {} 건)", contractCount, totalBlocks, BLOCK_SIZE);

        long overallStartTime = System.currentTimeMillis();
        int totalProcessed = 0;

        for (int blockIndex = 0; blockIndex < totalBlocks; blockIndex++) {
            int startIndex = blockIndex * BLOCK_SIZE;
            int endIndex = Math.min(startIndex + BLOCK_SIZE, contractCount);
            int currentBlockSize = endIndex - startIndex;

            log.info("\n=== 블록 {}/{} 처리 시작 ===", blockIndex + 1, totalBlocks);
            log.info("계약 범위: {}~{} ({} 건)", startIndex + 1, endIndex, currentBlockSize);

            long blockStartTime = System.currentTimeMillis();

            try {
                generateTestDataBlock(startIndex, currentBlockSize, productOfferingIds);
                totalProcessed += currentBlockSize;

                long blockEndTime = System.currentTimeMillis();
                double blockDurationSec = (blockEndTime - blockStartTime) / 1000.0;

                // 메모리 사용량 모니터링
                Runtime runtime = Runtime.getRuntime();
                long usedMemory = runtime.totalMemory() - runtime.freeMemory();
                long maxMemory = runtime.maxMemory();
                double memoryUsagePercent = (usedMemory * 100.0) / maxMemory;

                log.info("블록 {}/{} 완료 - 소요시간: {:.1f}초, 누적 처리: {} 건, 메모리 사용률: {:.1f}%",
                         blockIndex + 1, totalBlocks, blockDurationSec, totalProcessed, memoryUsagePercent);

                // 다음 블록 처리를 위한 메모리 정리 및 안정화
                performBlockCleanup(blockIndex + 1, totalBlocks);

            } catch (Exception e) {
                log.error("블록 {}/{} 처리 중 오류 발생 (범위: {}~{})", blockIndex + 1, totalBlocks, startIndex + 1, endIndex, e);
                throw new RuntimeException(String.format("블록 %d 처리 실패", blockIndex + 1), e);
            }
        }

        // 모든 계약 데이터 생성 후, 할인 정보 생성
        try {
            log.info("Contract Discount 데이터 생성 시작...");
            contractDiscountDataGenerator.generateContractDiscounts();
            log.info("Contract Discount 데이터 생성 완료");
        } catch (Exception e) {
            log.error("Contract Discount 생성 중 오류 발생", e);
        }

        long overallEndTime = System.currentTimeMillis();
        double totalDurationSec = (overallEndTime - overallStartTime) / 1000.0;
        double avgRate = totalProcessed / totalDurationSec;

        log.info("\n=== 전체 테스트 데이터 생성 완료 ===");
        log.info("총 처리 건수: {} 건", totalProcessed);
        log.info("총 소요 시간: {:.1f}초", totalDurationSec);
        log.info("평균 처리 속도: {:.0f} 건/초", avgRate);
    }

    /**
     * 데이터 생성 전, 모든 관련 테이블의 데이터를 초기화한다.
     */
    @Transactional
    private void initializeTestData() {
        log.info("=== 기존 테스트 데이터 초기화 시작 ===");
        logTableCounts("초기화 전");

        try {
            testDataMapper.disableForeignKeyChecks();
            log.info("✅ 외래키 제약조건 비활성화 완료");

            // 외래키 의존성 역순으로 테이블 초기화
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

            testDataMapper.enableForeignKeyChecks();
            log.info("✅ 외래키 제약조건 재활성화 완료");

            logTableCounts("초기화 후");
            log.info("=== 모든 테스트 데이터 테이블 초기화 성공 완료 ===");

        } catch (Exception e) {
            log.error("❌ 테이블 초기화 중 오류 발생", e);
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
     * 테이블이 비어있는지 확인하고 로그를 남긴다.
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
     * 현재 각 테이블의 레코드 수를 로깅한다.
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
     * DB에서 사용 가능한 상품 오퍼링 ID 목록을 조회한다.
     */
    private List<String> getProductOfferingIds() {
        List<String> productOfferingIds = testDataMapper.selectProductOfferingIds();
        log.info("사용 가능한 Product Offering: {}", productOfferingIds);

        if (productOfferingIds == null || productOfferingIds.isEmpty()) {
            log.error("Product Offering 데이터가 없습니다. sample_data.sql을 먼저 실행해주세요.");
            throw new RuntimeException("Product Offering 데이터가 없습니다. 기본 샘플 데이터를 확인해주세요.");
        }

        return productOfferingIds;
    }

    /**
     * 블록 단위의 데이터 생성을 마이크로 청크 단위로 나누어 처리한다.
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
                long memoryAfterChunk = runtime.totalMemory() - runtime.freeMemory();
                long memoryUsedInChunk = memoryAfterChunk - memoryBeforeChunk;

                log.info("마이크로 청크 {}/{} 완료: {} ms, 청크 메모리 사용량: {} MB, 현재 총 사용량: {} MB",
                        microChunkIndex + 1, totalMicroChunks, microEndTime - microStartTime,
                        memoryUsedInChunk / (1024 * 1024), memoryAfterChunk / (1024 * 1024));

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
     * 트랜잭션 단위인 마이크로 청크 데이터를 생성하고 DB에 삽입한다.
     */
    @Transactional
    private void generateMicroChunk(int startIndex, int chunkSize, List<String> productOfferingIds) {
        log.debug("마이크로 청크 데이터 생성: {} 건 (시작 인덱스: {})", chunkSize, startIndex);

        List<ContractEntity> contracts = generateContracts(chunkSize, startIndex);
        insertContractsBatch(contracts);
        log.debug("Contract {} 건 삽입 완료", contracts.size());

        List<ProductEntity> allProducts = new ArrayList<>();
        for (ContractEntity contract : contracts) {
            allProducts.addAll(generateProducts(contract, productOfferingIds));
        }
        insertProductsBatch(allProducts);
        log.debug("Product {} 건 삽입 완료", allProducts.size());
        allProducts.clear();

        List<SuspensionEntity> allSuspensions = new ArrayList<>();
        for (ContractEntity contract : contracts) {
            allSuspensions.addAll(generateSuspensions(contract));
        }
        if (!allSuspensions.isEmpty()) {
            insertSuspensionsBatch(allSuspensions);
            log.debug("Suspension {} 건 삽입 완료", allSuspensions.size());
        }
        allSuspensions.clear();

        List<DeviceInstallmentMasterEntity> allInstallmentMasters = new ArrayList<>();
        List<DeviceInstallmentDetailEntity> allInstallmentDetails = new ArrayList<>();
        for (ContractEntity contract : contracts) {
            List<DeviceInstallmentMasterEntity> masters = generateDeviceInstallmentMasters(contract);
            allInstallmentMasters.addAll(masters);
            for (DeviceInstallmentMasterEntity master : masters) {
                allInstallmentDetails.addAll(generateDeviceInstallmentDetails(master));
            }
        }
        if (!allInstallmentMasters.isEmpty()) {
            insertDeviceInstallmentMastersBatch(allInstallmentMasters);
            log.debug("DeviceInstallmentMaster {} 건 삽입 완료", allInstallmentMasters.size());
            insertDeviceInstallmentDetailsBatch(allInstallmentDetails);
            log.debug("DeviceInstallmentDetail {} 건 삽입 완료", allInstallmentDetails.size());
        }
        allInstallmentMasters.clear();
        allInstallmentDetails.clear();

        List<InstallationHistoryEntity> allInstallationHistories = new ArrayList<>();
        for (ContractEntity contract : contracts) {
            allInstallationHistories.addAll(generateInstallationHistories(contract));
        }
        if (!allInstallationHistories.isEmpty()) {
            insertInstallationHistoriesBatch(allInstallationHistories);
            log.debug("InstallationHistory {} 건 삽입 완료", allInstallationHistories.size());
        }
        allInstallationHistories.clear();

        contracts.clear();
        log.debug("마이크로 청크 데이터 생성 완료: {} 건 (메모리 정리 완료)", chunkSize);
    }

    /**
     * 마이크로 청크 처리 후, GC를 유도하여 메모리를 확보한다.
     */
    private void performMicroChunkCleanup() {
        Runtime runtime = Runtime.getRuntime();
        long memoryBeforeGC = runtime.totalMemory() - runtime.freeMemory();
        long totalMemoryBefore = runtime.totalMemory();

        try {
            for (int i = 0; i < 3; i++) {
                System.gc();
                Thread.sleep(200);
            }
            Thread.sleep(300);

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
     * 블록 처리 후, GC를 유도하고 잠시 대기하여 시스템을 안정화시킨다.
     */
    private void performBlockCleanup(int currentBlock, int totalBlocks) {
        Runtime runtime = Runtime.getRuntime();
        long memoryBeforeGC = runtime.totalMemory() - runtime.freeMemory();
        System.gc();

        try {
            Thread.sleep(1000);
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
                Thread.sleep(2000);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }
    }

    /**
     * 지정된 수의 계약(Contract) 엔티티를 생성한다.
     */
    private List<ContractEntity> generateContracts(int count, int startIndex) {
        List<ContractEntity> contracts = new ArrayList<>();
        long baseContractId = System.currentTimeMillis() / 1000;

        for (int i = 0; i < count; i++) {
            LocalDate subscribedAt = faker.date().past(365, TimeUnit.DAYS).toInstant()
                    .atZone(java.time.ZoneId.systemDefault()).toLocalDate();
            LocalDate initiallySubscribedAt = subscribedAt.minusDays(random.nextInt(30));

            LocalDate terminatedAt = random.nextDouble() < 0.3 ?
                    subscribedAt.plusDays(random.nextInt(200) + 30) : null;

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

    /**
     * 특정 계약에 대한 상품(Product) 엔티티를 생성한다.
     * 다양한 시나리오(단일, 다수, 동일, 혼합)에 따라 상품을 생성한다.
     */
    private List<ProductEntity> generateProducts(ContractEntity contract, List<String> productOfferingIds) {
        List<ProductEntity> products = new ArrayList<>();
        int pattern = random.nextInt(4) + 1;

        switch (pattern) {
            case 1: // 단일 상품
                products.add(createSingleProduct(contract, productOfferingIds));
                break;
            case 2: // 서로 다른 상품
                products.addAll(createDifferentProducts(contract, productOfferingIds));
                break;
            case 3: // 동일 상품 (시간 중첩 없음)
                products.addAll(createSameProducts(contract, productOfferingIds));
                break;
            case 4: // 혼합
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

        products.addAll(createSameProducts(contract, productOfferingIds));

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

    /**
     * 특정 계약에 대한 정지(Suspension) 이력 엔티티를 생성한다.
     * 70% 확률로 정지 이력이 생성되지 않는다.
     */
    private List<SuspensionEntity> generateSuspensions(ContractEntity contract) {
        List<SuspensionEntity> suspensions = new ArrayList<>();
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
            currentTime = suspensionEnd.plusDays(random.nextInt(30) + 7);
        }
        return suspensions;
    }

    /**
     * 단말기 할부 마스터 데이터를 생성한다.
     */
    private List<DeviceInstallmentMasterEntity> generateDeviceInstallmentMasters(ContractEntity contract) {
        List<DeviceInstallmentMasterEntity> masters = new ArrayList<>();
        int masterCount = random.nextInt(2) + 1; // 1~2개

        for (int i = 0; i < masterCount; i++) {
            Long installmentSequence = (long) (i + 1);
            LocalDate installmentStartDate = contract.getSubscribedAt().plusDays(random.nextInt(90));
            Integer installmentMonths = List.of(1, 3, 6).get(random.nextInt(3));
            BigDecimal totalAmount = BigDecimal.valueOf((random.nextInt(91) + 10) * 10000);
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
     * 단말기 할부 상세 데이터를 생성한다.
     */
    private List<DeviceInstallmentDetailEntity> generateDeviceInstallmentDetails(DeviceInstallmentMasterEntity master) {
        List<DeviceInstallmentDetailEntity> details = new ArrayList<>();
        BigDecimal monthlyAmount = master.getTotalInstallmentAmount().divide(
                BigDecimal.valueOf(master.getInstallmentMonths()), 0, BigDecimal.ROUND_DOWN);
        BigDecimal remainder = master.getTotalInstallmentAmount().subtract(
                monthlyAmount.multiply(BigDecimal.valueOf(master.getInstallmentMonths())));

        LocalDate currentBillingDate = master.getInstallmentStartDate();

        for (int round = 1; round <= master.getInstallmentMonths(); round++) {
            BigDecimal roundAmount = monthlyAmount;
            if (round == master.getInstallmentMonths()) {
                roundAmount = roundAmount.add(remainder);
            }

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
     * 설치 이력 데이터를 생성한다.
     */
    private List<InstallationHistoryEntity> generateInstallationHistories(ContractEntity contract) {
        List<InstallationHistoryEntity> histories = new ArrayList<>();
        int historyCount = random.nextInt(3); // 0~2개

        for (int i = 0; i < historyCount; i++) {
            Long sequenceNumber = (long) (i + 1);
            LocalDate installationDate = contract.getSubscribedAt().plusDays(random.nextInt(60));
            BigDecimal installationFee = BigDecimal.valueOf((random.nextInt(7) + 2) * 5000);
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

    /**
     * 생성된 엔티티 리스트를 배치 단위로 DB에 삽입한다.
     */
    private void insertContractsBatch(List<ContractEntity> contracts) {
        executeBatch(contracts, testDataMapper::insertContracts, "Contract");
    }

    private void insertProductsBatch(List<ProductEntity> products) {
        executeBatch(products, testDataMapper::insertProducts, "Product");
    }

    private void insertSuspensionsBatch(List<SuspensionEntity> suspensions) {
        executeBatch(suspensions, testDataMapper::insertSuspensions, "Suspension");
    }

    private void insertDeviceInstallmentMastersBatch(List<DeviceInstallmentMasterEntity> masters) {
        executeBatch(masters, testDataMapper::insertDeviceInstallmentMasters, "DeviceInstallmentMaster");
    }

    private void insertDeviceInstallmentDetailsBatch(List<DeviceInstallmentDetailEntity> details) {
        executeBatch(details, testDataMapper::insertDeviceInstallmentDetails, "DeviceInstallmentDetail");
    }

    private void insertInstallationHistoriesBatch(List<InstallationHistoryEntity> histories) {
        executeBatch(histories, testDataMapper::insertInstallationHistories, "InstallationHistory");
    }

    private <T> void executeBatch(List<T> list, java.util.function.Consumer<List<T>> batchInsert, String entityName) {
        if (list == null || list.isEmpty()) return;

        int totalBatches = (list.size() + BATCH_SIZE - 1) / BATCH_SIZE;
        log.info("{} 배치 삽입 시작: {} 건 ({} 배치)", entityName, list.size(), totalBatches);

        for (int i = 0; i < list.size(); i += BATCH_SIZE) {
            int endIndex = Math.min(i + BATCH_SIZE, list.size());
            List<T> batch = list.subList(i, endIndex);

            long batchStart = System.currentTimeMillis();
            batchInsert.accept(batch);
            long batchEnd = System.currentTimeMillis();

            int currentBatch = (i / BATCH_SIZE) + 1;
            int progress = (int) ((currentBatch * 100.0) / totalBatches);
            log.info("{} 배치 {}/{} 완료 ({}%) - {} 건, {} ms",
                    entityName, currentBatch, totalBatches, progress, batch.size(), (batchEnd - batchStart));
        }
    }
}
