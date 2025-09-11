package me.realimpact.telecom.billing.batch.partitioner;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.core.partition.support.Partitioner;
import org.springframework.batch.item.ExecutionContext;

import java.util.HashMap;
import java.util.Map;

/**
 * 계약 ID를 기반으로 파티션을 생성하는 Partitioner
 * 각 파티션은 contractId % threadCount = partitionKey 조건으로 데이터를 분할
 */
@RequiredArgsConstructor
@Slf4j
public class ContractPartitioner implements Partitioner {

    private final int threadCount;

    @Override
    public Map<String, ExecutionContext> partition(int gridSize) {
        Map<String, ExecutionContext> partitions = new HashMap<>();
        
        log.info("=== 파티션 생성 시작 ===");
        log.info("요청된 파티션 수 (gridSize): {}", gridSize);
        log.info("실제 생성할 파티션 수 (threadCount): {}", threadCount);
        
        // threadCount만큼 파티션 생성 (gridSize 무시하고 threadCount 사용)
        for (int i = 0; i < threadCount; i++) {
            ExecutionContext context = new ExecutionContext();
            
            // 파티션 키 설정 (contractId % threadCount = partitionKey)
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