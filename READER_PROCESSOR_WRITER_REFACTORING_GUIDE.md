# Reader-Processor-Writer 패턴 리팩토링 가이드

## 개요

`MonthlyFeeCalculatorService`를 Spring Batch의 Reader-Processor-Writer 패턴에 맞게 리팩토링했습니다. 이제 각 기능이 명확히 분리되어 웹서비스와 배치에서 모두 효율적으로 사용할 수 있습니다.

## 설계 개선 사항

### Before (문제점)
```java
// 하나의 메서드에 Read-Process-Write가 모두 섞여있음
public List<MonthlyFeeCalculationResult> calculate(CalculationRequest context) {
    // Read: 계약 데이터 조회
    Contract contract = contractQueryPort.findContract(...);
    
    // Process: 계산 수행
    List<ProratedPeriod> periods = contract.buildProratedPeriods(...);
    List<MonthlyFeeCalculationResult> results = periods.stream()...;
    
    // Write: 결과 저장
    calculationResultSavePort.batchSaveCalculationResults(...);
    
    return results;
}
```

### After (개선됨)
```java
// 각 기능이 명확히 분리됨
public class MonthlyFeeCalculatorService {
    
    // ============= Reader 패턴 =============
    public Contract readContract(CalculationRequest context) { ... }
    public Contract readContract(Long contractId, LocalDate start, LocalDate end) { ... }
    
    // ============= Processor 패턴 =============
    public List<MonthlyFeeCalculationResult> processCalculation(Contract contract, DefaultPeriod period) { ... }
    
    // ============= Writer 패턴 =============
    public void writeResults(List<MonthlyFeeCalculationResult> results, DefaultPeriod period) { ... }
    
    // ============= 전체 흐름 (기존 호환성 유지) =============
    public List<MonthlyFeeCalculationResult> calculate(CalculationRequest context) {
        Contract contract = readContract(context);          // Read
        List<MonthlyFeeCalculationResult> results = processCalculation(contract, context); // Process
        writeResults(results, context);                     // Write
        return results;
    }
}
```

## 장점

### 1. **단일 책임 원칙(SRP) 준수**
- Reader: 데이터 조회만 담당
- Processor: 순수 계산 로직만 담당
- Writer: 결과 저장만 담당

### 2. **Spring Batch 자연스러운 통합**
- 각 메서드가 Batch의 ItemReader, ItemProcessor, ItemWriter와 1:1 대응
- 기존 배치 프레임워크 패턴과 완벽 호환

### 3. **테스트 용이성**
- 각 기능을 독립적으로 테스트 가능
- 순수 계산 로직(Process)을 별도로 테스트 가능

### 4. **재사용성 증대**
- 웹서비스: 전체 흐름 메서드 사용
- 배치: 개별 Reader/Processor/Writer 사용
- 다른 컨텍스트에서 개별 기능 활용 가능

## 사용법

### 웹서비스에서 사용 (기존과 동일)
```java
@RestController
@RequiredArgsConstructor
public class CalculationController {
    
    private final MonthlyFeeCalculatorService calculatorService;
    
    @PostMapping("/calculate")
    public List<MonthlyFeeCalculationResult> calculate(@RequestBody CalculationRequest request) {
        // 기존과 동일하게 사용 (호환성 유지)
        return calculatorService.calculate(request);
    }
}
```

### Spring Batch에서 사용 (새로운 방식)
```java
@Configuration
public class MonthlyFeeCalculationBatchConfig {
    
    // Reader: MyBatisPagingItemReader 사용
    @Bean
    public MyBatisPagingItemReader<ContractDto> contractReader() {
        return new MyBatisPagingItemReaderBuilder<ContractDto>()
                .sqlSessionFactory(sqlSessionFactory)
                .queryId("ContractQueryMapper.findContractsWithProductsChargeItemsAndSuspensions")
                .parameterValues(parameterValues)
                .pageSize(1000)
                .build();
    }
    
    // Processor: MonthlyFeeCalculationProcessor 사용
    @Bean
    public MonthlyFeeCalculationProcessor monthlyFeeProcessor() {
        // MonthlyFeeCalculatorService.processCalculation() 활용
        return new MonthlyFeeCalculationProcessor(calculatorService, converter);
    }
    
    // Writer: MonthlyFeeCalculationWriter 사용  
    @Bean
    public MonthlyFeeCalculationWriter monthlyFeeWriter() {
        // MonthlyFeeCalculatorService.writeResults() 활용
        return new MonthlyFeeCalculationWriter(calculatorService);
    }
    
    @Bean
    public Step calculationStep() {
        return new StepBuilder("calculationStep", jobRepository)
                .<ContractDto, List<MonthlyFeeCalculationResult>>chunk(100, transactionManager)
                .reader(contractReader())
                .processor(monthlyFeeProcessor())
                .writer(monthlyFeeWriter())
                .build();
    }
}
```

