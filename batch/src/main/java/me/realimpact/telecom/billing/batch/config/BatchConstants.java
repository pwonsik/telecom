package me.realimpact.telecom.billing.batch.config;

/**
 * Spring Batch 애플리케이션에서 사용되는 공통 상수를 정의하는 클래스.
 */
public final class BatchConstants {
    
    private BatchConstants() {
        // 유틸리티 클래스이므로 인스턴스 생성을 방지한다.
    }
    
    /**
     * Spring Batch의 청크(chunk) 크기.
     * Reader가 한 번에 읽어올 아이템의 수이며, 트랜잭션이 커밋되는 단위이다.
     */
    public static final int CHUNK_SIZE = 1000;
    
    /**
     * 멀티스레드 스텝을 사용하는 기본 요금 계산 잡(Job)의 이름.
     */
    public static final String THREAD_POOL_JOB_NAME = "monthlyFeeCalculationJob";
    
    /**
     * 파티셔닝을 사용하는 요금 계산 잡(Job)의 이름.
     */
    public static final String PARTITIONED_JOB_NAME = "partitionedMonthlyFeeCalculationJob";
}
