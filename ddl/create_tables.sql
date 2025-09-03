-- MySQL DDL 스크립트
-- 텔레콤 빌링 시스템 테이블 생성

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
                                     PRIMARY KEY (revenue_item_id, effective_end_date)

) COMMENT = '수익 항목 마스터 데이터 (시계열 구조)';

-- 5. charge_item 테이블
DROP TABLE IF EXISTS charge_item;
CREATE TABLE charge_item (
                             product_offering_id VARCHAR(50) NOT NULL COMMENT '상품 오퍼링 ID',
                             charge_item_id VARCHAR(50) NOT NULL COMMENT '과금 항목 ID',
                             charge_item_name VARCHAR(200) NOT NULL COMMENT '과금 항목명',
                             revenue_item_id VARCHAR(50) NOT NULL COMMENT '수익 항목 ID',
                             suspension_charge_ratio DECIMAL(5,4) NOT NULL DEFAULT 0.0000 COMMENT '정지시 과금 비율',
                             calculation_method_code VARCHAR(20) NOT NULL COMMENT '계산 방법 코드',
                             calculation_method_name VARCHAR(100) NOT NULL COMMENT '계산 방법명',
                             flat_rate_amount DECIMAL(15,2) COMMENT '정액 요금',
                             pricing_type VARCHAR(20) NOT NULL COMMENT '가격 정책 유형',
                             created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP COMMENT '생성일시',
                             updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '수정일시',
                             PRIMARY KEY (product_offering_id, charge_item_id)
) COMMENT = '과금 항목';

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
                         PRIMARY KEY (contract_id, product_offering_id, effective_start_date_time, effective_end_date_time)
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
                            PRIMARY KEY (contract_id, suspension_type_code, effective_start_date_time, effective_end_date_time)
) COMMENT = '정지 정보';

-- 2. product_offering 테이블
DROP TABLE IF EXISTS product_offering;
CREATE TABLE product_offering (
                                  product_offering_id VARCHAR(50) NOT NULL PRIMARY KEY COMMENT '상품 오퍼링 ID',
                                  product_offering_name VARCHAR(200) NOT NULL COMMENT '상품 오퍼링 명',
                                  created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP COMMENT '생성일시',
                                  updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '수정일시'
) COMMENT = '상품 오퍼링 정보';

-- 단말할부 상세 테이블
DROP TABLE IF EXISTS device_installment_detail;
CREATE TABLE device_installment_detail (
                                           contract_id BIGINT NOT NULL COMMENT '계약 ID',
                                           installment_sequence BIGINT NOT NULL COMMENT '할부 일련번호',
                                           installment_round INT NOT NULL COMMENT '할부 회차 (1부터 시작)',
                                           installment_amount DECIMAL(10, 0) NOT NULL COMMENT '회차별 할부금',
                                           billing_completed_date DATE NULL COMMENT '청구 완료일 (NULL이면 미청구)',
                                           created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP COMMENT '생성일시',
                                           updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '수정일시',
                                           PRIMARY KEY (contract_id, installment_sequence, installment_round)
) COMMENT = '단말할부 상세';


-- 단말할부 마스터 테이블
DROP TABLE IF EXISTS device_installment_master;
CREATE TABLE device_installment_master (
                                           contract_id BIGINT NOT NULL COMMENT '계약 ID',
                                           installment_sequence BIGINT NOT NULL COMMENT '할부 일련번호',
                                           installment_start_date DATE NOT NULL COMMENT '할부 시작일',
                                           total_installment_amount DECIMAL(10, 0) NOT NULL COMMENT '할부금 총액',
                                           installment_months INT NOT NULL COMMENT '할부 개월수',
                                           billed_count INT NOT NULL DEFAULT 0 COMMENT '할부 청구 횟수',
                                           created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP COMMENT '생성일시',
                                           updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '수정일시',
                                           PRIMARY KEY (contract_id, installment_sequence)
) COMMENT = '단말할부 마스터';


-- 설치 이력 테이블
DROP TABLE IF EXISTS installation_history;
CREATE TABLE installation_history (
                                      contract_id BIGINT NOT NULL COMMENT '계약 ID',
                                      sequence_number BIGINT NOT NULL COMMENT '일련번호',
                                      installation_date DATE NOT NULL COMMENT '설치일',
                                      installation_fee DECIMAL(10, 0) NOT NULL COMMENT '설치비',
                                      billed_flag CHAR(1) NOT NULL DEFAULT 'N' COMMENT '청구 여부 (Y/N)',
                                      created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP COMMENT '생성일시',
                                      updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '수정일시',
                                      PRIMARY KEY (contract_id, sequence_number)
) COMMENT = '설치 이력';


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

-- 계약 할인 가입 이력 테이블
CREATE TABLE contract_discount (
                                   contract_id BIGINT NOT NULL COMMENT '계약 ID',
                                   discount_id VARCHAR(50) NOT NULL COMMENT '할인 ID',
                                   discount_start_date DATE NOT NULL COMMENT '할인 시작일',
                                   discount_end_date DATE NOT NULL COMMENT '할인 종료일',
                                   product_offering_id VARCHAR(50) NOT NULL COMMENT '상품 오퍼링 ID',
                                   discount_aply_unit VARCHAR(10) NOT NULL COMMENT '할인 적용 단위 (RATE:율, AMOUNT:금액)',
                                   discount_amt BIGINT COMMENT '할인 금액',
                                   discount_rate DECIMAL(15,2) COMMENT '할인 비율 (예: 10 = 10%)',
                                   discount_applied_amount DECIMAL(15,2) COMMENT '적용된 할인 금액',
    -- 복합 기본키: 계약ID + 할인ID + 할인 기간
                                   PRIMARY KEY (contract_id, discount_id, discount_start_date, discount_end_date)
) COMMENT='계약별 할인 가입 이력을 관리하는 테이블';

-- 계약 ID 기준 조회 성능을 위한 인덱스
CREATE INDEX idx_contract_discount_contract_id ON contract_discount(contract_id);

-- 할인 기간과 청구 기간 겹침 조회를 위한 인덱스
CREATE INDEX idx_contract_discount_date_range ON contract_discount(discount_start_date, discount_end_date);


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
                                    revenue_item_id VARCHAR(50) NOT NULL COMMENT '수익 항목 ID',
                                    effective_start_date DATE NOT NULL COMMENT '유효 시작일',
                                    effective_end_date DATE NOT NULL COMMENT '유효 종료일',
                                    suspension_type VARCHAR(30) COMMENT '정지 유형 (TEMPORARY_SUSPENSION, PARTIAL_SUSPENSION 등)',
                                    fee DECIMAL(15,2) NOT NULL COMMENT '계산된 요금',
                                    balance DECIMAL(15,2) NOT NULL COMMENT '잔액',

    -- 메타데이터
                                    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP COMMENT '생성 시각',

    -- 인덱스 (대용량 조회 성능 향상)
                                    INDEX idx_contract (contract_id)
) COMMENT = '월정액 요금 계산 결과 (평면화 구조)';

-- 파티셔닝을 위한 준비 (추후 대용량 데이터 처리시 월별 파티셔닝 가능)
-- ALTER TABLE calculation_result
-- PARTITION BY RANGE (YEAR(billing_start_date) * 100 + MONTH(billing_start_date)) (
--     PARTITION p202401 VALUES LESS THAN (202402),
--     PARTITION p202402 VALUES LESS THAN (202403),
--     -- 추가 파티션들...
-- );

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
                                     PRIMARY KEY (revenue_item_id, effective_end_date)

) COMMENT = '수익 항목 마스터 데이터 (시계열 구조)';


