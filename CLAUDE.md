# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Overview

This is a Spring Boot 3-based telecom billing calculation system implementing hexagonal architecture for Korean telecommunication services. The system calculates monthly fees with complex pro-rated billing logic following TMForum specifications.

## Development Commands

### Build and Test
```bash
# Build all modules
./gradlew build

# Run tests
./gradlew test

# Run tests for specific module
./gradlew :domain:test
./gradlew :web-service:test
./gradlew :batch:test

# Run a single test class
./gradlew :domain:test --tests "MonthlyFeeCalculationIntegrationTest"

# Clean build
./gradlew clean build
```

### Development
```bash
# Run web service
./gradlew :web-service:bootRun

# Run batch application
./gradlew :batch:bootRun

# Run batch with parameters (example)
./gradlew :batch:bootRun --args="--billingStartDate=2024-03-01 --billingEndDate=2024-03-31"

# Build and run batch JAR
./gradlew :batch:bootJar
java -jar batch/build/libs/batch-0.0.1-SNAPSHOT.jar --billingStartDate=2024-03-01 --billingEndDate=2024-03-31
```

### Batch Execution
The system includes comprehensive batch processing capabilities:
- Use `./run-batch-jar.sh` for interactive batch execution
- For full batch processing guide, see `BATCH_EXECUTION_GUIDE.md`
- Supports both full contractWithProductsAndSuspensions processing and single contractWithProductsAndSuspensions processing via `contractId` parameter

## Architecture

### Module Structure
- **domain**: Core business logic with hexagonal architecture (library module, no Spring Boot executable)
- **web-service**: REST API layer depending on domain
- **batch**: Spring Batch processing layer depending on domain for large-scale calculations
- **testgen**: Test data generation utility with JavaFaker

### Hexagonal Architecture
The domain module follows strict hexagonal architecture:
- `api/`: Inbound ports (use cases)
- `application/`: Application services implementing use cases
- `domain/`: Core business entities and logic
- `port/out/`: Outbound ports for external dependencies

### Key Business Components

#### Monthly Fee Calculation Flow
1. **CalculationUseCase**: Main entry point implementing `CalculationCommandUseCase`
2. **MonthlyFeeCalculator**: Core calculator handling pro-rated billing logic
3. **ProratedPeriodBuilder**: Splits billing periods based on contractWithProductsAndSuspensions changes, suspensions, and product changes
4. **Pricing Policies**: Strategy pattern for different pricing models (Flat rate, Range-based, Tier-based, Unit price, etc.)

#### Policy Strategy Pattern
The system uses strategy pattern for pricing policies via `MonthlyChargingPolicyFactory`:
- `FlatRatePolicy`: Standard flat rate billing
- `MatchingFactorPolicy`: B2B products with matching criteria
- `RangeFactorPolicy`: Range-based pricing
- `StepFactorPolicy`: Step-based pricing  
- `TierFactorPolicy`: Tier-based pricing
- `UnitPriceFactorPolicy`: Unit price multiplication

## Development Rules

### Code Style
- Use **record** for DTOs
- Use **Lombok** annotations to minimize boilerplate code
- Follow clean code principles with well-named variables and methods
- JPA entities should have 'JpaEntity' suffix to avoid naming conflicts
- JpaRepository interfaces should have 'JpaRepository' suffix

### Business Rules Implementation
- **Pro-rated calculation**: Contract start date is included, end date is excluded
- **Suspension periods**: Apply suspension billing rates defined per product
- **Period segmentation**: Overlap contractWithProductsAndSuspensions history with service status history for accurate billing
- **OCP principle**: New B2B products must be addable without modifying existing code
- **Calculation period**: Maximum one month (1st to end of month)

### Technology Stack
- Spring Boot 3.x with Java 21
- JPA with QueryDSL for database queries
- MyBatis for complex SQL queries and batch processing
- JUnit 5 for testing (avoid mocking in domain tests)
- Spring Batch for large-scale data processing
- MySQL as primary database
- Hexagonal architecture with clear separation of concerns

### Testing
- Domain tests should avoid mocking
- Integration tests exist in `MonthlyFeeCalculationIntegrationTest`
- Use descriptive test method names reflecting business scenarios
- Test various pricing policy combinations

## Key Business Context

This system implements Korean telecom billing with these specific requirements:
- Monthly fee calculation with complex pro-rating rules
- Support for service suspensions with configurable billing rates  
- B2B products with multiple pricing strategies
- TMForum specification compliance
- Historical data tracking for all billing factors

