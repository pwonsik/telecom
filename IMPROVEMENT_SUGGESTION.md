# `ProductQueryPortResolver` 개선 제안

## 설명

이 개선안의 핵심 목표는 `ProductQueryPortResolver` 클래스를 수정하지 않고도 새로운 `ProductQueryPort` 구현체를 시스템에 추가할 수 있도록 만드는 것입니다.

### 현재 설계의 문제점

기존 `ProductQueryPortResolver`는 생성자에서 `@Qualifier`를 사용하여 특정 `ProductQueryPort` 구현체(`previewProductQueryPort`, `defaultProductQueryPort`)를 직접 주입받습니다.

```java
// 기존 ProductQueryPortResolver 생성자
public ProductQueryPortResolver(
        @Qualifier("preview_product_query_repository") ProductQueryPort previewProductQueryPort,
        @Qualifier("default_product_query_repository") ProductQueryPort defaultProductQueryPort) {
    
    this.defaultQueryPort = defaultQueryPort;
    this.queryPortMap.put(BillingCalculationType.PREVIEW_INQUIRY, previewProductQueryPort);
}
```

이 방식의 문제점은 다음과 같습니다.

1.  **확장성 부족**: 새로운 `BillingCalculationType`과 그에 해당하는 `ProductQueryPort` 구현체가 추가될 때마다 `ProductQueryPortResolver` 클래스의 생성자 코드를 수정해야 합니다.
2.  **유지보수 어려움**: `Port`가 추가되거나 변경될 때마다 `Resolver`의 코드를 변경해야 하므로, 코드가 복잡해지고 오류 발생 가능성이 높아집니다. 이는 OCP(Open-Closed Principle) 원칙에 위배됩니다. OCP는 "소프트웨어 개체(클래스, 모듈, 함수 등)는 확장에 대해 열려 있어야 하고, 수정에 대해서는 닫혀 있어야 한다"는 원칙입니다.

### 제안하는 해결책

이 문제를 해결하기 위해, Spring의 의존성 주입(Dependency Injection)과 전략 패턴(Strategy Pattern)을 활용하는 새로운 접근 방식을 제안합니다.

1.  **`ProductQueryPort` 인터페이스 변경**:
    `ProductQueryPort` 인터페이스에 `getBillingCalculationType()` 메서드를 추가합니다. 이 메서드는 각 `Port` 구현체가 어떤 `BillingCalculationType`을 처리하는지를 알려주는 역할을 합니다.

    ```java
    public interface ProductQueryPort {
        // ... 기존 메서드
        BillingCalculationType getBillingCalculationType();
    }
    ```

2.  **`ProductQueryPort` 구현체 변경**:
    `ProductQueryRepository`와 `PreviewProductQueryRepository`는 `getBillingCalculationType()` 메서드를 구현하여 각각 자신이 처리하는 `BillingCalculationType`을 반환합니다.

    ```java
    // ProductQueryRepository.java
    @Override
    public BillingCalculationType getBillingCalculationType() {
        return BillingCalculationType.REVENUE_CONFIRMATION; // 예시: 매출 확정 타입 처리
    }

    // PreviewProductQueryRepository.java
    @Override
    public BillingCalculationType getBillingCalculationType() {
        return BillingCalculationType.PREVIEW_INQUIRY; // 미리보기 타입 처리
    }
    ```

3.  **`ProductQueryPortResolver` 리팩토링**:
    `ProductQueryPortResolver`의 생성자에서 `ProductQueryPort`의 `List`를 주입받도록 변경합니다. Spring은 `@Component`나 `@Repository` 같은 어노테이션이 붙은 모든 `ProductQueryPort` 구현체를 자동으로 찾아서 이 `List`에 주입해 줍니다.

    ```java
    // 개선된 ProductQueryPortResolver 생성자
    public ProductQueryPortResolver(
            List<ProductQueryPort> queryPorts,
            @Qualifier("default_product_query_repository") ProductQueryPort defaultQueryPort) {
        
        // 주입받은 Port 리스트를 Map으로 변환
        this.queryPortMap = queryPorts.stream()
                .filter(port -> port.getBillingCalculationType() != null)
                .collect(Collectors.toMap(ProductQueryPort::getBillingCalculationType, Function.identity()));
        
        this.defaultQueryPort = defaultQueryPort;
    }
    ```
    생성자에서는 이 `List`를 순회하며 `getBillingCalculationType()` 메서드를 호출하고, `BillingCalculationType`을 키(key)로, `ProductQueryPort` 구현체를 값(value)으로 하는 `Map`을 만듭니다.

