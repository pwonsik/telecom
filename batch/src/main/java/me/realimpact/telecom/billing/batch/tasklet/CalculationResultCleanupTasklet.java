package me.realimpact.telecom.billing.batch.tasklet;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import me.realimpact.telecom.billing.batch.CalculationParameters;
import me.realimpact.telecom.calculation.infrastructure.adapter.mybatis.CalculationResultMapper;
import org.springframework.batch.core.StepContribution;
import org.springframework.batch.core.configuration.annotation.StepScope;
import org.springframework.batch.core.scope.context.ChunkContext;
import org.springframework.batch.core.step.tasklet.Tasklet;
import org.springframework.batch.repeat.RepeatStatus;
import org.springframework.stereotype.Component;

/**
 * 계산 결과 정리 Tasklet
 * 배치 실행 전에 해당 청구 기간의 기존 계산 결과를 삭제한다.
 */
@Component
@StepScope
@RequiredArgsConstructor
@Slf4j
public class CalculationResultCleanupTasklet implements Tasklet {
    
    private final CalculationResultMapper calculationResultMapper;
    
    @Override
    public RepeatStatus execute(StepContribution contribution, ChunkContext chunkContext) throws Exception {
        long startTime = System.currentTimeMillis();
        
        try {
            int deletedCount = calculationResultMapper.deleteAllCalculationResults();
            
            // Step 실행 결과에 삭제된 행 수 기록
            contribution.incrementWriteCount(deletedCount);
            
            long executionTime = System.currentTimeMillis() - startTime;
            log.info("=== Calculation Result Cleanup Completed ===");
            log.info("삭제된 행 수: {}, 실행 시간: {}ms", deletedCount, executionTime);
            
            return RepeatStatus.FINISHED;
            
        } catch (Exception e) {
            long executionTime = System.currentTimeMillis() - startTime;
            log.error("=== Calculation Result Cleanup Failed ===");
            log.error("실행 시간: {}ms", executionTime);
            log.error("에러 발생: ", e);
            throw e;
        }
    }

}