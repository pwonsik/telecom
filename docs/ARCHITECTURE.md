# Architecture Guide

This document provides detailed technical architecture information for the telecom billing system.

## Module Structure

### domain (Core Business Logic)
- **Purpose**: Core business logic with hexagonal architecture (library JAR module, bootJar disabled)
- **Dependencies**: Spring Data JPA, MyBatis, QueryDSL, MySQL Connector
- **Contains**: Business entities, use cases, application services, infrastructure adapters

### web-service (REST API Layer)
- **Purpose**: REST API layer depending on domain
- **Dependencies**: Spring Web, SpringDoc OpenAPI, MyBatis, domain module
- **Database**: MySQL for production, H2 in-memory for testing/development
- **Features**: REST API endpoints, Swagger documentation, global exception handling

### batch (Spring Batch Processing)
- **Purpose**: Spring Batch processing layer for large-scale calculations
- **Dependencies**: Spring Batch, MyBatis, domain module, MySQL Connector
- **Features**: Multi-threaded processing, chunk-based processing, MySQL connection pooling

### testgen (Test Data Generation)
- **Purpose**: Test data generation utility with JavaFaker
- **Dependencies**: Spring Boot Starter, MyBatis, MySQL Connector, JavaFaker
- **Features**: Generate realistic test data for development and testing

## Hexagonal Architecture

The domain module follows strict hexagonal architecture with clear package structure:

### Package Structure
- **`api/`**: Inbound ports (use cases) - `CalculationCommandUseCase`
- **`application/`**: Application services implementing use cases - `CalculationCommandService`, calculators
  - `monthlyfee/`: Monthly fee calculation components - `BaseFeeCalculator`, `ProratedPeriodBuilder`
  - `onetimecharge/`: One-time charge calculators - `InstallationFeeCalculator`, `DeviceInstallmentCalculator`
- **`domain/`**: Core business entities and logic - `ContractWithProductsAndSuspensions`, policies, pricing models
- **`infrastructure/`**: Adapters and external integrations
  - `adapter/`: Repository implementations with separate `mybatis/` subpackage
  - `dto/`: Data transfer objects with converter classes
  - `converter/`: Domain-DTO conversion logic - `ContractDtoToDomainConverter`, `OneTimeChargeDtoConverter`
- **`port/out/`**: Outbound ports for external dependencies - `CalculationResultSavePort`, `ProductQueryPort`, `InstallationHistoryQueryPort`, `DeviceInstallmentQueryPort`

## Technology Stack

### Core Technologies
- **Spring Boot 3.2.4** with **Java 21**
- **JPA with QueryDSL 5.0.0** for domain entity queries
- **MyBatis 3.0.3** for complex SQL queries and batch processing with cursor-based reading
- **Spring Batch** for large-scale multi-threaded data processing
- **MySQL** as primary database with HikariCP connection pooling

### API & Documentation
- **SpringDoc OpenAPI 3** (`springdoc-openapi-starter-webmvc-ui:2.3.0`) for API documentation
- **Jakarta Validation** for request validation and data binding

### Testing & Development
- **H2** for web-service development/testing
- **JUnit 5** for testing (avoid mocking in domain tests)
- **Lombok** for reducing boilerplate code
- **JavaFaker** for test data generation

## Spring Batch Architecture

### Dual Batch Processing Architecture (2025)
The system supports two parallel batch processing approaches for performance comparison and optimization:

#### 1. Thread Pool Architecture (Original)
- **Job Name**: `monthlyFeeCalculationJob`
- **Execution**: `./run-batch-jar.sh`
- **Reader**: `ChunkedContractReader` wrapped in `SynchronizedItemStreamReader` for thread safety
- **Characteristics**: Dynamic work distribution, memory sharing across threads
- **Thread Pool**: `ThreadPoolTaskExecutor` with configurable core/max pool sizes and queue capacity

#### 2. Partitioner Architecture (New)
- **Job Name**: `partitionedMonthlyFeeCalculationJob`
- **Execution**: `./run-partitioned-batch-jar.sh`
- **Reader**: `PartitionedContractReader` with partition-specific data loading
- **Characteristics**: Static work distribution using `contractId % threadCount = partitionKey`
- **Master-Worker Pattern**: `partitionedMasterStep` orchestrates `partitionedWorkerStep` execution

### Common Components
- **Processor**: `CalculationProcessor` processes `CalculationTarget` using multiple calculators with unified interface
- **Writer**: `CalculationWriter` with thread-safe batch writing using `@Transactional`
- **Chunk Size**: Configured via `BatchConstants.CHUNK_SIZE`, typically 100 items per chunk
- **Calculator Orchestration**: Multiple calculators executed in sequence based on `@Order` annotation

### Batch Job Parameters
- **billingStartDate** (required): Billing period start date (YYYY-MM-DD format)
- **billingEndDate** (required): Billing period end date (YYYY-MM-DD format)
- **contractId** (optional): Specific contract ID for single contract processing
- **billingCalculationType** (required): Type of billing calculation (e.g., MONTHLY_FEE)
- **billingCalculationPeriod** (required): Period for calculation (e.g., MONTHLY)
- **threadCount** (optional): Number of threads for parallel processing (default: 8)

## MyBatis Configuration

### Dual Usage Pattern
MyBatis queries support conditional WHERE clauses for flexible usage:
- Web service: single contract queries with `contractId` parameter
- Batch processing: full dataset queries with `contractId = null`

