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
        
        this.defaultQueryPort = defaultProductQueryPort;

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