The core complexity lies in accurately segmenting billing periods when contracts, products, and service states change, then applying the correct pricing policy to each segment.

## Spring Batch Architecture

### Multi-threaded Processing Design
- **Reader**: Single-threaded data reading using MyBatisCursorItemReader wrapped in SynchronizedItemStreamReader
- **Processor**: Multi-threaded parallel processing via TaskExecutor (configurable via `threadCount` job parameter)
- **Writer**: Thread-safe batch writing with custom MonthlyFeeCalculationResultWriter using @Transactional
- **Chunk Size**: Configurable via constants, typically 100 items per chunk for optimal performance

### Batch Job Parameters
- **billingStartDate** (required): Billing period start date (YYYY-MM-DD format)
- **billingEndDate** (required): Billing period end date (YYYY-MM-DD format)  
- **contractId** (optional): Specific contractWithProductsAndSuspensions ID for single contractWithProductsAndSuspensions processing
- **threadCount** (optional): Number of threads for parallel processing (default: 8)

### MyBatis Configuration for Batch Processing

#### Dual Usage Pattern
MyBatis queries support conditional WHERE clauses for flexible usage:
- Web service: single contractWithProductsAndSuspensions queries with `contractId` parameter
- Batch processing: full dataset queries with `contractId = null`

#### Complex Data Reading Strategy
Current implementation uses MyBatisCursorItemReader for streaming large datasets:
- Cursor-based reading prevents memory overflow
- SynchronizedItemStreamReader ensures thread-safety in multi-threaded environment
- SQL fragment reuse (`contractSelectClause`, `contractFilterConditions`, `contractOrderByClause`)

#### Key Composite Keys for Proper Pagination
- contractWithProductsAndSuspensions key: contractId
- product key: contractId, productOfferingId, effectiveStartDateTime, effectiveEndDateTime
- suspension key: contractId, suspensionType, effectiveStartDateTime, effectiveEndDateTime
- ProductOffering key: productId, chargeItemId

#### Critical ORDER BY Requirements
ORDER BY clauses must maintain consistent sorting for proper pagination and MyBatis ResultMap grouping:
```sql
ORDER BY c.contract_id, po.product_offering_id, p.effective_start_date_time, 
         p.effective_end_date_time, mci.charge_item_id, s.suspension_type_code,
         s.effective_start_date_time, s.effective_end_date_time
```

### Batch Processing Considerations
- **ExecutorType Conflicts**: Avoid MyBatisPagingItemReader in multi-threaded environments due to ExecutorType.BATCH conflicts with existing transactions
- **Transaction Management**: Use Spring's declarative transaction management (@Transactional) in Writers, not manual SqlSession management
- **Memory Management**: Cursor-based readers prevent memory issues with large datasets
- **Thread Safety**: Always wrap non-thread-safe readers with SynchronizedItemStreamReader for multi-threaded processing

For detailed MyBatis paging usage, see `MYBATIS_PAGING_USAGE.md`

## Spring Batch Troubleshooting

### Common Multi-threading Issues
- **ExecutorType Conflicts**: `TransientDataAccessResourceException: Cannot change the ExecutorType when there is an existing transaction`
  - Cause: MyBatisPagingItemReader attempts to change ExecutorType to BATCH within existing transaction
  - Solution: Use MyBatisCursorItemReader instead, or implement custom pagination logic
- **Thread Safety**: Ensure all ItemReaders are wrapped with SynchronizedItemStreamReader for multi-threaded processing
- **Transaction Boundaries**: Multi-threaded processing means each thread manages its own transaction scope

### Dependency Injection Best Practices
- Use `@RequiredArgsConstructor` with Lombok instead of `@Autowired` field injection
- Ensure all batch components are `@StepScope` or `@JobScope` for proper parameter injection
- Job parameters are injected at runtime via SpEL expressions: `@Value("#{jobParameters['paramName']}")`

## Error Handling and Monitoring

### Batch Execution Monitoring
Spring Batch stores execution metadata in these tables:
- `BATCH_JOB_INSTANCE`: Job instance information
- `BATCH_JOB_EXECUTION`: Execution details and status
- `BATCH_STEP_EXECUTION`: Step-level execution metrics

### Performance Optimization
- **Chunk Size**: Balance between transaction size and memory usage (typically 100-1000)
- **Thread Count**: Configure via job parameter, usually 4-16 threads depending on system resources
- **Database Connection Pool**: Ensure adequate connections for multi-threaded processing
- **JVM Memory**: Set appropriate heap size for large dataset processing: `-Xmx2g`