### 개선된 설계의 장점

*   **확장성 향상**: 새로운 `ProductQueryPort` 구현체를 추가할 때, 새로운 클래스를 만들고 `ProductQueryPort` 인터페이스를 구현하기만 하면 됩니다. `ProductQueryPortResolver`의 코드는 전혀 수정할 필요가 없습니다. Spring이 알아서 새로운 구현체를 찾아 `Resolver`에 주입해 줍니다.
*   **유지보수 용이성**: `Resolver`는 더 이상 개별 `Port` 구현체에 대해 알 필요가 없습니다. 단지 `ProductQueryPort`라는 인터페이스에만 의존하므로, 코드의 결합도(coupling)가 낮아지고, 각 컴포넌트를 독립적으로 이해하고 수정하기 쉬워집니다.
*   **OCP 원칙 준수**: 기존 코드를 수정하지 않고도 시스템의 기능을 확장할 수 있으므로 OCP 원칙을 잘 따르는 설계가 됩니다.

이러한 변경을 통해 `ProductQueryPortResolver`는 더욱 유연하고 확장 가능한 구조를 갖게 되어, 앞으로 새로운 요금 계산 정책이나 조회 유형이 추가되더라도 손쉽게 대응할 수 있습니다.

---

## 기존 제안 내용

`ProductQueryPortResolver` 클래스와 관련 구성 요소를 분석했습니다. 새로운 포트를 추가할 때 소스 코드를 수정할 필요 없이 확장성을 개선하기 위해 다음과 같은 변경을 제안합니다.

먼저, 참고용으로 기존 `ProductQueryPortResolver.java` 파일입니다.
```java
package me.realimpact.telecom.calculation.infrastructure.adapter;

import lombok.extern.slf4j.Slf4j;
import me.realimpact.telecom.calculation.api.BillingCalculationType;
import me.realimpact.telecom.calculation.port.out.ProductQueryPort;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * ProductQueryPort 구현체들을 BillingCalculationType별로 관리하고 적절한 구현체를 반환하는 Resolver
 */
@Component
@Slf4j
public class ProductQueryPortResolver {
    
    private final Map<BillingCalculationType, ProductQueryPort> queryPortMap = new HashMap<>();
    private final ProductQueryPort defaultQueryPort;
    
    /**
     * 생성자에서 모든 ProductQueryPort 구현체를 주입받아 Map으로 구성
     * Spring이 자동으로 모든 ProductQueryPort 구현체를 List로 주입
     */
    public ProductQueryPortResolver(
            @Qualifier("preview_product_query_repository") ProductQueryPort previewProductQueryPort,
            @Qualifier("default_product_query_repository") ProductQueryPort defaultProductQueryPort) {
        
        this.defaultQueryPort = defaultQueryPort;

        this.queryPortMap.put(BillingCalculationType.PREVIEW_INQUIRY, previewProductQueryPort);
    }
    
    /**
     * BillingCalculationType에 따른 적절한 ProductQueryPort 구현체 반환
     */
    public ProductQueryPort getProductQueryPort(BillingCalculationType billingCalculationType) {
        ProductQueryPort queryPort = queryPortMap.get(billingCalculationType);
        return queryPort == null ? defaultQueryPort : queryPort;
    }

}
```

제안하는 개선 사항은 각 `Port`가 구현해야 하는 인터페이스를 정의하고, Spring의 의존성 주입을 사용하여 모든 `Port` 구현을 자동으로 검색하도록 `Resolver`를 리팩토링하는 것입니다. 이렇게 하면 시스템을 더 유지보수하기 쉽고 확장 가능하게 만들 수 있습니다.

다음은 제안된 코드 변경 사항입니다.

`ProductQueryPort.java`
```java
package me.realimpact.telecom.calculation.port.out;

import me.realimpact.telecom.calculation.api.BillingCalculationType;
import me.realimpact.telecom.calculation.domain.monthlyfee.ContractWithProductsAndSuspensions;

import java.time.LocalDate;
import java.util.List;

public interface ProductQueryPort {
    List<ContractWithProductsAndSuspensions> findContractsAndProductInventoriesByContractIds(
        List<Long> contractIds, LocalDate billingStartDate, LocalDate billingEndDate
    );

    BillingCalculationType getBillingCalculationType();
}
```

