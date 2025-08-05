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
```

## Architecture

### Module Structure
- **domain**: Core business logic with hexagonal architecture (no Spring Boot executable)
- **web-service**: REST API layer depending on domain
- **batch**: Batch processing layer depending on domain

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
3. **ProratedPeriodBuilder**: Splits billing periods based on contract changes, suspensions, and product changes
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
- **Period segmentation**: Overlap contract history with service status history for accurate billing
- **OCP principle**: New B2B products must be addable without modifying existing code
- **Calculation period**: Maximum one month (1st to end of month)

### Technology Stack
- Spring Boot 3.x with Java 17+
- JPA with QueryDSL for database queries
- JUnit 5 for testing (avoid mocking in domain tests)
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

## MyBatis SQL XML Configuration

### Key Considerations for Paging and Sorting in Spring Batch MyBatisPagingItemReader
- For MyBatis SQL XML, carefully define composite keys for different entities
- Ensure proper key configuration for pagination and sorting in batch processing:
  * contract key: contractId
  * product key: contractId, productOfferingId, effectiveStartDateTime, effectiveEndDateTime
  * suspension key: contractId, suspensionType, effectiveStartDateTime, effectiveEndDateTime
  * ProductOffering key: productId, chargeItemId
- When using MyBatisPagingItemReader, implement comprehensive paging and sorting logic
- Composite keys are crucial for accurate data retrieval in batch processes
- Recommend using multiple columns in order by and where clauses to ensure precise pagination