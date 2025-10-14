package me.realimpact.telecom.billing.batch.partitioner;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.core.partition.support.Partitioner;
import org.springframework.batch.item.ExecutionContext;

import java.util.HashMap;
import java.util.Map;

/**
 * 계약 ID를 기준으로 데이터 처리를 병렬화하기 위해 파티션을 생성하는 Partitioner.
 * 각 파티션은 `contractId % threadCount = partitionKey` 조건에 따라 데이터를 분할하여 처리한다.
 */
@RequiredArgsConstructor
@Slf4j
public class ContractPartitioner implements Partitioner {

    private final int threadCount;

    /**
     * 지정된 gridSize(스레드 수)에 따라 파티션을 생성한다.
     * 각 파티션은 고유한 `partitionKey`와 전체 파티션 수 `partitionCount`를 ExecutionContext에 포함한다.
     * @param gridSize 파티션의 수 (보통 스레드 수와 동일)
     * @return 파티션 이름과 ExecutionContext를 담은 맵
     */
    @Override
    public Map<String, ExecutionContext> partition(int gridSize) {
        Map<String, ExecutionContext> partitions = new HashMap<>();
        
        log.info("=== 파티션 생성 시작 ===");
        log.info("요청된 파티션 수 (gridSize): {}", gridSize);
        log.info("실제 생성할 파티션 수 (threadCount): {}", threadCount);
        
        // gridSize 대신 생성자에서 받은 threadCount를 기준으로 파티션을 생성한다.
        for (int i = 0; i < threadCount; i++) {
            ExecutionContext context = new ExecutionContext();
            
            // 각 파티션이 처리할 데이터 범위를 정의하는 정보를 ExecutionContext에 저장한다.
            context.putInt("partitionKey", i);
            context.putInt("partitionCount", threadCount);
            
            String partitionName = "partition" + i;
            partitions.put(partitionName, context);
            
            log.info("파티션 생성: {} (partitionKey={}, partitionCount={})", 
                    partitionName, i, threadCount);
        }
        
        log.info("총 {} 개 파티션 생성 완료", partitions.size());
        
        return partitions;
    }
}
