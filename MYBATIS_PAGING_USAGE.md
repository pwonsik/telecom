# MyBatis 조건부 조회 및 배치 처리 가이드

## 개요

MyBatis SQL을 웹서비스와 배치에서 공통으로 사용할 수 있도록 리팩토링했습니다. `contractId` 입력 여부에 따라 단건/전체 조회가 결정됩니다.

## SQL 구조 변경사항

### 1. 조건부 WHERE 절
```xml
<sql id="contractFilterConditions">
    <!-- 계약 ID 조건 (조건부) -->
    <if test="contractId != null">
        AND c.contract_id = #{contractId}
    </if>
    <!-- 기타 공통 조건들... -->
</sql>
```

### 2. SQL Fragment 분리
- `contractSelectClause`: SELECT 절과 FROM/JOIN 절
- `contractFilterConditions`: WHERE 조건절 (조건부 contractId 포함)
- `contractOrderByClause`: ORDER BY 절 (MyBatisPagingItemReader용)

### 3. 두 개의 메서드 제공
- `findContractWithProductsChargeItemsAndSuspensions`: 웹서비스용 (단건, 기존 호환성 유지)
- `findContractsWithProductsChargeItemsAndSuspensions`: 배치용 (단건/전체 선택 가능)

## 사용법

### 웹서비스에서 단건 조회
```java
// ContractQueryRepository 사용
Contract contractWithProductsAndSuspensions = contractQueryRepository.findContractWithProductsChargeItemsAndSuspensions(
    contractId,        // Long: 계약 ID
    billingStartDate,  // LocalDate: 청구 시작일
    billingEndDate     // LocalDate: 청구 종료일
);
```

### Spring Batch에서 전체 조회
```java
// MyBatisPagingItemReader 설정
Map<String, Object> parameterValues = new HashMap<>();
parameterValues.put("contractId", null);  // null = 전체 조회
parameterValues.put("billingStartDate", LocalDate.of(2024, 3, 1));
parameterValues.put("billingEndDate", LocalDate.of(2024, 3, 31));

MyBatisPagingItemReader<ContractDto> reader = new MyBatisPagingItemReaderBuilder<ContractDto>()
    .sqlSessionFactory(sqlSessionFactory)
    .queryId("me.realimpact.telecom.calculation.infrastructure.adapter.mybatis.ContractQueryMapper.findContractsWithProductsChargeItemsAndSuspensions")
    .parameterValues(parameterValues)
    .pageSize(1000)  // 1000개씩 페이징
    .build();
```

### Spring Batch에서 특정 계약만 조회
```java
// 특정 계약 ID 지정
parameterValues.put("contractId", 12345L);  // 특정 계약만 조회
```

## MyBatisPagingItemReader 정렬 키

복합키 순서로 정렬되도록 ORDER BY 절이 설정되어 있습니다:

```sql
ORDER BY 
    c.contract_id,                    -- 계약 ID
    po.product_offering_id,           -- 상품 오퍼링 ID
    p.effective_start_date_time,      -- 상품 유효 시작일시
    p.effective_end_date_time,        -- 상품 유효 종료일시
    mci.charge_item_id,               -- 과금 항목 ID
    s.suspension_type_code,           -- 정지 유형 코드
    s.effective_start_date_time,      -- 정지 시작일시
    s.effective_end_date_time         -- 정지 종료일시
```

이 정렬 순서는 MyBatis의 중첩된 ResultMap에서 올바르게 데이터를 그룹핑하는 데 필요합니다.

## 배치 처리 예제

전체적인 배치 처리 흐름:

```java
@Configuration
public class ContractBatchConfig {
    
    // 1. Reader: 계약 데이터 읽기
    @Bean
    public MyBatisPagingItemReader<ContractDto> contractReader() {
        // contractId null = 전체 조회
        // contractId 값 지정 = 해당 계약만 조회
    }
    
    // 2. Processor: 계산 수행
    @Bean
    public ItemProcessor<ContractDto, MonthlyFeeCalculationResult> contractProcessor() {
        return contractDto -> {
            // DTO를 도메인 객체로 변환
            Contract contractWithProductsAndSuspensions = converter.convertToContract(contractDto);
            
            // 월정액 요금 계산 수행
            List<MonthlyFeeCalculationResult> results = calculator.calculate(contractWithProductsAndSuspensions);
            
            return results;
        };
    }
    
    // 3. Writer: 결과 저장
    @Bean
    public ItemWriter<MonthlyFeeCalculationResult> resultWriter() {
        return chunk -> {
            // 배치로 DB에 저장
            calculationResultRepository.batchSaveCalculationResults(chunk);
        };
    }
}
```

## 성능 고려사항

### 1. 페이지 크기 설정
- `pageSize`: 1000~5000 정도가 적당 (메모리와 성능 균형)
- 너무 크면 메모리 부족, 너무 작으면 성능 저하

### 2. 청크 크기 설정
- Spring Batch `chunk`: 100~1000 정도가 적당
- 트랜잭션 단위가 되므로 적절한 크기 유지

### 3. 인덱스 활용
```sql
-- 필요한 인덱스들
CREATE INDEX idx_contract_billing_period ON contractWithProductsAndSuspensions(contract_id, subscribed_at, terminated_at);
CREATE INDEX idx_product_period ON product(contract_id, effective_start_date_time, effective_end_date_time);
CREATE INDEX idx_suspension_period ON suspension(contract_id, effective_start_date_time, effective_end_date_time);
```

## 주의사항

1. **정렬 순서 변경 금지**: MyBatisPagingItemReader는 ORDER BY 절에 의존하므로 정렬 순서를 변경하면 페이징이 깨집니다.

2. **파라미터 타입 일치**: MyBatis 파라미터 타입이 일치해야 합니다.
   - `contractId`: `Long` 타입 (null 가능)
   - `billingStartDate`, `billingEndDate`: `LocalDate` 타입

3. **트랜잭션 관리**: 배치 처리에서는 적절한 트랜잭션 경계를 설정해야 합니다.

4. **메모리 관리**: 대용량 처리시 JVM 메모리 설정을 적절히 조정해야 합니다.