`ProductQueryRepository.java`
```java
package me.realimpact.telecom.calculation.infrastructure.adapter;

import lombok.RequiredArgsConstructor;
import me.realimpact.telecom.calculation.api.BillingCalculationType;
import me.realimpact.telecom.calculation.domain.monthlyfee.ContractWithProductsAndSuspensions;
import me.realimpact.telecom.calculation.infrastructure.adapter.mybatis.ProductQueryMapper;
import me.realimpact.telecom.calculation.infrastructure.converter.ContractDtoToDomainConverter;
import me.realimpact.telecom.calculation.infrastructure.dto.ContractProductsSuspensionsDto;
import me.realimpact.telecom.calculation.port.out.ProductQueryPort;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;

@Repository
@RequiredArgsConstructor
@Qualifier("default_product_query_repository")
public class ProductQueryRepository implements ProductQueryPort {
    private final ProductQueryMapper productQueryMapper;
    private final ContractDtoToDomainConverter converter;

    @Override
    public List<ContractWithProductsAndSuspensions> findContractsAndProductInventoriesByContractIds(
        List<Long> contractIds, LocalDate billingStartDate, LocalDate billingEndDate
    ) {
        List<ContractProductsSuspensionsDto> contractProductsSuspensionsDtos = productQueryMapper.findContractsAndProductInventoriesByContractIds(contractIds, billingStartDate, billingEndDate);
        return converter.convertToContracts(contractProductsSuspensionsDtos);
    }

    @Override
    public BillingCalculationType getBillingCalculationType() {
        return BillingCalculationType.REVENUE_CONFIRMATION;
    }
}
```

`PreviewProductQueryRepository.java`
```java
package me.realimpact.telecom.calculation.infrastructure.adapter;

import lombok.RequiredArgsConstructor;
import me.realimpact.telecom.calculation.api.BillingCalculationType;
import me.realimpact.telecom.calculation.domain.monthlyfee.ContractWithProductsAndSuspensions;
import me.realimpact.telecom.calculation.infrastructure.adapter.mybatis.PreviewProductQueryMapper;
import me.realimpact.telecom.calculation.infrastructure.converter.ContractDtoToDomainConverter;
import me.realimpact.telecom.calculation.infrastructure.dto.ContractProductsSuspensionsDto;
import me.realimpact.telecom.calculation.port.out.ProductQueryPort;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;

@Repository
@RequiredArgsConstructor
@Qualifier("preview_product_query_repository")
public class PreviewProductQueryRepository implements ProductQueryPort {
    private final PreviewProductQueryMapper previewProductQueryMapper;
    private final ContractDtoToDomainConverter converter;

    @Override
    public List<ContractWithProductsAndSuspensions> findContractsAndProductInventoriesByContractIds(
            List<Long> contractIds, LocalDate billingStartDate, LocalDate billingEndDate
    ) {
        List<ContractProductsSuspensionsDto> contractProductsSuspensionsDtos =
                previewProductQueryMapper.findContractsAndProductInventoriesByContractIds(contractIds, billingEndDate);
        return converter.convertToContracts(contractProductsSuspensionsDtos);
    }

    @Override
    public BillingCalculationType getBillingCalculationType() {
        return BillingCalculationType.PREVIEW_INQUIRY;
    }
}
```

`ProductQueryPortResolver.java`
```java
package me.realimpact.telecom.calculation.infrastructure.adapter;

import lombok.extern.slf4j.Slf4j;
import me.realimpact.telecom.calculation.api.BillingCalculationType;
import me.realimpact.telecom.calculation.port.out.ProductQueryPort;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Component
@Slf4j
public class ProductQueryPortResolver {

    private final Map<BillingCalculationType, ProductQueryPort> queryPortMap;
    private final ProductQueryPort defaultQueryPort;

    public ProductQueryPortResolver(
            List<ProductQueryPort> queryPorts,
            @Qualifier("default_product_query_repository") ProductQueryPort defaultQueryPort) {
        this.queryPortMap = queryPorts.stream()
                .filter(port -> port.getBillingCalculationType() != null)
                .collect(Collectors.toMap(ProductQueryPort::getBillingCalculationType, Function.identity()));
        this.defaultQueryPort = defaultQueryPort;
        log.info("Initialized ProductQueryPortResolver with {} ports.", queryPortMap.size());
    }

    public ProductQueryPort getProductQueryPort(BillingCalculationType billingCalculationType) {
        return queryPortMap.getOrDefault(billingCalculationType, defaultQueryPort);
    }
}
```