# Business Domain Guide

This document provides detailed business domain and logic information for the Korean telecom billing system.

## Business Overview

This system implements Korean telecom billing with complex pro-rating rules and suspension handling, following TMForum specifications for telecommunication services.

## Key Business Components

### Unified Calculator Pattern
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

### Revenue Tracking and Caching
The system includes comprehensive revenue tracking capabilities:

1. **RevenueMasterData**: Domain entity for revenue item master data management
2. **RevenueMasterDataCacheService**: In-memory caching service with `@PostConstruct` initialization
3. **Revenue Item Integration**: ChargeItem includes `revenueItemId` for revenue tracking
4. **Caching Strategy**: Application startup loads all revenue master data into memory for performance
5. **Revenue Repository Pattern**: `RevenueMasterDataQueryPort` and repository implementation with DTO conversion

### Monthly Fee Calculation Flow
1. **CalculationCommandService**: Orchestrates multiple calculators using the unified pattern
2. **ProratedPeriodBuilder**: Splits billing periods based on contract changes, suspensions, and product changes
3. **Pricing Policies**: Strategy pattern for different pricing models via `DefaultMonthlyChargingPolicyFactory`
4. **Stream-based Processing**: Functional approach using flatMap for result aggregation

### Policy Strategy Pattern
The system uses strategy pattern for pricing policies via `DefaultMonthlyChargingPolicyFactory`:
- **FlatRatePolicy**: Standard flat rate billing
- **MatchingFactorPolicy**: B2B products with matching criteria
- **RangeFactorPolicy**: Range-based pricing
- **StepFactorPolicy**: Step-based pricing
- **TierFactorPolicy**: Tier-based pricing
- **UnitPriceFactorPolicy**: Unit price multiplication

## Business Rules Implementation

### Pro-rated Calculation Rules
- **Contract start date is included, end date is excluded**
- **Suspension periods**: Apply suspension billing rates defined per product
- **Period segmentation**: Overlap contract history with service status history for accurate billing
- **OCP principle**: New B2B products must be addable without modifying existing code
- **Calculation period**: Maximum one month (1st to end of month)

### Calculator Pattern Implementation
- All calculators must implement `Calculator<I>` interface
- Use `@Order` annotation to control execution sequence
- Implement all lifecycle methods: `read`, `process`, `write`, `post`
- Leverage default `execute` method with Stream API for consistent processing
- Use method references and functional interfaces where possible

## Domain Model Evolution

### Core Domain Entities
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

### ChargeItem Evolution (Major Refactoring)
The system underwent a major refactoring from MonthlyChargeItem to ChargeItem with revenue tracking:

- **ChargeItem**: Renamed from `MonthlyChargeItem` to support all charge types (monthly + one-time)
- **Revenue Integration**: Added `revenueItemId` field for comprehensive revenue tracking
- **Database Schema**: `monthly_charge_item` table renamed to `charge_item` with `revenue_item_id` column
- **MyBatis Updates**: All XML mappings updated to reflect new table and field names
- **Domain Consistency**: Unified charge item representation across monthly and one-time charges
- **DTO Alignment**: All DTOs updated to match new domain model structure

### Revenue Master Data Architecture
- **RevenueMasterData**: Domain record for revenue item master data (`revenueItemId`, `revenueItemName`, `revenueTypeCode`)
- **RevenueMasterDataDto**: Infrastructure DTO with database mapping
- **RevenueMasterDataConverter**: Conversion logic between domain and DTO layers
- **RevenueMasterDataCacheService**: Application service with in-memory caching and `@PostConstruct` initialization
- **Repository Pattern**: `RevenueMasterDataRepository` implementing `RevenueMasterDataQueryPort`

## Key Business Context

This system implements Korean telecom billing with these specific requirements:

### Core Billing Features
- **Monthly fee calculation** with complex pro-rating rules and suspension handling
- **One-time charges** including installation fees and device installment processing
- **Contract discounts** with rate-based and amount-based discount calculations
- **VAT calculation** based on existing results and revenue master data mapping
- **Service suspensions** with configurable billing rates and period-based calculations
- **B2B products** with multiple pricing strategies and dynamic policy selection
- **Calculation result prorating** for period segmentation and balance management
- **TMForum specification** compliance with extensible architecture
- **Historical data tracking** for all billing factors with audit trail support

### Core Complexity Areas
1. **Period Segmentation**: Accurately splitting billing periods when contracts, products, and service states change using CalculationResult prorate functionality
2. **Policy Application**: Applying the correct pricing policy to each calculated segment
3. **Data Orchestration**: Coordinating multiple data sources (contracts, products, suspensions, installations, installments, discounts)
4. **Calculation Unification**: Processing different charge types through a unified calculator pattern
5. **Discount Application**: Applying discounts to existing calculation results with balance tracking
6. **VAT Processing**: Calculating VAT on applicable revenue items with automated revenue mapping

## CalculationResult Prorate Functionality (2024-2025)

Implemented sophisticated period splitting and balance tracking in CalculationResult:

