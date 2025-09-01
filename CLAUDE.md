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
- Supports both full contract processing and single contract processing via `contractId` parameter
- Includes one-time charge processing for installation fees and device installments

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
  - `monthlyfee/`: Monthly fee calculation components - `BaseFeeCalculator`, `ProratedPeriodBuilder`
  - `onetimecharge/`: One-time charge calculators - `InstallationFeeCalculator`, `DeviceInstallmentCalculator`
- `domain/`: Core business entities and logic - `ContractWithProductsAndSuspensions`, policies, pricing models
- `infrastructure/`: Adapters and external integrations
  - `adapter/`: Repository implementations with separate `mybatis/` subpackage
  - `dto/`: Data transfer objects with converter classes
  - `converter/`: Domain-DTO conversion logic - `ContractDtoToDomainConverter`, `OneTimeChargeDtoConverter`
- `port/out/`: Outbound ports for external dependencies - `CalculationResultSavePort`, `ProductQueryPort`, `InstallationHistoryQueryPort`, `DeviceInstallmentQueryPort`

### Key Business Components

#### Unified Calculator Pattern
All calculators follow a standardized `Calculator<I>` interface pattern with consistent lifecycle methods:

1. **Calculator Interface**: Generic `Calculator<I>` with `read`, `process`, `write`, `post` methods and default `execute` implementation
2. **BaseFeeCalculator**: Monthly fee calculator implementing `Calculator<ContractWithProductsAndSuspensions>`
3. **InstallationFeeCalculator**: One-time installation fee calculator implementing `Calculator<InstallationHistory>`
4. **DeviceInstallmentCalculator**: Device installment calculator implementing `Calculator<DeviceInstallmentMaster>`
5. **DiscountCalculator**: Contract discount calculator implementing specialized discount processing
6. **VatCalculator**: VAT calculation based on existing CalculationResults and RevenueMasterData
7. **Order-based Execution**: Calculators use `@Order` annotation for controlled execution sequence
8. **CalculationTarget Record**: Unified data structure containing all calculation inputs including discounts
9. **CalculationContext**: Domain object encapsulating calculation parameters and context

#### Revenue Tracking and Caching
The system now includes comprehensive revenue tracking capabilities:

1. **RevenueMasterData**: Domain entity for revenue item master data management
2. **RevenueMasterDataCacheService**: In-memory caching service with `@PostConstruct` initialization
3. **Revenue Item Integration**: ChargeItem includes `revenueItemId` for revenue tracking
4. **Caching Strategy**: Application startup loads all revenue master data into memory for performance
5. **Revenue Repository Pattern**: `RevenueMasterDataQueryPort` and repository implementation with DTO conversion

#### Monthly Fee Calculation Flow
1. **CalculationCommandService**: Orchestrates multiple calculators using the unified pattern
2. **ProratedPeriodBuilder**: Splits billing periods based on contract changes, suspensions, and product changes
3. **Pricing Policies**: Strategy pattern for different pricing models via `DefaultMonthlyChargingPolicyFactory`
4. **Stream-based Processing**: Functional approach using flatMap for result aggregation

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
- Use **record** for DTOs and parameter objects (`CalculationTarget`, `CalculationParameters`)
- Use **Lombok** annotations to minimize boilerplate code (`@RequiredArgsConstructor`, `@Getter`, etc.)
- Follow clean code principles with well-named variables and methods
- JPA entities should have 'JpaEntity' suffix to avoid naming conflicts
- JpaRepository interfaces should have 'JpaRepository' suffix
- Prefer **Stream API** for collection processing and functional transformations

### Calculator Pattern Implementation
- All calculators must implement `Calculator<I>` interface
- Use `@Order` annotation to control execution sequence
- Implement all lifecycle methods: `read`, `process`, `write`, `post`
- Leverage default `execute` method with Stream API for consistent processing
- Use method references and functional interfaces where possible

### Business Rules Implementation
- **Pro-rated calculation**: Contract start date is included, end date is excluded
- **Suspension periods**: Apply suspension billing rates defined per product
- **Period segmentation**: Overlap contract history with service status history for accurate billing
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
- **Calculator tests**: Individual test classes for each calculator following the unified pattern
- **Policy tests**: Individual test classes for each pricing policy (`FlatRatePolicyTest`, `TierFactorPolicyTest`, etc.)
- **Converter tests**: `ContractDtoToDomainConverterTest`, `OneTimeChargeDtoConverter` for data transformation logic
- **Batch tests**: Test `CalculationTarget` processing and batch job parameter handling
- **Test naming**: Use descriptive method names reflecting business scenarios
- **Coverage**: Test various pricing policy combinations, calculator interactions, and edge cases

