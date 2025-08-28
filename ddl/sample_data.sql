-- 샘플 데이터 삽입 스크립트
-- 텔레콤 빌링 시스템 테스트용 데이터

-- 2. product_offering 테이블 샘플 데이터
INSERT INTO product_offering (product_offering_id, product_offering_name) VALUES
('PO001', '5G 프리미엄 요금제'),
('PO002', 'LTE 표준 요금제'),
('PO003', 'B2B 전용 요금제'),
('PO004', 'IoT 특별 요금제');

-- 5. charge_item 테이블 샘플 데이터
INSERT INTO charge_item (product_offering_id, charge_item_id, charge_item_name, revenue_item_id, suspension_charge_ratio, calculation_method_code, calculation_method_name, flat_rate_amount, pricing_type) VALUES
-- PO001 (5G 프리미엄) 과금 항목들
('PO001', 'CI001', '5G 기본료', 'REV_MONTHLY_001', 0.5000, 'FLAT', '정액제', 89000.00, 'FLAT_RATE'),
('PO001', 'CI002', '5G 데이터 요금', 'REV_MONTHLY_002', 0.0000, 'FLAT', '정액제', 0.00, 'FLAT_RATE'),
('PO001', 'CI003', '5G 부가서비스료', 'REV_ADDON_001', 1.0000, 'FLAT', '정액제', 5000.00, 'FLAT_RATE'),

-- PO002 (LTE 표준) 과금 항목들
('PO002', 'CI004', 'LTE 기본료', 'REV_MONTHLY_001', 0.5000, 'FLAT', '정액제', 55000.00, 'FLAT_RATE'),
('PO002', 'CI005', 'LTE 데이터 요금', 'REV_MONTHLY_002', 0.0000, 'FLAT', '정액제', 0.00, 'FLAT_RATE'),

-- PO003 (B2B 전용) 과금 항목들  
('PO003', 'CI006', 'B2B 기본료', 'REV_MONTHLY_003', 0.3000, 'MATCHING', '매칭팩터', 100000.00, 'MATCHING_FACTOR'),
('PO003', 'CI007', 'B2B 회선료', 'REV_MONTHLY_003', 0.5000, 'UNIT_PRICE', '단가곱셈', 5000.00, 'UNIT_PRICE_FACTOR'),
('PO003', 'CI008', 'B2B 관리비', 'REV_MONTHLY_003', 1.0000, 'RANGE', '구간별', 20000.00, 'RANGE_FACTOR'),

-- PO004 (IoT 특별) 과금 항목들
('PO004', 'CI009', 'IoT 기본료', 'REV_MONTHLY_004', 0.0000, 'TIER', '구간별', 1000.00, 'TIER_FACTOR'),
('PO004', 'CI010', 'IoT 데이터 전송료', 'REV_MONTHLY_004', 0.0000, 'STEP', '단계별', 100.00, 'STEP_FACTOR');

-- Revenue Master Data 샘플 데이터
-- VAT 계산 테스트를 위한 기본 수익 항목과 VAT 수익 항목 매핑

-- 1. 월정액 관련 수익 항목
INSERT INTO revenue_master_data (
    revenue_item_id,
    effective_start_date,
    effective_end_date,
    revenue_item_name,
    overdue_charge_revenue_item_id,
    vat_revenue_item_id
) VALUES
      ('REV_MONTHLY_001', '2024-01-01', '2999-12-31', '기본 월정액', 'REV_OVERDUE_001', 'REV_VAT_001'),
      ('REV_MONTHLY_002', '2024-01-01', '2999-12-31', '프리미엄 월정액', 'REV_OVERDUE_001', 'REV_VAT_002'),
      ('REV_MONTHLY_003', '2024-01-01', '2999-12-31', '데이터 월정액', 'REV_OVERDUE_001', 'REV_VAT_003'),
    ('REV_MONTHLY_004', '2024-01-01', '2999-12-31', 'iot 월정액', 'REV_OVERDUE_001', 'REV_VAT_004');

