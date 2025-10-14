-- Oracle Sample Data for Telecom Billing
SET DEFINE OFF;

-- Clear existing data
DELETE FROM calculation_result;
DELETE FROM contract_discount;
DELETE FROM installation_history;
DELETE FROM device_installment_detail;
DELETE FROM device_installment_master;
DELETE FROM suspension;
DELETE FROM product;
DELETE FROM charge_item;
DELETE FROM product_offering;
DELETE FROM contract;
DELETE FROM revenue_master_data;
COMMIT;

-- Master data
INSERT INTO product_offering (product_offering_id, product_offering_name) VALUES ('PO-BASIC', 'Basic Plan');

INSERT INTO revenue_master_data (
  revenue_item_id, effective_start_date, effective_end_date, revenue_item_name, overdue_charge_revenue_item_id, vat_revenue_item_id
) VALUES (
  'REV-MONTHLY', DATE '2020-01-01', DATE '9999-12-31', 'Monthly Fee', NULL, 'REV-VAT'
);

INSERT INTO charge_item (
  product_offering_id, charge_item_id, charge_item_name, revenue_item_id, suspension_charge_ratio,
  calculation_method_code, calculation_method_name, flat_rate_amount, pricing_type
) VALUES (
  'PO-BASIC', 'CH-MRC', 'Monthly Recurring Charge', 'REV-MONTHLY', 0.0000, 'FLAT', '정액', 10000.00, 'FLAT'
);

-- Contracts
INSERT INTO contract (
  contract_id, subscribed_at, initially_subscribed_at, terminated_at, preffered_termination_date
) VALUES (
  1001, DATE '2024-01-10', DATE '2024-01-10', NULL, NULL
);

-- Product subscriptions (effective period open-ended)
INSERT INTO product (
  contract_id, product_offering_id, effective_start_date_time, effective_end_date_time,
  subscribed_at, activated_at, terminated_at
) VALUES (
  1001, 'PO-BASIC', TIMESTAMP '2024-01-10 00:00:00', TIMESTAMP '9999-12-31 23:59:59',
  DATE '2024-01-10', DATE '2024-01-10', NULL
);

-- Optional: temporary suspension sample overlapping a billing window
INSERT INTO suspension (
  contract_id, suspension_type_code, effective_start_date_time, effective_end_date_time, suspension_type_description
) VALUES (
  1001, 'TEMP', TIMESTAMP '2024-02-10 00:00:00', TIMESTAMP '2024-02-15 23:59:59', 'Temporary Suspension'
);

-- Discount sample (10% for a month)
INSERT INTO contract_discount (
  contract_id, discount_id, discount_start_date, discount_end_date, product_offering_id,
  discount_aply_unit, discount_amt, discount_rate, discount_applied_amount
) VALUES (
  1001, 'DISC-10PCT', DATE '2024-02-01', DATE '2024-02-29', 'PO-BASIC', 'RATE', NULL, 10.00, NULL
);

-- Device installments (2 months, unpaid yet)
INSERT INTO device_installment_master (
  contract_id, installment_sequence, installment_start_date, total_installment_amount, installment_months, billed_count
) VALUES (
  1001, 1, DATE '2024-01-10', 200000, 2, 0
);

INSERT INTO device_installment_detail (
  contract_id, installment_sequence, installment_round, installment_amount, billing_completed_date
) VALUES (
  1001, 1, 1, 100000, NULL
);

INSERT INTO device_installment_detail (
  contract_id, installment_sequence, installment_round, installment_amount, billing_completed_date
) VALUES (
  1001, 1, 2, 100000, NULL
);

-- Installation history (unbilled)
INSERT INTO installation_history (
  contract_id, sequence_number, installation_date, installation_fee, billed_flag
) VALUES (
  1001, 1, DATE '2024-01-10', 30000, 'N'
);

COMMIT;
