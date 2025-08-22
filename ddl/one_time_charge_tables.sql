-- 단말할부 마스터 테이블
CREATE TABLE device_installment_master (
    contract_id BIGINT NOT NULL COMMENT '계약 ID',
    installment_sequence BIGINT NOT NULL COMMENT '할부 일련번호',
    installment_start_date DATE NOT NULL COMMENT '할부 시작일',
    total_installment_amount DECIMAL(10, 0) NOT NULL COMMENT '할부금 총액',
    installment_months INT NOT NULL COMMENT '할부 개월수',
    billed_count INT NOT NULL DEFAULT 0 COMMENT '할부 청구 횟수',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP COMMENT '생성일시',
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '수정일시',
    PRIMARY KEY (contract_id, installment_sequence),
    INDEX idx_contract_id (contract_id),
    INDEX idx_installment_start_date (installment_start_date)
) COMMENT = '단말할부 마스터';

-- 단말할부 상세 테이블
CREATE TABLE device_installment_detail (
    contract_id BIGINT NOT NULL COMMENT '계약 ID',
    installment_sequence BIGINT NOT NULL COMMENT '할부 일련번호',
    installment_round INT NOT NULL COMMENT '할부 회차 (1부터 시작)',
    installment_amount DECIMAL(10, 0) NOT NULL COMMENT '회차별 할부금',
    billing_completed_date DATE NULL COMMENT '청구 완료일 (NULL이면 미청구)',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP COMMENT '생성일시',
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '수정일시',
    PRIMARY KEY (contract_id, installment_sequence, installment_round),
    FOREIGN KEY (contract_id, installment_sequence) 
        REFERENCES device_installment_master(contract_id, installment_sequence) 
        ON DELETE CASCADE,
    INDEX idx_billing_completed_date (billing_completed_date),
    INDEX idx_contract_installment (contract_id, installment_sequence)
) COMMENT = '단말할부 상세';

-- 설치 이력 테이블
CREATE TABLE installation_history (
    contract_id BIGINT NOT NULL COMMENT '계약 ID',
    sequence_number BIGINT NOT NULL COMMENT '일련번호',
    installation_date DATE NOT NULL COMMENT '설치일',
    installation_fee DECIMAL(10, 0) NOT NULL COMMENT '설치비',
    billed_flag CHAR(1) NOT NULL DEFAULT 'N' COMMENT '청구 여부 (Y/N)',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP COMMENT '생성일시',
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '수정일시',
    PRIMARY KEY (contract_id, sequence_number),
    INDEX idx_contract_id (contract_id),
    INDEX idx_installation_date (installation_date),
    INDEX idx_billed_flag (billed_flag),
    CHECK (billed_flag IN ('Y', 'N'))
) COMMENT = '설치 이력';