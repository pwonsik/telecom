-- RevenueMasterData 테이블 생성 스크립트
-- 수익 항목 마스터 데이터를 저장하는 테이블
-- 시계열 데이터로 동일한 revenue_item_id가 여러 기간에 걸쳐 다른 설정을 가질 수 있음

DROP TABLE IF EXISTS revenue_master_data;

CREATE TABLE revenue_master_data (
    -- Primary Key (복합키)
    revenue_item_id VARCHAR(50) NOT NULL COMMENT '수익 항목 ID',
    effective_end_date DATE NOT NULL COMMENT '유효 종료일',
    
    -- 일반 컬럼
    effective_start_date DATE NOT NULL COMMENT '유효 시작일',
    revenue_item_name VARCHAR(200) NOT NULL COMMENT '수익 항목명',
    overdue_charge_revenue_item_id VARCHAR(50) COMMENT '연체료 수익 항목 ID',
    vat_revenue_item_id VARCHAR(50) COMMENT 'VAT 수익 항목 ID',
    
    -- 메타데이터
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP COMMENT '생성 시각',
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '수정 시각',
    
    -- Primary Key 정의
    PRIMARY KEY (revenue_item_id, effective_end_date),
    
    -- 제약조건: 시작일은 종료일보다 작아야 함
    CHECK (effective_start_date <= effective_end_date)
    
) COMMENT = '수익 항목 마스터 데이터 (시계열 구조)';