### Domain Model Evolution

#### Core Domain Entities
- **CalculationTarget**: Record containing all calculation inputs (`contractWithProductsAndSuspensions`, `installationHistories`, `deviceInstallmentMasters`, `discounts`)
- **CalculationParameters**: Record encapsulating batch job parameters with `toCalculationContext()` conversion method
- **CalculationContext**: Domain object representing calculation context with billing dates and parameters
- **CalculationResult**: Unified result object for all calculation types with prorate functionality and balance tracking
- **ContractWithProductsAndSuspensions**: Main domain entity (evolved from `Contract`) with product and suspension relationships
- **InstallationHistory**: Domain object for installation fee calculations
- **DeviceInstallmentMaster**: Domain object for device installment calculations
- **Discount**: Domain object for individual discount information with contractId integration
- **ContractDiscounts**: Domain object managing contract-level discount collections
- **PostProcessor**: Functional interface for embedded post-processing logic in CalculationResults

#### ChargeItem Evolution (Major Refactoring)
The system underwent a major refactoring from MonthlyChargeItem to ChargeItem with revenue tracking:

- **ChargeItem**: Renamed from `MonthlyChargeItem` to support all charge types (monthly + one-time)
- **Revenue Integration**: Added `revenueItemId` field for comprehensive revenue tracking
- **Database Schema**: `monthly_charge_item` table renamed to `charge_item` with `revenue_item_id` column
- **MyBatis Updates**: All XML mappings updated to reflect new table and field names
- **Domain Consistency**: Unified charge item representation across monthly and one-time charges
- **DTO Alignment**: All DTOs updated to match new domain model structure

#### Revenue Master Data Architecture
- **RevenueMasterData**: Domain record for revenue item master data (`revenueItemId`, `revenueItemName`, `revenueTypeCode`)
- **RevenueMasterDataDto**: Infrastructure DTO with database mapping
- **RevenueMasterDataConverter**: Conversion logic between domain and DTO layers
- **RevenueMasterDataCacheService**: Application service with in-memory caching and `@PostConstruct` initialization
- **Repository Pattern**: `RevenueMasterDataRepository` implementing `RevenueMasterDataQueryPort`

## Key Business Context

This system implements Korean telecom billing with these specific requirements:
- **Monthly fee calculation** with complex pro-rating rules and suspension handling
- **One-time charges** including installation fees and device installment processing
- **Contract discounts** with rate-based and amount-based discount calculations
- **VAT calculation** based on existing results and revenue master data mapping
- **Service suspensions** with configurable billing rates and period-based calculations
- **B2B products** with multiple pricing strategies and dynamic policy selection
- **Calculation result prorating** for period segmentation and balance management
- **TMForum specification** compliance with extensible architecture
- **Historical data tracking** for all billing factors with audit trail support

The core complexity lies in:
1. **Period Segmentation**: Accurately splitting billing periods when contracts, products, and service states change using CalculationResult prorate functionality
2. **Policy Application**: Applying the correct pricing policy to each calculated segment
3. **Data Orchestration**: Coordinating multiple data sources (contracts, products, suspensions, installations, installments, discounts)
4. **Calculation Unification**: Processing different charge types through a unified calculator pattern
5. **Discount Application**: Applying discounts to existing calculation results with balance tracking
6. **VAT Processing**: Calculating VAT on applicable revenue items with automated revenue mapping

## Spring Batch Architecture

### Multi-threaded Processing Design
- **Reader**: `ChunkedContractReader` reads contract IDs and builds `CalculationTarget` objects, wrapped in `SynchronizedItemStreamReader` for thread safety
- **Processor**: `CalculationProcessor` processes `CalculationTarget` using multiple calculators with unified interface
- **Writer**: `CalculationWriter` with thread-safe batch writing using `@Transactional`
- **Chunk Size**: Configured via `BatchConstants.CHUNK_SIZE`, typically 100 items per chunk
- **Thread Pool**: `ThreadPoolTaskExecutor` with configurable core/max pool sizes and queue capacity
- **Calculator Orchestration**: Multiple calculators executed in sequence based on `@Order` annotation

