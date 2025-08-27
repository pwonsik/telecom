-- CalculationResult 테이블 생성 스크립트
-- MonthlyFeeCalculationResult의 각 MonthlyFeeCalculationResultItem을 평면화해서 저장
-- 단일 테이블로 batch insert 최적화

DROP TABLE IF EXISTS calculation_result;

CREATE TABLE calculation_result (
    -- Primary Key
    calculation_result_id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY COMMENT '계산 결과 ID',
    
    -- Contract 정보 (중복 저장됨)
    contract_id BIGINT NOT NULL COMMENT '계약 ID',
    
    -- 청구 기간 정보 (중복 저장됨)
    billing_start_date DATE NOT NULL COMMENT '청구 시작일',
    billing_end_date DATE NOT NULL COMMENT '청구 종료일',
    
    -- MonthlyFeeCalculationResultItem 정보
    product_offering_id VARCHAR(50) NOT NULL COMMENT '상품 오퍼링 ID',
    charge_item_id VARCHAR(50) NOT NULL COMMENT '과금 항목 ID',
    effective_start_date DATE NOT NULL COMMENT '유효 시작일',
    effective_end_date DATE NOT NULL COMMENT '유효 종료일',
    suspension_type VARCHAR(30) COMMENT '정지 유형 (TEMPORARY_SUSPENSION, PARTIAL_SUSPENSION 등)',
    fee DECIMAL(15,2) NOT NULL COMMENT '계산된 요금',
    
    -- 메타데이터
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP COMMENT '생성 시각',
    
    -- 인덱스 (대용량 조회 성능 향상)
    INDEX idx_contract (contract_id),
    INDEX idx_billing_period (billing_start_date, billing_end_date),
    INDEX idx_product_offering (product_offering_id),
    INDEX idx_charge_item (charge_item_id),
    INDEX idx_effective_period (effective_start_date, effective_end_date),
    INDEX idx_suspension_type (suspension_type),
    INDEX idx_created_at (created_at)
) COMMENT = '월정액 요금 계산 결과 (평면화 구조)';

-- 파티셔닝을 위한 준비 (추후 대용량 데이터 처리시 월별 파티셔닝 가능)
-- ALTER TABLE calculation_result 
-- PARTITION BY RANGE (YEAR(billing_start_date) * 100 + MONTH(billing_start_date)) (
--     PARTITION p202401 VALUES LESS THAN (202402),
--     PARTITION p202402 VALUES LESS THAN (202403),
--     -- 추가 파티션들...
-- );