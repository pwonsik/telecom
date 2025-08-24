# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Overview

This is a Spring Boot 3-based telecom billing calculation system implementing hexagonal architecture for Korean telecommunication services. The system calculates monthly fees with complex pro-rated billing logic following TMForum specifications.

**Project Name**: `telecom-billing`  
**Group ID**: `me.realimpact.telecom.billing`  
**Version**: `0.0.1-SNAPSHOT`  
**Java Version**: 21  
**Spring Boot Version**: 3.2.4

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
- **domain**: Core business logic with hexagonal architecture (library JAR module, bootJar disabled)
  - Dependencies: Spring Data JPA, MyBatis, QueryDSL, MySQL Connector
  - Contains: Business entities, use cases, application services, infrastructure adapters
- **web-service**: REST API layer depending on domain
  - Dependencies: Spring Web, domain module
  - Database: H2 in-memory for testing/development
- **batch**: Spring Batch processing layer depending on domain for large-scale calculations
  - Dependencies: Spring Batch, MyBatis, domain module, MySQL Connector
  - Features: Multi-threaded processing, chunk-based processing, MySQL connection pooling
- **testgen**: Test data generation utility with JavaFaker
  - Dependencies: Spring Boot Starter, MyBatis, MySQL Connector, JavaFaker
  - Purpose: Generate realistic test data for development and testing

### Hexagonal Architecture
The domain module follows strict hexagonal architecture with clear package structure:
- `api/`: Inbound ports (use cases) - `CalculationCommandUseCase`
- `application/`: Application services implementing use cases - `CalculationCommandService`, calculators
- `domain/`: Core business entities and logic - `ContractWithProductsAndSuspensions`, policies, pricing models
- `infrastructure/`: Adapters and external integrations
  - `adapter/`: Repository implementations
  - `dto/`: Data transfer objects
  - `converter/`: Domain-DTO conversion logic
- `port/out/`: Outbound ports for external dependencies - `CalculationResultSavePort`, `ContractQueryPort`

### Key Business Components

#### Monthly Fee Calculation Flow
1. **CalculationCommandService**: Main application service implementing `CalculationCommandUseCase`
2. **BaseFeeCalculator**: Core calculator handling pro-rated billing logic and monthly fee calculations
3. **ProratedPeriodBuilder**: Splits billing periods based on contract changes, suspensions, and product changes
4. **Pricing Policies**: Strategy pattern for different pricing models implemented via `DefaultMonthlyChargingPolicyFactory`
5. **One-time Charge Calculators**: `DeviceInstallmentCalculator`, `InstallationFeeCalculator` for non-recurring charges

#### Policy Strategy Pattern
The system uses strategy pattern for pricing policies via `DefaultMonthlyChargingPolicyFactory`:
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
- **Spring Boot 3.2.4** with **Java 21**
- **JPA with QueryDSL 5.0.0** for domain entity queries
- **MyBatis 3.0.3** for complex SQL queries and batch processing with cursor-based reading
- **Spring Batch** for large-scale multi-threaded data processing
- **MySQL** as primary database with HikariCP connection pooling
- **H2** for web-service development/testing
- **JUnit 5** for testing (avoid mocking in domain tests)
- **Lombok** for reducing boilerplate code
- **JavaFaker** for test data generation
- **Hexagonal architecture** with clear separation of concerns

### Testing
- **Domain tests**: Avoid mocking, focus on business logic testing
- **Integration tests**: `MonthlyFeeCalculationIntegrationTest` for end-to-end scenarios
- **Policy tests**: Individual test classes for each pricing policy (`FlatRatePolicyTest`, `TierFactorPolicyTest`, etc.)
- **Converter tests**: `ContractDtoToDomainConverterTest` for data transformation logic
- **Test naming**: Use descriptive method names reflecting business scenarios
- **Coverage**: Test various pricing policy combinations and edge cases

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
- **Reader**: `ChunkedContractReader` for bulk contract reading, wrapped in `SynchronizedItemStreamReader` for thread safety
- **Processor**: `MonthlyFeeCalculationProcessor` with multi-threaded parallel processing via `TaskExecutor`
- **Writer**: `MonthlyFeeCalculationResultWriter` with thread-safe batch writing using `@Transactional`
- **Chunk Size**: Configured via `BatchConstants.CHUNK_SIZE`, typically 100 items per chunk
- **Thread Pool**: `ThreadPoolTaskExecutor` with configurable core/max pool sizes and queue capacity

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
Current implementation uses `ChunkedContractReader` for efficient bulk reading:
- Reads contract IDs in chunks, then bulk loads related data
- Prevents memory overflow with controlled batch sizes
- Uses `ContractQueryMapper` for complex SQL with joins and subqueries
- Supports both single contract processing (`contractId` parameter) and full dataset processing
- Integrates with `InstallationHistoryMapper` and `DeviceInstallmentMapper` for one-time charges

#### Key Composite Keys for Proper Data Grouping
- **Contract key**: `contractId`
- **Product key**: `contractId`, `productOfferingId`, `effectiveStartDateTime`, `effectiveEndDateTime`
- **Suspension key**: `contractId`, `suspensionType`, `effectiveStartDateTime`, `effectiveEndDateTime`  
- **ProductOffering key**: `productId`, `chargeItemId`
- **Device Installment key**: `contractId`, `deviceInstallmentId`
- **Installation History key**: `contractId`, `installationDate`

#### Critical ORDER BY Requirements
ORDER BY clauses must maintain consistent sorting for proper pagination and MyBatis ResultMap grouping:
```sql
ORDER BY c.contract_id, po.product_offering_id, p.effective_start_date_time, 
         p.effective_end_date_time, mci.charge_item_id, s.suspension_type_code,
         s.effective_start_date_time, s.effective_end_date_time
```

### Batch Processing Considerations
- **Reader Design**: Uses custom `ChunkedContractReader` instead of MyBatisPagingItemReader to avoid ExecutorType conflicts
- **Transaction Management**: Uses Spring's declarative `@Transactional` in Writers, avoiding manual SqlSession management
- **Memory Management**: Chunk-based processing with controlled batch sizes prevents memory issues
- **Thread Safety**: `SynchronizedItemStreamReader` wrapper ensures thread-safe reading in multi-threaded environment
- **Connection Pooling**: HikariCP configuration optimized for multi-threaded batch processing (max pool size: 20)

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
- **Chunk Size**: Configured via `BatchConstants.CHUNK_SIZE`, typically 100 for optimal performance
- **Thread Count**: Configurable via `threadCount` job parameter (default: 8 threads)
- **Connection Pool**: HikariCP optimized for batch processing
  - Maximum pool size: 20
  - Minimum idle: 8  
  - Connection timeout: 60s
- **MyBatis Configuration**: 
  - Default fetch size: 100
  - Statement timeout: 0 (unlimited for batch)
  - Cache disabled for batch processing
- **JVM Memory**: Recommended `-Xmx2g` for large dataset processing