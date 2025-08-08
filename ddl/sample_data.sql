-- 샘플 데이터 삽입 스크립트
-- 텔레콤 빌링 시스템 테스트용 데이터

-- 1. contract 테이블 샘플 데이터
INSERT INTO contract (contract_id, subscribed_at, initially_subscribed_at, terminated_at, preffered_termination_date) VALUES
(1001, '2024-01-01', '2024-01-01', NULL, NULL),
(1002, '2024-02-15', '2024-02-15', '2024-07-15', NULL),
(1003, '2024-03-01', '2024-03-01', NULL, '2024-12-31');

-- 2. product_offering 테이블 샘플 데이터
INSERT INTO product_offering (product_offering_id, product_offering_name) VALUES
('PO001', '5G 프리미엄 요금제'),
('PO002', 'LTE 표준 요금제'),
('PO003', 'B2B 전용 요금제'),
('PO004', 'IoT 특별 요금제');

-- 3. product 테이블 샘플 데이터
INSERT INTO product (contract_id, product_offering_id, effective_start_date_time, effective_end_date_time, subscribed_at, activated_at, terminated_at) VALUES
-- 계약 1001의 상품들
(1001, 'PO001', '2024-01-01 00:00:00', '2024-05-16 15:59:59', '2024-01-01', '2024-01-01', NULL),
(1001, 'PO002', '2024-05-16 16:00:00', '2999-12-31 23:59:59', '2024-05-16', '2024-05-16', NULL),

-- 계약 1002의 상품들  
(1002, 'PO003', '2024-02-15 00:00:00', '2024-07-15 23:59:59', '2024-02-15', '2024-02-15', '2024-07-15'),

-- 계약 1003의 상품들
(1003, 'PO004', '2024-03-01 00:00:00', '2999-12-31 23:59:59', '2024-03-01', '2024-03-01', NULL);

-- 4. suspension 테이블 샘플 데이터
INSERT INTO suspension (contract_id, suspension_type_code, effective_start_date_time, effective_end_date_time, suspension_type_description) VALUES
-- 계약 1001의 정지 이력
(1001, 'F1', '2024-05-10 14:00:00', '2024-05-13 17:59:59', '일시정지'),

-- 계약 1002의 정지 이력
(1002, 'F3', '2024-06-01 00:00:00', '2024-06-15 23:59:59', '미납정지'),
(1002, 'F1', '2024-06-20 10:00:00', '2024-06-25 09:59:59', '일시정지');

-- 5. monthly_charge_item 테이블 샘플 데이터
INSERT INTO monthly_charge_item (product_offering_id, charge_item_id, charge_item_name, suspension_charge_ratio, calculation_method_code, calculation_method_name, flat_rate_amount, pricing_type) VALUES
-- PO001 (5G 프리미엄) 과금 항목들
('PO001', 'CI001', '5G 기본료', 0.5000, 'FLAT', '정액제', 89000.00, 'FLAT_RATE'),
('PO001', 'CI002', '5G 데이터 요금', 0.0000, 'FLAT', '정액제', 0.00, 'FLAT_RATE'),
('PO001', 'CI003', '5G 부가서비스료', 1.0000, 'FLAT', '정액제', 5000.00, 'FLAT_RATE'),

-- PO002 (LTE 표준) 과금 항목들
('PO002', 'CI004', 'LTE 기본료', 0.5000, 'FLAT', '정액제', 55000.00, 'FLAT_RATE'),
('PO002', 'CI005', 'LTE 데이터 요금', 0.0000, 'FLAT', '정액제', 0.00, 'FLAT_RATE'),

-- PO003 (B2B 전용) 과금 항목들  
('PO003', 'CI006', 'B2B 기본료', 0.3000, 'MATCHING', '매칭팩터', 100000.00, 'MATCHING_FACTOR'),
('PO003', 'CI007', 'B2B 회선료', 0.5000, 'UNIT_PRICE', '단가곱셈', 5000.00, 'UNIT_PRICE_FACTOR'),
('PO003', 'CI008', 'B2B 관리비', 1.0000, 'RANGE', '구간별', 20000.00, 'RANGE_FACTOR'),

-- PO004 (IoT 특별) 과금 항목들
('PO004', 'CI009', 'IoT 기본료', 0.0000, 'TIER', '구간별', 1000.00, 'TIER_FACTOR'),
('PO004', 'CI010', 'IoT 데이터 전송료', 0.0000, 'STEP', '단계별', 100.00, 'STEP_FACTOR');