### Batch Job Parameters
- **billingStartDate** (required): Billing period start date (YYYY-MM-DD format)
- **billingEndDate** (required): Billing period end date (YYYY-MM-DD format)  
- **contractId** (optional): Specific contract ID for single contract processing
- **billingCalculationType** (required): Type of billing calculation (e.g., MONTHLY_FEE)
- **billingCalculationPeriod** (required): Period for calculation (e.g., MONTHLY)
- **threadCount** (optional): Number of threads for parallel processing (default: 8)

### MyBatis Configuration for Batch Processing

#### Dual Usage Pattern
MyBatis queries support conditional WHERE clauses for flexible usage:
- Web service: single contract queries with `contractId` parameter
- Batch processing: full dataset queries with `contractId = null`

#### Enhanced Batch Processing Architecture
Current implementation uses unified data processing with `CalculationTarget`:
- **ChunkedContractReader**: Reads contract IDs in chunks, loads all related data types including discounts
- **CalculationTarget**: Record containing `contractWithProductsAndSuspensions`, `installationHistories`, `deviceInstallmentMasters`, `discounts`
- **CalculationResultProrater**: Specialized service for prorating calculation results based on discount periods
- **Integrated Processing Flow**: Sequential processing through multiple calculators with discount and VAT calculation
- **ProductQueryMapper**: Renamed from `ContractQueryMapper`, handles complex contract and product data
- **Specialized Mappers**: `InstallationHistoryMapper`, `DeviceInstallmentMapper` for one-time charges
- **Unified Processing**: Single `CalculationProcessor` handles multiple calculation types
- **CalculationParameters**: Record encapsulating batch job parameters with `toCalculationContext()` method
- **BatchConstants**: Centralized configuration for chunk sizes and processing parameters

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
         p.effective_end_date_time, ci.charge_item_id, s.suspension_type_code,
         s.effective_start_date_time, s.effective_end_date_time