### Core Features
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
            BigDecimal prorationRatio = BigDecimal.valueOf(intersectionDays)
                .divide(BigDecimal.valueOf(totalDays), 10, RoundingMode.HALF_UP);

            return new CalculationResult<>(
                // Prorated calculation logic
            );
        })
        .filter(Objects::nonNull)
        .toList();
}
```

### Key Capabilities
- **Period Intersection Logic**: Calculates overlapping periods between billing and discount periods
- **Proportional Fee Calculation**: Applies pro-ration based on day counts with BigDecimal precision
- **Balance Tracking**: Maintains running balances with `addBalance()` and `subtractBalance()` methods
- **PostProcessor Integration**: Supports embedded post-processing logic via functional interfaces
- **Stream-based Processing**: Functional approach for clean period splitting operations

### Usage Patterns
- Used by DiscountCalculator for applying discounts to existing CalculationResults
- Integrates with VAT calculation for proportional tax application
- Supports complex billing scenarios with multiple overlapping periods

## OneTimeCharge Spring Bean Auto-Injection Architecture (2025)

Completed major refactoring to eliminate conditional logic and achieve complete extensibility using Spring DI patterns:

### Core Architecture Components
- **OneTimeChargeDomain**: Marker interface for all OneTimeCharge domain objects
- **OneTimeChargeCalculator<T extends OneTimeChargeDomain>**: Unified calculator interface
- **OneTimeChargeDataLoader<T extends OneTimeChargeDomain>**: Data loading abstraction
- Map-based automatic dependency injection eliminating all type-based conditional statements

### Key Infrastructure Changes
```java
// CalculationTarget evolved to Map structure
public record CalculationTarget(
    Long contractId,
    List<ContractWithProductsAndSuspensions> contractWithProductsAndSuspensions,
    Map<Class<? extends OneTimeChargeDomain>, List<? extends OneTimeChargeDomain>> oneTimeChargeData,
    List<Discount> discounts
) {}

// Automatic Map-based processing in services
private final Map<Class<? extends OneTimeChargeDomain>, OneTimeChargeCalculator<? extends OneTimeChargeDomain>>
        calculatorMap;
private final Map<Class<? extends OneTimeChargeDomain>, OneTimeChargeDataLoader<? extends OneTimeChargeDomain>>
        dataLoaderMap;
```

### Implemented Components
- **InstallationHistoryDataLoader**: Installation history data loading with `@Component` registration
- **DeviceInstallmentDataLoader**: Device installment data loading with `@Component` registration
- Enhanced existing calculators: `InstallationFeeCalculator`, `DeviceInstallmentCalculator` implementing dual interfaces
- Updated domain objects: `InstallationHistory`, `DeviceInstallmentMaster` implementing `OneTimeChargeDomain`

### Architectural Benefits
1. **Complete Conditional Logic Elimination**: No more type-based if/else statements in ChunkedContractReader or CalculationCommandService
2. **Zero-Code Extensibility**: Adding new OneTimeCharge types requires only creating 2-3 classes with `@Component` annotation
3. **Automatic Spring DI Integration**: Maps are auto-populated from registered beans using Stream collectors
4. **Type Safety**: Marker interfaces and generics ensure compile-time type verification
5. **Backward Compatibility**: Existing tests and APIs remain functional with compatibility methods

### Extension Pattern Example
```java
// New OneTimeCharge type - only these classes needed
@Component
public class MaintenanceFeeDataLoader implements OneTimeChargeDataLoader<MaintenanceFee> {
    // Implementation automatically integrated into processing pipeline
}

@Component
@Order(300)
public class MaintenanceFeeCalculator implements OneTimeChargeCalculator<MaintenanceFee> {
    // Automatically executed in sequence with other calculators
}

// Result: Zero modifications to existing ChunkedContractReader, CalculationCommandService, or CalculationTarget
```

### Performance and Maintainability Improvements
- **Map O(1) Lookup**: Eliminates sequential if-else chains for type resolution
- **Spring @Order Integration**: Uses standard Spring ordering instead of custom getOrder() methods
- **Reduced Code Duplication**: Single processing logic handles all OneTimeCharge types
- **Enhanced Testability**: Each calculator can be independently unit tested and conditionally enabled

## Development Guidelines for Business Logic

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

### Record Usage Guidelines
- **Immutable Data**: Use records for immutable data structures (`CalculationTarget`, `CalculationParameters`)
- **Parameter Objects**: Group related parameters into records for cleaner method signatures
- **Conversion Methods**: Add conversion methods like `toCalculationContext()` for seamless transformations
- **Builder Pattern Alternative**: Records can often replace complex builders for simple data structures

### Business Logic Best Practices
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
- **OneTimeCharge Extensibility**: New charge types must implement `OneTimeChargeDomain` marker interface and provide both DataLoader and Calculator components
- **Spring DI Best Practices**: Use `@Component` and `@Order` annotations instead of custom ordering methods; leverage automatic List injection and Map conversion
- **Conditional Logic Elimination**: Avoid type-based conditional statements; use Map-based automatic type resolution patterns
- **Marker Interface Design**: Use marker interfaces for type safety and compile-time verification in generic processing pipelines

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