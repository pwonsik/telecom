-- Oracle Bulk Sample Data Generation (1000 contracts)
-- This script generates 1000 additional contracts and related data.
-- It assumes master data (product_offering, revenue_master_data, charge_item) already exists.

SET SERVEROUTPUT ON;
SET DEFINE OFF;

DECLARE
  v_start_contract_id NUMBER := 1002; -- Start after existing sample data
  v_end_contract_id   NUMBER := v_start_contract_id + 999;
  v_subscribed_at     DATE;
  v_start_date        TIMESTAMP;
BEGIN
  DBMS_OUTPUT.PUT_LINE('Starting bulk insert of 1000 contracts from ' || v_start_contract_id || ' to ' || v_end_contract_id);

  FOR i IN v_start_contract_id..v_end_contract_id LOOP
    v_subscribed_at := TRUNC(SYSDATE) - DBMS_RANDOM.VALUE(1, 1095); -- Subscribed within the last 3 years
    v_start_date := TO_TIMESTAMP(TO_CHAR(v_subscribed_at, 'YYYY-MM-DD') || ' 00:00:00', 'YYYY-MM-DD HH24:MI:SS');

    -- 1. Insert Contract
    INSERT INTO contract (
      contract_id, subscribed_at, initially_subscribed_at, terminated_at, preffered_termination_date
    ) VALUES (
      i, v_subscribed_at, v_subscribed_at, NULL, NULL
    );

    -- 2. Insert Product Subscription
    INSERT INTO product (
      contract_id, product_offering_id, effective_start_date_time, effective_end_date_time,
      subscribed_at, activated_at, terminated_at
    ) VALUES (
      i, 'PO-BASIC', v_start_date, TIMESTAMP '9999-12-31 23:59:59',
      v_subscribed_at, v_subscribed_at, NULL
    );

    -- 3. (Conditional) Add temporary suspension for every 10th contract
    IF MOD(i, 10) = 0 THEN
      INSERT INTO suspension (
        contract_id, suspension_type_code, effective_start_date_time, effective_end_date_time, suspension_type_description
      ) VALUES (
        i, 'TEMP', v_start_date + 20, v_start_date + 25, 'Temporary Suspension'
      );
    END IF;

    -- 4. (Conditional) Add a discount for every 5th contract
    IF MOD(i, 5) = 0 THEN
      INSERT INTO contract_discount (
        contract_id, discount_id, discount_start_date, discount_end_date, product_offering_id,
        discount_aply_unit, discount_amt, discount_rate, discount_applied_amount
      ) VALUES (
        i, 'DISC-BULK-' || i, ADD_MONTHS(v_subscribed_at, 1), ADD_MONTHS(v_subscribed_at, 2) - 1, 'PO-BASIC', 'RATE', NULL, 15.00, NULL
      );
    END IF;

    -- 5. (Conditional) Add device installment for every 20th contract
    IF MOD(i, 20) = 0 THEN
      INSERT INTO device_installment_master (
        contract_id, installment_sequence, installment_start_date, total_installment_amount, installment_months, billed_count
      ) VALUES (
        i, 1, v_subscribed_at, 500000, 5, 0
      );
      FOR r IN 1..5 LOOP
        INSERT INTO device_installment_detail (
          contract_id, installment_sequence, installment_round, installment_amount, billing_completed_date
        ) VALUES (
          i, 1, r, 100000, NULL
        );
      END LOOP;
    END IF;

    -- 6. (Conditional) Add installation fee for every 7th contract
    IF MOD(i, 7) = 0 THEN
      INSERT INTO installation_history (
        contract_id, sequence_number, installation_date, installation_fee, billed_flag
      ) VALUES (
        i, 1, v_subscribed_at, 50000, 'N'
      );
    END IF;

    -- Commit every 100 records
    IF MOD(i, 100) = 0 THEN
      COMMIT;
      DBMS_OUTPUT.PUT_LINE('Committed up to contract_id: ' || i);
    END IF;

  END LOOP;

  COMMIT;
  DBMS_OUTPUT.PUT_LINE('Bulk insert completed successfully.');

EXCEPTION
  WHEN OTHERS THEN
    DBMS_OUTPUT.PUT_LINE('An error occurred: ' || SQLERRM);
    ROLLBACK;
END;
/
