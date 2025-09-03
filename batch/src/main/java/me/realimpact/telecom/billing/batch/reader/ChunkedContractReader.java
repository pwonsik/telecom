package me.realimpact.telecom.billing.batch.reader;

import lombok.extern.slf4j.Slf4j;
import me.realimpact.telecom.billing.batch.CalculationParameters;
import me.realimpact.telecom.calculation.application.monthlyfee.BaseFeeCalculator;
import me.realimpact.telecom.calculation.application.discount.DiscountCalculator;
import me.realimpact.telecom.calculation.domain.CalculationContext;
import me.realimpact.telecom.calculation.domain.discount.ContractDiscounts;
import me.realimpact.telecom.calculation.domain.onetimecharge.OneTimeChargeDomain;
import me.realimpact.telecom.calculation.application.onetimecharge.OneTimeChargeDataLoader;
import org.apache.ibatis.session.SqlSessionFactory;
import org.mybatis.spring.batch.MyBatisCursorItemReader;
import org.springframework.batch.core.configuration.annotation.StepScope;
import org.springframework.batch.item.ExecutionContext;
import org.springframework.batch.item.ItemStreamException;
import org.springframework.batch.item.ItemStreamReader;
import org.springframework.batch.item.support.ListItemReader;

import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

import static me.realimpact.telecom.billing.batch.config.BatchConstants.CHUNK_SIZE;

@StepScope
@Slf4j
public class ChunkedContractReader implements ItemStreamReader<CalculationTarget> {

    private final BaseFeeCalculator baseFeeCalculator;
    private final DiscountCalculator discountCalculator;
    
    // DataLoader Map - 조건문 제거
    private final Map<Class<? extends OneTimeChargeDomain>, OneTimeChargeDataLoader<? extends OneTimeChargeDomain>>
            oneTimeChargeDataLoaderMap;

    private final SqlSessionFactory sqlSessionFactory;
    private final CalculationParameters calculationParameters;

    private static final int chunkSize = CHUNK_SIZE;

    private MyBatisCursorItemReader<Long> contractIdReader;
    private ListItemReader<CalculationTarget> currentChunkReader;
    private boolean initialized = false;

    /**
     * 생성자에서 DataLoader List를 Map으로 변환
     */
    public ChunkedContractReader(
            BaseFeeCalculator baseFeeCalculator,
            DiscountCalculator discountCalculator,
            List<OneTimeChargeDataLoader<? extends OneTimeChargeDomain>> oneTimeChargeDataLoaders,
            SqlSessionFactory sqlSessionFactory,
            CalculationParameters calculationParameters) {
        
        this.baseFeeCalculator = baseFeeCalculator;
        this.discountCalculator = discountCalculator;
        this.sqlSessionFactory = sqlSessionFactory;
        this.calculationParameters = calculationParameters;
        
        // DataLoader List를 Map으로 변환
        this.oneTimeChargeDataLoaderMap = oneTimeChargeDataLoaders.stream()
            .collect(Collectors.toMap(
                OneTimeChargeDataLoader::getDataType,
                Function.identity()
            ));
            
        log.info("Registered {} OneTimeCharge DataLoaders: {}",
                oneTimeChargeDataLoaders.size(),
                oneTimeChargeDataLoaders.stream()
                .map(loader -> loader.getDataType().getSimpleName())
                .collect(Collectors.joining(", ")));
    }

    
    @Override
    public void open(ExecutionContext executionContext) throws ItemStreamException {
        if (!initialized) {
            initializeContractIdReader();
            contractIdReader.open(executionContext);
            initialized = true;
        }
    }
    
    @Override
    public void update(ExecutionContext executionContext) throws ItemStreamException {
        if (contractIdReader != null) {
            contractIdReader.update(executionContext);
        }
    }
    
    @Override
    public void close() throws ItemStreamException {
        if (contractIdReader != null) {
            contractIdReader.close();
        }
    }
    
    @Override
    public CalculationTarget read() throws Exception {
        // 현재 청크에서 아이템을 읽기 시도
        if (currentChunkReader != null) {
            CalculationTarget item = currentChunkReader.read();
            if (item != null) {
                return item;
            }
        }
        
        // 현재 청크가 끝났으면 다음 청크 로드
        loadNextChunk();
        if (currentChunkReader == null) {
            return null; // 더 이상 읽을 데이터가 없음
        }
        
        // 새로운 청크에서 첫 번째 아이템 반환
        return currentChunkReader.read();
    }
    
    private void initializeContractIdReader() {
        log.info("=== ChunkedContractReader 초기화 ===");
        log.info("chunkSize: {}", chunkSize);

        contractIdReader = new MyBatisCursorItemReader<>();
        contractIdReader.setSqlSessionFactory(sqlSessionFactory);
        contractIdReader.setQueryId("me.realimpact.telecom.calculation.infrastructure.adapter.mybatis.ContractQueryMapper.findContractIds");
        contractIdReader.setParameterValues(getParameterValues());
    }

    private Map<String, Object> getParameterValues() {
        Map<String, Object> parameterValues = new HashMap<>();
        parameterValues.put("billingStartDate", calculationParameters.getBillingStartDate());
        parameterValues.put("billingEndDate", calculationParameters.getBillingEndDate());
        parameterValues.put("contractIds", calculationParameters.getContractIds());
        return parameterValues;
    }

