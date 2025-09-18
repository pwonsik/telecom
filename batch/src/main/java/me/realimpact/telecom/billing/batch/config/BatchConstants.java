package me.realimpact.telecom.billing.batch.config;

/**
 * Spring Batch 관련 상수 정의
 */
public final class BatchConstants {
    
    private BatchConstants() {
        // Utility class - 인스턴스 생성 방지
    }
    
    /**
     * Spring Batch chunk size
     * Reader에서 한 번에 읽어올 데이터 개수 및 트랜잭션 단위
     */
    public static final int CHUNK_SIZE = 1000;
    
    /**
     * Job Names
     */
    public static final String THREAD_POOL_JOB_NAME = "monthlyFeeCalculationJob";
    public static final String PARTITIONED_JOB_NAME = "partitionedMonthlyFeeCalculationJob";
}