```

Note: Updated from `mci.charge_item_id` to `ci.charge_item_id` following the MonthlyChargeItem → ChargeItem refactoring.

### Batch Processing Considerations
- **Reader Design**: Uses custom `ChunkedContractReader` with `CalculationTarget` construction instead of MyBatisPagingItemReader to avoid ExecutorType conflicts
- **Transaction Management**: Uses Spring's declarative `@Transactional` in Writers, avoiding manual SqlSession management
- **Memory Management**: Chunk-based processing with controlled batch sizes prevents memory issues
- **Thread Safety**: `SynchronizedItemStreamReader` wrapper ensures thread-safe reading in multi-threaded environment
- **Connection Pooling**: HikariCP configuration optimized for multi-threaded batch processing (max pool size: 20)
- **Data Loading Strategy**: Bulk loading of related data (products, suspensions, installations, installments) in single query operations
- **Calculator Integration**: Seamless integration of multiple calculators through unified interface pattern

### Infrastructure Layer Updates
- **MyBatis Mapper Organization**: All mappers moved to `infrastructure.adapter.mybatis` subpackage
- **ProductQueryMapper**: Renamed from `ContractQueryMapper` with enhanced complex SQL queries for contract and product data
- **Specialized Mappers**: `InstallationHistoryMapper`, `DeviceInstallmentMapper` for one-time charge data
- **Converter Classes**: `ContractDtoToDomainConverter`, `OneTimeChargeDtoConverter` for clean DTO-to-domain transformation
- **Repository Adaptation**: Updated repository implementations to work with new port interfaces

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

## New Development Patterns and Best Practices

### Stream API Usage Patterns
- **Functional Processing**: Prefer Stream API with `flatMap` for processing collections of calculation results
- **Method References**: Use method references (`Calculator::process`, `List::stream`) for cleaner code
- **Null-Safe Operations**: Use `Optional` and null-safe stream operations for robust data processing

### Generic Helper Methods
For repetitive collection processing patterns, use generic helper methods:
```java
private <T> void processAndAddResults(
    Collection<T> items,
    BiFunction<CalculationContext, T, List<CalculationResult>> processor,
    CalculationContext context,
    List<CalculationResult> results
) {
    results.addAll(
        items.stream()
            .flatMap(item -> processor.apply(context, item).stream())
            .toList()
    );
}
```

### Calculator Pattern Best Practices
- **Interface Compliance**: Always implement the complete `Calculator<I>` interface
- **Order Management**: Use `@Order` annotation for controlled execution sequence
- **Default Method Leverage**: Use the default `execute` method unless custom logic is required
- **Stream-based Processing**: Prefer functional approaches over imperative loops
- **Context Passing**: Always pass `CalculationContext` for consistent parameter access

### Record Usage Guidelines
- **Immutable Data**: Use records for immutable data structures (`CalculationTarget`, `CalculationParameters`)
- **Parameter Objects**: Group related parameters into records for cleaner method signatures
- **Conversion Methods**: Add conversion methods like `toCalculationContext()` for seamless transformations
- **Builder Pattern Alternative**: Records can often replace complex builders for simple data structures

### MyBatis Mapper Patterns
- **Consistent Naming**: Use descriptive method names that reflect business intent
- **Bulk Operations**: Design queries to support both single-item and bulk processing
- **ResultMap Organization**: Structure complex ResultMaps with proper key definitions for data grouping
- **SQL Fragment Reuse**: Use `<sql>` fragments for reusable query parts (SELECT, WHERE, ORDER BY clauses)

## Recent Major Changes (2024-2025)

### ChargeItem Refactoring
Completed major refactoring from MonthlyChargeItem to ChargeItem with revenue tracking:

**Database Changes:**
```sql
-- Table renamed and enhanced
RENAME TABLE monthly_charge_item TO charge_item;
ALTER TABLE charge_item ADD COLUMN revenue_item_id VARCHAR(50) NOT NULL COMMENT '수익 항목 ID';
```

**Domain Model Changes:**
- `MonthlyChargeItem` → `ChargeItem` class rename
- Added `revenueItemId` field for revenue tracking
- Updated all constructor calls and field references
- Unified charge item representation across calculation types

**Infrastructure Updates:**
- `MonthlyChargeItemDto` → `ChargeItemDto` with revenue fields
- MyBatis XML mappings updated to new table and column names
- All repository implementations updated
- Test data and sample data updated

### RevenueMasterData Implementation
Added comprehensive revenue master data management:

**New Components:**
- `RevenueMasterData` domain record
- `RevenueMasterDataDto` infrastructure DTO
- `RevenueMasterDataConverter` conversion logic
- `RevenueMasterDataCacheService` with `@PostConstruct` caching
- `RevenueMasterDataRepository` with port implementation
- Database table: `revenue_master_data`

**Caching Strategy:**
```java
@PostConstruct
public void initCache() {
    List<RevenueMasterData> allData = revenueMasterDataQueryPort.findAll();
    cache.clear();
    allData.forEach(data -> cache.put(data.revenueItemId(), data));
    log.info("Revenue master data cache initialized with {} items", cache.size());
}
```

### Discount Calculation Architecture (2024-2025)
Added comprehensive discount functionality following hexagonal architecture pattern:

**New Components:**
- `Discount` domain object with contractId integration and rate/amount-based calculations
- `ContractDiscounts` domain object managing discount collections per contract
- `DiscountCalculator` implementing `Calculator<Discount>` interface
- `ContractDiscountCommandPort` and `ContractDiscountQueryPort` for port abstraction
- `ContractDiscountRepository` implementing both query and command ports
- `ContractDiscountMapper` MyBatis interface with complex discount queries
- `ContractDiscountDto` and `DiscountDto` infrastructure DTOs
- `ContractDiscountDtoConverter` for domain-DTO conversions

**Database Schema:**
```sql
CREATE TABLE contract_discount (
    contract_id BIGINT NOT NULL COMMENT '계약 ID',
    discount_id VARCHAR(50) NOT NULL COMMENT '할인 ID', 
    discount_start_date DATE NOT NULL COMMENT '할인 시작일',
    discount_end_date DATE NOT NULL COMMENT '할인 종료일',
    product_offering_id VARCHAR(50) COMMENT '상품 제공 ID',
    discount_aply_unit VARCHAR(20) NOT NULL COMMENT '할인 적용 단위',
    discount_amt DECIMAL(12,2) COMMENT '할인 금액',
    discount_rate DECIMAL(5,2) COMMENT '할인율',
    discount_applied_amount DECIMAL(12,2) COMMENT '할인 적용 금액',
    PRIMARY KEY (contract_id, discount_id, discount_start_date, discount_end_date)
);
```

**Key Features:**
- Composite primary key with `contractId`, `discountId`, `discountStartDate`, `discountEndDate`
- Support for both rate-based (RATE) and amount-based (AMOUNT) discounts
- Period-based discount application with billing period intersection logic
- Integration with existing `CalculationTarget` and batch processing flow
- MyBatis ResultMap with nested discount collections for complex queries

### CalculationResult Prorate Functionality (2024-2025)
Implemented sophisticated period splitting and balance tracking in CalculationResult:

**Core Features:**
```java
public List<CalculationResult<?>> prorate(List<DefaultPeriod> periods) {
    return periods.stream()
        .map(period -> {
            LocalDate intersectionStart = billingPeriod.getStartDate().isAfter(period.getStartDate()) 
                ? billingPeriod.getStartDate() : period.getStartDate();
            LocalDate intersectionEnd = billingPeriod.getEndDate().isBefore(period.getEndDate()) 
                ? billingPeriod.getEndDate() : period.getEndDate();
            
            if (!intersectionStart.isBefore(intersectionEnd)) {
                return null; // No intersection
            }
            
            long totalDays = ChronoUnit.DAYS.between(billingPeriod.getStartDate(), billingPeriod.getEndDate());
            long intersectionDays = ChronoUnit.DAYS.between(intersectionStart, intersectionEnd);
            BigDecimal prорationRatio = BigDecimal.valueOf(intersectionDays)
                .divide(BigDecimal.valueOf(totalDays), 10, RoundingMode.HALF_UP);
            
            return new CalculationResult<>(
                // Prorated calculation logic
            );
        })
        .filter(Objects::nonNull)
        .toList();
}
```

**Key Capabilities:**
- **Period Intersection Logic**: Calculates overlapping periods between billing and discount periods
- **Proportional Fee Calculation**: Applies pro-ration based on day counts with BigDecimal precision
- **Balance Tracking**: Maintains running balances with `addBalance()` and `subtractBalance()` methods
- **PostProcessor Integration**: Supports embedded post-processing logic via functional interfaces
- **Stream-based Processing**: Functional approach for clean period splitting operations

**Usage Patterns:**
- Used by DiscountCalculator for applying discounts to existing CalculationResults
- Integrates with VAT calculation for proportional tax application
- Supports complex billing scenarios with multiple overlapping periods

### VAT Calculation Integration (2024-2025)
Added VAT processing capabilities with revenue master data integration:

**New Components:**
- `VatCalculator` implementing `Calculator<CalculationResult<?>>` interface
- Revenue-based VAT determination using `RevenueMasterDataCacheService`
- Integration with existing `CalculationResult` prorate functionality
- Automated VAT rate application based on revenue item classification

**VAT Processing Flow:**
1. **Revenue Lookup**: Query cached revenue master data for VAT eligibility
2. **Rate Application**: Apply appropriate VAT rates based on revenue type
3. **Period Prorating**: Use `CalculationResult.prorate()` for period-specific VAT calculation
4. **Result Integration**: Add VAT results to overall calculation result collection

**Integration Points:**
- Leverages existing `RevenueMasterDataCacheService` for performance
- Uses `PostProcessor` functional interface for embedded VAT logic
- Integrates with Spring Batch processing via unified Calculator pattern
- Maintains audit trail with balance tracking in CalculationResult

### Development Guidelines Updates
- **Jakarta EE Migration**: Use `jakarta.annotation.PostConstruct` instead of `javax.annotation.PostConstruct` for Spring Boot 3 compatibility
- **Revenue Integration**: All charge items must include valid revenue item IDs
- **Caching Best Practices**: Use `@PostConstruct` for application startup data loading
- **Domain Model Consistency**: Maintain unified charge item structure across all calculation types
- **Discount Architecture**: Follow hexagonal pattern with proper port abstraction for discount functionality
- **Period Calculations**: Use `CalculationResult.prorate()` for all period-based calculations and balance management
- **Composite Keys**: Design database schemas with proper composite keys for data integrity
- **Functional Interfaces**: Leverage `PostProcessor` for embedded business logic in calculation results
- **BigDecimal Precision**: Use appropriate scale and rounding modes for financial calculations
- **Test Tolerance**: Use `isCloseTo()` with tolerance for BigDecimal assertions in tests