-- 2. 일회성 요금 관련 수익 항목
INSERT INTO revenue_master_data (
    revenue_item_id,
    effective_start_date,
    effective_end_date,
    revenue_item_name,
    overdue_charge_revenue_item_id,
    vat_revenue_item_id
) VALUES
      ('REV_INSTALL_001', '2024-01-01', '2999-12-31', '설치비', 'REV_OVERDUE_001', 'REV_VAT_INSTALL_001'),
      ('REV_DEVICE_001', '2024-01-01', '2999-12-31', '단말 할부금', 'REV_OVERDUE_DEVICE_001', 'REV_VAT_DEVICE_001');

-- 3. 부가 서비스 관련 수익 항목
INSERT INTO revenue_master_data (
    revenue_item_id,
    effective_start_date,
    effective_end_date,
    revenue_item_name,
    overdue_charge_revenue_item_id,
    vat_revenue_item_id
) VALUES
      ('REV_ADDON_001', '2024-01-01', '2999-12-31', '부가서비스 A', 'REV_OVERDUE_001', 'REV_VAT_ADDON_001'),
      ('REV_ADDON_002', '2024-01-01', '2999-12-31', '부가서비스 B', 'REV_OVERDUE_002', 'REV_VAT_ADDON_002');

-- 4. VAT 전용 수익 항목 (VAT 계산 결과를 저장하는 항목들)
INSERT INTO revenue_master_data (
    revenue_item_id,
    effective_start_date,
    effective_end_date,
    revenue_item_name,
    overdue_charge_revenue_item_id,
    vat_revenue_item_id
) VALUES
-- VAT 항목들은 자기 자신에게 VAT를 적용하지 않음 (vat_revenue_item_id = NULL)
('REV_VAT_001', '2024-01-01', '2999-12-31', '기본 월정액 VAT', 'REV_OVERDUE_001', NULL),
('REV_VAT_002', '2024-01-01', '2999-12-31', '프리미엄 월정액 VAT', 'REV_OVERDUE_001', NULL),
('REV_VAT_003', '2024-01-01', '2999-12-31', '데이터 월정액 VAT', 'REV_OVERDUE_001', NULL),
('REV_VAT_004', '2024-01-01', '2999-12-31', 'IOT 기본료 VAT', 'REV_OVERDUE_001', NULL),
('REV_VAT_INSTALL_001', '2024-01-01', '2999-12-31', '설치비 VAT', 'REV_OVERDUE_001', NULL),
('REV_VAT_DEVICE_001', '2024-01-01', '2999-12-31', '단말 할부금 VAT', 'REV_OVERDUE_DEVICE_001', NULL),
('REV_VAT_ADDON_001', '2024-01-01', '2999-12-31', '부가서비스 A VAT', 'REV_OVERDUE_001', NULL),
('REV_VAT_ADDON_002', '2024-01-01', '2999-12-31', '부가서비스 B VAT', 'REV_OVERDUE_001', NULL);

-- 5. 연체료 관련 수익 항목 (참고용)
INSERT INTO revenue_master_data (
    revenue_item_id,
    effective_start_date,
    effective_end_date,
    revenue_item_name,
    overdue_charge_revenue_item_id,
    vat_revenue_item_id
) VALUES
      ('REV_OVERDUE_001', '2024-01-01', '2999-12-31', '기본 월정액 연체료', NULL, null),
      ('REV_OVERDUE_DEVICE_001', '2024-01-01', '2999-12-31', '단말 할부금 연체료', NULL, null),

-- 7. VAT 면세 대상 수익 항목 (VAT 적용 안 됨)
INSERT INTO revenue_master_data (
    revenue_item_id,
    effective_start_date,
    effective_end_date,
    revenue_item_name,
    overdue_charge_revenue_item_id,
    vat_revenue_item_id
) VALUES
    ('REV_EXEMPT_001', '2024-01-01', '2999-12-31', '면세 서비스 A', 'REV_OVERDUE_EXEMPT_001', NULL),
    ('REV_EXEMPT_002', '2024-01-01', '2999-12-31', '면세 서비스 B', 'REV_OVERDUE_EXEMPT_002', NULL);