    private void loadNextChunk() throws Exception {
        //log.debug("=== ChunkedContractReader loadNextChunk ===");
        
        List<Long> contractIds = new ArrayList<>();
        
        // chunk size만큼 contract ID를 읽어오기
        for (int i = 0; i < chunkSize; i++) {
            Long contractId = contractIdReader.read();
            if (contractId == null) {
                break; // 더 이상 읽을 데이터가 없음
            }
            contractIds.add(contractId);
        }
        
        if (contractIds.isEmpty()) {
            currentChunkReader = null;
            return;
        }

        // ListItemReader로 감싸서 하나씩 반환할 수 있도록 설정
        currentChunkReader = new ListItemReader<>(getCalculationTargets(contractIds));
    }

    private List<CalculationTarget> getCalculationTargets(List<Long> contractIds) {
        CalculationContext ctx = calculationParameters.toCalculationContext();
        
        // 월정액 (기존 방식 유지)
        var contractWithProductsAndSuspensionsMap = baseFeeCalculator.read(ctx, contractIds);
        
        // OneTimeCharge 데이터를 Map으로 로딩 - 조건문 없음
        //Map<Class<? extends OneTimeChargeDomain>, Map<Long, List<? extends OneTimeChargeDomain>>>
        var oneTimeChargeDataByType = loadOneTimeChargeDataByType(contractIds, ctx);
        
        // 할인 (기존 방식 유지)
        var contractDiscountsMap = discountCalculator.read(ctx, contractIds);

        List<CalculationTarget> calculationTargets = new ArrayList<>();
        
        // 모든 조회 대상을 calculationTarget으로 모은다.
        for (Long contractId : contractIds) {
            // OneTimeCharge 데이터를 계약별로 그룹화
            var oneTimeChargeDataForContract = groupOneTimeChargeDataByContract(contractId, oneTimeChargeDataByType);
            
            var discounts = Optional.ofNullable(contractDiscountsMap.get(contractId))
                .map(ContractDiscounts::discounts)
                .orElse(Collections.emptyList());

            CalculationTarget calculationTarget = new CalculationTarget(
                contractId,
                contractWithProductsAndSuspensionsMap.getOrDefault(contractId, Collections.emptyList()),
                oneTimeChargeDataForContract,
                discounts
            );
            calculationTargets.add(calculationTarget);
        }

        log.info("생성된 calculationTargets 개수: {}", calculationTargets.size());

        return calculationTargets;
    }
    
    /**
     * 모든 DataLoader를 실행하여 OneTimeCharge 데이터 로딩
     * key : OneTimeCharge종류
     * value : key가 계약Id이고, value가 domain의 list인 map
     */
    private Map<Class<? extends OneTimeChargeDomain>, Map<Long, List<OneTimeChargeDomain>>> 
            loadOneTimeChargeDataByType(List<Long> contractIds, CalculationContext context) {
        
        Map<Class<? extends OneTimeChargeDomain>, Map<Long, List<OneTimeChargeDomain>>> result = new HashMap<>();
        
        // Map을 순회하면서 각 DataLoader 실행 - 조건문 완전 제거
        //for (Map.Entry<Class<? extends OneTimeChargeDomain>, OneTimeChargeDataLoader<? extends OneTimeChargeDomain>>
        for (var entry : oneTimeChargeDataLoaderMap.entrySet()) {
//            Class<? extends OneTimeChargeDomain> dataType = entry.getKey();
//            OneTimeChargeDataLoader<? extends OneTimeChargeDomain> loader = entry.getValue();
            var dataType = entry.getKey();
            var loader = entry.getValue();
            Map<Long, List<OneTimeChargeDomain>> data = loader.read(contractIds, context);
            if (!data.isEmpty()) {
                result.put(dataType, data);
            }
        }
        
        return result;
    }
    /**
     * 특정 계약의 OneTimeCharge 데이터 그룹화
     */
    private Map<Class<? extends OneTimeChargeDomain>, List<OneTimeChargeDomain>>
        groupOneTimeChargeDataByContract(
            Long contractId,
            Map<Class<? extends OneTimeChargeDomain>, Map<Long, List<OneTimeChargeDomain>>> oneTimeChargeDataByType) {

        Map<Class<? extends OneTimeChargeDomain>, List<OneTimeChargeDomain>> result = new HashMap<>();

        //for (Map.Entry<Class<? extends OneTimeChargeDomain>, Map<Long, List<? extends OneTimeChargeDomain>>>
        for (var entry : oneTimeChargeDataByType.entrySet()) {

//            Class<? extends OneTimeChargeDomain> dataType = entry.getKey();
//            Map<Long, List<? extends OneTimeChargeDomain>> dataByContract = entry.getValue();
            var dataType = entry.getKey();
            var dataByContract = entry.getValue();

            List<OneTimeChargeDomain> contractData = dataByContract.get(contractId);
            if (contractData != null && !contractData.isEmpty()) {
                result.put(dataType, contractData);
            }
        }

        return result;
    }
}