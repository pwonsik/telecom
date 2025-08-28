package me.realimpact.telecom.billing.batch.tasklet;

import me.realimpact.telecom.billing.batch.CalculationParameters;
import me.realimpact.telecom.calculation.api.BillingCalculationPeriod;
import me.realimpact.telecom.calculation.api.BillingCalculationType;
import me.realimpact.telecom.calculation.infrastructure.adapter.mybatis.CalculationResultMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.batch.core.StepContribution;
import org.springframework.batch.core.StepExecution;
import org.springframework.batch.core.scope.context.ChunkContext;
import org.springframework.batch.core.scope.context.StepContext;
import org.springframework.batch.repeat.RepeatStatus;

import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CalculationResultCleanupTaskletTest {

    @Mock
    private CalculationResultMapper calculationResultMapper;
    
    @Mock
    private StepContribution stepContribution;
    
    @Mock
    private ChunkContext chunkContext;
    
    @Mock
    private StepContext stepContext;
    
    @Mock
    private StepExecution stepExecution;
    
    private CalculationParameters calculationParameters;
    private CalculationResultCleanupTasklet tasklet;
    
    private Map<String, Object> jobParameters;

    @BeforeEach
    void setUp() {
        calculationParameters = new CalculationParameters(
            LocalDate.of(2024, 3, 1),
            LocalDate.of(2024, 3, 31),
            BillingCalculationType.REVENUE_CONFIRMATION,
            BillingCalculationPeriod.POST_BILLING_CURRENT_MONTH,
            4,
            List.of()
        );
        
        tasklet = new CalculationResultCleanupTasklet(calculationResultMapper, calculationParameters);
        
        jobParameters = new HashMap<>();
        
        when(chunkContext.getStepContext()).thenReturn(stepContext);
        when(stepContext.getJobParameters()).thenReturn(jobParameters);
    }

    @Test
    @DisplayName("청구 기간별 삭제 모드 - 기본 동작")
    void execute_RangeMode_Success() throws Exception {
        // given
        int expectedDeleteCount = 150;
        when(calculationResultMapper.deleteCalculationResultsByDateRange(any(), any()))
            .thenReturn(expectedDeleteCount);

        // when
        RepeatStatus result = tasklet.execute(stepContribution, chunkContext);

        // then
        assertThat(result).isEqualTo(RepeatStatus.FINISHED);
        
        verify(calculationResultMapper).deleteCalculationResultsByDateRange(
            calculationParameters.getBillingStartDate(),
            calculationParameters.getBillingEndDate()
        );
        verify(calculationResultMapper, never()).deleteAllCalculationResults();
        verify(stepContribution).incrementWriteCount(expectedDeleteCount);
    }

    @Test
    @DisplayName("전체 삭제 모드 - cleanupMode=ALL")
    void execute_AllMode_Success() throws Exception {
        // given
        jobParameters.put("cleanupMode", "ALL");
        int expectedDeleteCount = 1000;
        when(calculationResultMapper.deleteAllCalculationResults())
            .thenReturn(expectedDeleteCount);

        // when
        RepeatStatus result = tasklet.execute(stepContribution, chunkContext);

        // then
        assertThat(result).isEqualTo(RepeatStatus.FINISHED);
        
        verify(calculationResultMapper).deleteAllCalculationResults();
        verify(calculationResultMapper, never()).deleteCalculationResultsByDateRange(any(), any());
        verify(stepContribution).incrementWriteCount(expectedDeleteCount);
    }

    @Test
    @DisplayName("cleanup 건너뛰기 - skipCleanup=true")
    void execute_SkipCleanup_Success() throws Exception {
        // given
        jobParameters.put("skipCleanup", "true");

        // when
        RepeatStatus result = tasklet.execute(stepContribution, chunkContext);

        // then
        assertThat(result).isEqualTo(RepeatStatus.FINISHED);
        
        verify(calculationResultMapper, never()).deleteAllCalculationResults();
        verify(calculationResultMapper, never()).deleteCalculationResultsByDateRange(any(), any());
        verify(stepContribution, never()).incrementWriteCount(anyInt());
    }

    @Test
    @DisplayName("cleanup 모드 대소문자 구분 없음 - cleanupMode=all")
    void execute_CleanupModeIgnoreCase_Success() throws Exception {
        // given
        jobParameters.put("cleanupMode", "all");  // 소문자
        int expectedDeleteCount = 500;
        when(calculationResultMapper.deleteAllCalculationResults())
            .thenReturn(expectedDeleteCount);

        // when
        RepeatStatus result = tasklet.execute(stepContribution, chunkContext);

        // then
        assertThat(result).isEqualTo(RepeatStatus.FINISHED);
        verify(calculationResultMapper).deleteAllCalculationResults();
    }

    @Test
    @DisplayName("데이터베이스 오류 발생 시 예외 전파")
    void execute_DatabaseError_ThrowsException() {
        // given
        RuntimeException dbException = new RuntimeException("Database connection failed");
        when(calculationResultMapper.deleteCalculationResultsByDateRange(any(), any()))
            .thenThrow(dbException);

        // when & then
        assertThatThrownBy(() -> tasklet.execute(stepContribution, chunkContext))
            .isInstanceOf(RuntimeException.class)
            .hasMessage("Database connection failed");
        
        verify(stepContribution, never()).incrementWriteCount(anyInt());
    }

    @Test
    @DisplayName("Job Parameters null 처리")
    void execute_NullJobParameters_UseDefaults() throws Exception {
        // given
        when(stepContext.getJobParameters()).thenReturn(new HashMap<>());  // 빈 맵
        int expectedDeleteCount = 100;
        when(calculationResultMapper.deleteCalculationResultsByDateRange(any(), any()))
            .thenReturn(expectedDeleteCount);

        // when
        RepeatStatus result = tasklet.execute(stepContribution, chunkContext);

        // then
        assertThat(result).isEqualTo(RepeatStatus.FINISHED);
        
        // 기본값 RANGE 모드로 동작
        verify(calculationResultMapper).deleteCalculationResultsByDateRange(any(), any());
        verify(calculationResultMapper, never()).deleteAllCalculationResults();
    }
}