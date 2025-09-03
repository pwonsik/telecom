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
            List<ProductQueryPort> productQueryPorts,
            @Qualifier("")
            @Qualifier("default") ProductQueryPort defaultQueryPort) {
        
        this.defaultQueryPort = defaultQueryPort;
        
        // List의 각 구현체에서 @Qualifier 값을 추출하여 Map 생성
        this.queryPortMap = productQueryPorts.stream()
            .collect(Collectors.toMap(
                this::extractQualifierName,
                Function.identity()
            ));
            
        log.info("ProductQueryPortResolver 초기화 완료. 등록된 구현체: {}", 
                queryPortMap.keySet());
    }
    
    /**
     * BillingCalculationType에 따른 적절한 ProductQueryPort 구현체 반환
     */
    public ProductQueryPort getProductQueryPort(BillingCalculationType billingCalculationType) {
        String qualifierName = billingCalculationType.name();
        ProductQueryPort queryPort = queryPortMap.get(qualifierName);
        return queryPort == null ? defaultQueryPort : queryPort;
    }
    
    /**
     * ProductQueryPort 구현체에서 @Qualifier 값 추출
     * Spring의 빈 이름이나 @Qualifier 어노테이션 값을 기반으로 식별
     */
    private String extractQualifierName(ProductQueryPort productQueryPort) {
        Class<?> clazz = productQueryPort.getClass();
        
        // @Qualifier 어노테이션에서 값 추출
        Qualifier qualifierAnnotation = clazz.getAnnotation(Qualifier.class);
        if (qualifierAnnotation != null) {
            return qualifierAnnotation.value();
        }
        
        // @Qualifier가 없으면 클래스 이름을 기본값으로 사용
        return clazz.getSimpleName();
    }
}