## 주요 클래스

### 1. MonthlyFeeCalculatorService (리팩토링됨)
```java
// Reader 메서드들
public Contract readContract(CalculationRequest context);
public Contract readContract(Long contractId, LocalDate start, LocalDate end);

// Processor 메서드들  
public List<MonthlyFeeCalculationResult> processCalculation(Contract contract, DefaultPeriod period);
public List<MonthlyFeeCalculationResult> processCalculation(Contract contract, CalculationRequest context);

// Writer 메서드들
public void writeResults(List<MonthlyFeeCalculationResult> results, DefaultPeriod period);
public void writeResults(List<MonthlyFeeCalculationResult> results, CalculationRequest context);

// 유틸리티
public DefaultPeriod createBillingPeriod(CalculationRequest context);

// 전체 흐름 (기존 호환성)
public List<MonthlyFeeCalculationResult> calculate(CalculationRequest context);
```

### 2. MonthlyFeeCalculationProcessor (새로 생성)
```java
@Component
public class MonthlyFeeCalculationProcessor 
    implements ItemProcessor<ContractDto, List<MonthlyFeeCalculationResult>> {
    
    @Override
    public List<MonthlyFeeCalculationResult> process(ContractDto contractDto) throws Exception {
        // DTO → 도메인 변환
        Contract contract = dtoToDomainConverter.convertToContract(contractDto);
        
        // 계산 수행 (MonthlyFeeCalculatorService의 processCalculation 활용)
        return monthlyFeeCalculatorService.processCalculation(contract, billingPeriod);
    }
}
```

### 3. MonthlyFeeCalculationWriter (새로 생성)
```java
@Component  
public class MonthlyFeeCalculationWriter 
    implements ItemWriter<List<MonthlyFeeCalculationResult>> {
    
    @Override
    public void write(Chunk<? extends List<MonthlyFeeCalculationResult>> chunk) throws Exception {
        // 모든 결과 플래튼
        List<MonthlyFeeCalculationResult> allResults = chunk.getItems().stream()
                .flatMap(List::stream)
                .toList();
        
        // 저장 (MonthlyFeeCalculatorService의 writeResults 활용)
        monthlyFeeCalculatorService.writeResults(allResults, billingPeriod);
    }
}
```

## 데이터 흐름

### 웹서비스 흐름
```
CalculationRequest 
  ↓
MonthlyFeeCalculatorService.calculate()
  ↓
read → process → write → return results
```

### Spring Batch 흐름
```
MyBatisPagingItemReader<ContractDto>
  ↓
MonthlyFeeCalculationProcessor (ContractDto → List<MonthlyFeeCalculationResult>)
  ↓  
MonthlyFeeCalculationWriter (List<MonthlyFeeCalculationResult> → DB)
```

## 성능 고려사항

### 1. 웹서비스
- 단건 처리에 최적화
- Read-Process-Write가 하나의 트랜잭션에서 실행
- 빠른 응답 시간

### 2. Spring Batch
- 대용량 처리에 최적화
- 청크 단위 트랜잭션 처리 (예: 100개씩)
- 페이징 기반 메모리 효율적 처리
- 실패 시 재시작 지원

## 마이그레이션 가이드

### 기존 코드는 변경 불필요
```java
// 기존 코드 그대로 동작
List<MonthlyFeeCalculationResult> results = calculatorService.calculate(request);
```

### 새로운 배치 처리
```java
// 새로운 배치 설정으로 대용량 처리 가능
@Bean
public Job monthlyFeeCalculationJob() {
    return new JobBuilder("monthlyFeeCalculationJob", jobRepository)
            .start(monthlyFeeCalculationStep())
            .build();
}
```

## 결론

이번 리팩토링으로:
1. **기존 웹서비스 호환성 유지**
2. **Spring Batch 자연스러운 통합**  
3. **코드 품질 향상** (SRP, 테스트 용이성)
4. **확장성 증대** (개별 기능 재사용)

모든 것이 달성되었습니다! 🎉