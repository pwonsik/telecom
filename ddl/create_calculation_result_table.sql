-- CalculationResult 테이블 생성 스크립트
-- MonthlyFeeCalculationResult 저장을 위한 테이블

DROP TABLE IF EXISTS calculation_result;
CREATE TABLE calculation_result (
    -- Primary Key: 계산 결과 고유 식별자
    calculation_result_id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY COMMENT '계산 결과 ID',
    
    -- Contract 정보
    contract_id BIGINT NOT NULL COMMENT '계약 ID',
    
    -- Product 정보  
    product_contract_id BIGINT NOT NULL COMMENT '상품 계약 ID',
    product_offering_id VARCHAR(50) NOT NULL COMMENT '상품 오퍼링 ID',
    
    -- MonthlyChargeItem 정보
    charge_item_id VARCHAR(50) NOT NULL COMMENT '과금 항목 ID',
    
    -- Suspension 정보 (Optional)
    suspension_type_code VARCHAR(10) COMMENT '정지 유형 코드',

    -- Period 정보 (ProratedPeriod의 period)
    period_start_date DATE NOT NULL COMMENT '계산 기간 시작일',
    period_end_date DATE NOT NULL COMMENT '계산 기간 종료일',
    usage_days INT NOT NULL COMMENT '사용 일수',
    days_of_month INT NOT NULL COMMENT '해당 월 총일수',
    
    -- 계산 결과
    calculated_fee DECIMAL(15,5) NOT NULL COMMENT '계산된 요금',

    -- 메타데이터
    billing_start_date DATE NOT NULL COMMENT '청구 시작일',
    billing_end_date DATE NOT NULL COMMENT '청구 종료일',
    
    -- 인덱스 (대용량 조회 성능 향상)
    INDEX idx_contract (contract_id)
    
    -- 외래 키 제약조건
    FOREIGN KEY (contract_id) REFERENCES contract(contract_id)
) COMMENT = '월정액 요금 계산 결과';

-- 파티셔닝을 위한 준비 (추후 대용량 데이터 처리시 월별 파티셔닝 가능)
-- ALTER TABLE calculation_result 
-- PARTITION BY RANGE (YEAR(billing_start_date) * 100 + MONTH(billing_start_date)) (
--     PARTITION p202401 VALUES LESS THAN (202402),
--     PARTITION p202402 VALUES LESS THAN (202403),
--     -- 추가 파티션들...
-- );