-- 샘플 데이터 삽입 스크립트
-- 텔레콤 빌링 시스템 테스트용 데이터

-- 2. product_offering 테이블 샘플 데이터
INSERT INTO product_offering (product_offering_id, product_offering_name) VALUES
('PO001', '5G 프리미엄 요금제'),
('PO002', 'LTE 표준 요금제'),
('PO003', 'B2B 전용 요금제'),
('PO004', 'IoT 특별 요금제');

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