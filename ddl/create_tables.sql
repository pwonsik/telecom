-- MySQL DDL 스크립트
-- 텔레콤 빌링 시스템 테이블 생성

-- 1. contract 테이블
DROP TABLE IF EXISTS contract;
CREATE TABLE contract (
    contract_id BIGINT NOT NULL PRIMARY KEY COMMENT '계약 ID',
    subscribed_at DATE NOT NULL COMMENT '가입일',
    initially_subscribed_at DATE NOT NULL COMMENT '최초 가입일',
    terminated_at DATE COMMENT '해지일',
    preffered_termination_date DATE COMMENT '선호 해지일',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP COMMENT '생성일시',
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '수정일시'
) COMMENT = '계약 정보';

-- 2. product_offering 테이블
DROP TABLE IF EXISTS product_offering;
CREATE TABLE product_offering (
    product_offering_id VARCHAR(50) NOT NULL PRIMARY KEY COMMENT '상품 오퍼링 ID',
    product_offering_name VARCHAR(200) NOT NULL COMMENT '상품 오퍼링 명',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP COMMENT '생성일시',
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '수정일시'
) COMMENT = '상품 오퍼링 정보';

-- 3. product 테이블
DROP TABLE IF EXISTS product;
CREATE TABLE product (
    contract_id BIGINT NOT NULL COMMENT '계약 ID',
    product_offering_id VARCHAR(50) NOT NULL COMMENT '상품 오퍼링 ID',
    effective_start_date_time DATETIME NOT NULL COMMENT '유효 시작일시',
    effective_end_date_time DATETIME NOT NULL COMMENT '유효 종료일시',
    subscribed_at DATE NOT NULL COMMENT '가입일',
    activated_at DATE COMMENT '활성화일',
    terminated_at DATE COMMENT '종료일',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP COMMENT '생성일시',
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '수정일시',
    PRIMARY KEY (contract_id, product_offering_id, effective_start_date_time, effective_end_date_time),
    FOREIGN KEY (contract_id) REFERENCES contract(contract_id),
    FOREIGN KEY (product_offering_id) REFERENCES product_offering(product_offering_id)
) COMMENT = '상품 정보';

-- 4. suspension 테이블  
DROP TABLE IF EXISTS suspension;
CREATE TABLE suspension (
    contract_id BIGINT NOT NULL COMMENT '계약 ID',
    suspension_type_code VARCHAR(10) NOT NULL COMMENT '정지 유형 코드',
    effective_start_date_time DATETIME NOT NULL COMMENT '유효 시작일시',
    effective_end_date_time DATETIME NOT NULL COMMENT '유효 종료일시',
    suspension_type_description VARCHAR(100) NOT NULL COMMENT '정지 유형 설명',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP COMMENT '생성일시',
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '수정일시',
    PRIMARY KEY (contract_id, suspension_type_code, effective_start_date_time, effective_end_date_time),
    FOREIGN KEY (contract_id) REFERENCES contract(contract_id)
) COMMENT = '정지 정보';

-- 5. monthly_charge_item 테이블
DROP TABLE IF EXISTS monthly_charge_item;
CREATE TABLE monthly_charge_item (
    product_offering_id VARCHAR(50) NOT NULL COMMENT '상품 오퍼링 ID',
    charge_item_id VARCHAR(50) NOT NULL COMMENT '과금 항목 ID',
    charge_item_name VARCHAR(200) NOT NULL COMMENT '과금 항목명',
    suspension_charge_ratio DECIMAL(5,4) NOT NULL DEFAULT 0.0000 COMMENT '정지시 과금 비율',
    calculation_method_code VARCHAR(20) NOT NULL COMMENT '계산 방법 코드',
    calculation_method_name VARCHAR(100) NOT NULL COMMENT '계산 방법명',
    flat_rate_amount DECIMAL(15,2) COMMENT '정액 요금',
    pricing_type VARCHAR(20) NOT NULL COMMENT '가격 정책 유형',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP COMMENT '생성일시',
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '수정일시',
    PRIMARY KEY (product_offering_id, charge_item_id),
    FOREIGN KEY (product_offering_id) REFERENCES product_offering(product_offering_id)
) COMMENT = '월정액 과금 항목';

-- 인덱스 생성
-- contract 테이블 인덱스
--CREATE INDEX idx_contract_subscribed_at ON contract(subscribed_at);
--CREATE INDEX idx_contract_terminated_at ON contract(terminated_at);

-- product 테이블 인덱스  
--CREATE INDEX idx_product_contract_id ON product(contract_id);
--CREATE INDEX idx_product_offering_id ON product(product_offering_id);
--CREATE INDEX idx_product_effective_dates ON product(effective_start_date_time, effective_end_date_time);
--CREATE INDEX idx_product_subscribed_at ON product(subscribed_at);
--CREATE INDEX idx_product_terminated_at ON product(terminated_at);

-- suspension 테이블 인덱스
--CREATE INDEX idx_suspension_contract_id ON suspension(contract_id);
--CREATE INDEX idx_suspension_effective_dates ON suspension(effective_start_date_time, effective_end_date_time);
--CREATE INDEX idx_suspension_type_code ON suspension(suspension_type_code);

-- monthly_charge_item 테이블 인덱스
--CREATE INDEX idx_monthly_charge_item_product_offering ON monthly_charge_item(product_offering_id);
--CREATE INDEX idx_monthly_charge_item_calculation_method ON monthly_charge_item(calculation_method_code);
--CREATE INDEX idx_monthly_charge_item_pricing_type ON monthly_charge_item(pricing_type);