### Enhanced Batch Processing Architecture
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

### Key Composite Keys for Proper Data Grouping
- **Contract key**: `contractId`
- **Product key**: `contractId`, `productOfferingId`, `effectiveStartDateTime`, `effectiveEndDateTime`
- **Suspension key**: `contractId`, `suspensionType`, `effectiveStartDateTime`, `effectiveEndDateTime`
- **ProductOffering key**: `productId`, `chargeItemId`
- **Device Installment key**: `contractId`, `deviceInstallmentId`
- **Installation History key**: `contractId`, `installationDate`

### Critical ORDER BY Requirements
ORDER BY clauses must maintain consistent sorting for proper pagination and MyBatis ResultMap grouping:
```sql
ORDER BY c.contract_id, po.product_offering_id, p.effective_start_date_time,
         p.effective_end_date_time, ci.charge_item_id, s.suspension_type_code,
         s.effective_start_date_time, s.effective_end_date_time
```

## Development Rules

### Code Style
- Use **record** for DTOs and parameter objects (`CalculationTarget`, `CalculationParameters`)
- Use **Lombok** annotations to minimize boilerplate code (`@RequiredArgsConstructor`, `@Getter`, etc.)
- Follow clean code principles with well-named variables and methods
- JPA entities should have 'JpaEntity' suffix to avoid naming conflicts
- JpaRepository interfaces should have 'JpaRepository' suffix
- Prefer **Stream API** for collection processing and functional transformations

### Testing Guidelines
- **Domain tests**: Avoid mocking, focus on business logic testing
- **Integration tests**: `MonthlyFeeCalculationIntegrationTest` for end-to-end scenarios
- **Calculator tests**: Individual test classes for each calculator following the unified pattern
- **Policy tests**: Individual test classes for each pricing policy (`FlatRatePolicyTest`, `TierFactorPolicyTest`, etc.)
- **Converter tests**: `ContractDtoToDomainConverterTest`, `OneTimeChargeDtoConverter` for data transformation logic
- **Batch tests**: Test `CalculationTarget` processing and batch job parameter handling
- **Test naming**: Use descriptive method names reflecting business scenarios
- **Coverage**: Test various pricing policy combinations, calculator interactions, and edge cases

## Performance Optimization

### Thread Pool vs Partitioner Comparison
| Aspect | Thread Pool | Partitioner |
|--------|-------------|-------------|
| **Work Distribution** | Dynamic (SynchronizedItemStreamReader) | Static (Modulo-based) |
| **Memory Usage** | Shared memory across threads | Independent partition memory |
| **Load Balancing** | Runtime work stealing | Predetermined data distribution |
| **Scalability** | Limited by synchronization overhead | Linear scaling per partition |
| **Data Locality** | Mixed contract processing | Partition-specific processing |

### Common Settings
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

## Web Service Module Architecture

### Exception Handling Architecture

**GlobalExceptionHandler:**
- `@ControllerAdvice` for centralized exception handling
- Structured `ErrorResponse` record for consistent error format
- Specific handlers for validation, binding, and business logic errors
- Detailed field-level validation error reporting

### Controller Development Patterns

**Standard Controller Template:**
```java
@Tag(name = "API Name", description = "API Description")
@RestController
@RequestMapping("/api/endpoint")
@RequiredArgsConstructor
@Validated
@Slf4j
public class ExampleController {

    @Operation(summary = "Operation summary", description = "Detailed description")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Success"),
        @ApiResponse(responseCode = "400", description = "Bad Request"),
        @ApiResponse(responseCode = "500", description = "Internal Server Error")
    })
    @PostMapping
    public ResponseEntity<ResponseType> methodName(
        @Parameter(description = "Parameter description", required = true)
        @Valid @RequestBody RequestType request) {
        // Implementation
    }
}
```

### Request/Response Design Patterns

**Request Record Pattern:**
```java
public record CalculationRequest(
    @NotEmpty(message = "계약 ID 목록은 비어있을 수 없습니다")
    List<Long> contractIds,

    @NotNull(message = "청구 시작일은 필수입니다")
    LocalDate billingStartDate,

    // other validated fields
) {}
```

**Response Wrapper Pattern:**
```java
public record CalculationResultGroup(
    List<CalculationResult<?>> calculationResults
) {}
```

### Error Handling Best Practices

**Structured Error Response:**
```java
public record ErrorResponse(
    String errorCode,
    String message,
    Map<String, String> fieldErrors,
    String path,
    LocalDateTime timestamp
) {}
```

**Exception Handling Strategy:**
- Use `@ControllerAdvice` for global exception handling
- Provide specific handlers for different exception types
- Include field-level validation errors in responses
- Log errors with appropriate levels (WARN for client errors, ERROR for server errors)

## Multi-Module MyBatis Setup

### Domain Module Configuration
- Contains all MyBatis Mapper XML files in `src/main/resources/mapper/`
- Provides MyBatis configuration in `application.yml`
- Type aliases package: `me.realimpact.telecom.calculation.infrastructure.dto`

### Web Service Module Configuration
- Uses `@MapperScan("me.realimpact.telecom.calculation.infrastructure.adapter.mybatis")`
- Inherits domain module MyBatis configuration via dependency
- MySQL connection with HikariCP optimization for production
- H2 in-memory database for development/testing