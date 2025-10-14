-- Oracle DDL for Telecom Billing (calculation-related tables)
-- Safe drops (ignore if table does not exist)

SET DEFINE OFF;

BEGIN EXECUTE IMMEDIATE 'DROP TABLE device_installment_detail PURGE'; EXCEPTION WHEN OTHERS THEN IF SQLCODE != -942 THEN RAISE; END IF; END;
/
BEGIN EXECUTE IMMEDIATE 'DROP TABLE device_installment_master PURGE'; EXCEPTION WHEN OTHERS THEN IF SQLCODE != -942 THEN RAISE; END IF; END;
/
BEGIN EXECUTE IMMEDIATE 'DROP TABLE suspension PURGE'; EXCEPTION WHEN OTHERS THEN IF SQLCODE != -942 THEN RAISE; END IF; END;
/
BEGIN EXECUTE IMMEDIATE 'DROP TABLE product PURGE'; EXCEPTION WHEN OTHERS THEN IF SQLCODE != -942 THEN RAISE; END IF; END;
/
BEGIN EXECUTE IMMEDIATE 'DROP TABLE charge_item PURGE'; EXCEPTION WHEN OTHERS THEN IF SQLCODE != -942 THEN RAISE; END IF; END;
/
BEGIN EXECUTE IMMEDIATE 'DROP TABLE product_offering PURGE'; EXCEPTION WHEN OTHERS THEN IF SQLCODE != -942 THEN RAISE; END IF; END;
/
BEGIN EXECUTE IMMEDIATE 'DROP TABLE installation_history PURGE'; EXCEPTION WHEN OTHERS THEN IF SQLCODE != -942 THEN RAISE; END IF; END;
/
BEGIN EXECUTE IMMEDIATE 'DROP TABLE contract_discount PURGE'; EXCEPTION WHEN OTHERS THEN IF SQLCODE != -942 THEN RAISE; END IF; END;
/
BEGIN EXECUTE IMMEDIATE 'DROP TABLE calculation_result PURGE'; EXCEPTION WHEN OTHERS THEN IF SQLCODE != -942 THEN RAISE; END IF; END;
/
BEGIN EXECUTE IMMEDIATE 'DROP TABLE contract PURGE'; EXCEPTION WHEN OTHERS THEN IF SQLCODE != -942 THEN RAISE; END IF; END;
/
BEGIN EXECUTE IMMEDIATE 'DROP TABLE revenue_master_data PURGE'; EXCEPTION WHEN OTHERS THEN IF SQLCODE != -942 THEN RAISE; END IF; END;
/

-- revenue_master_data
CREATE TABLE revenue_master_data (
  revenue_item_id                 VARCHAR2(50)   NOT NULL,
  effective_end_date              DATE           NOT NULL,
  effective_start_date            DATE           NOT NULL,
  revenue_item_name               VARCHAR2(200)  NOT NULL,
  overdue_charge_revenue_item_id  VARCHAR2(50),
  vat_revenue_item_id             VARCHAR2(50),
  created_at                      TIMESTAMP      DEFAULT SYSTIMESTAMP,
  updated_at                      TIMESTAMP      DEFAULT SYSTIMESTAMP,
  CONSTRAINT pk_revenue_master_data PRIMARY KEY (revenue_item_id, effective_end_date)
);

-- charge_item
CREATE TABLE charge_item (
  product_offering_id     VARCHAR2(50)   NOT NULL,
  charge_item_id          VARCHAR2(50)   NOT NULL,
  charge_item_name        VARCHAR2(200)  NOT NULL,
  revenue_item_id         VARCHAR2(50)   NOT NULL,
  suspension_charge_ratio NUMBER(5,4)    DEFAULT 0 NOT NULL,
  calculation_method_code VARCHAR2(20)   NOT NULL,
  calculation_method_name VARCHAR2(100)  NOT NULL,
  flat_rate_amount        NUMBER(15,2),
  pricing_type            VARCHAR2(20)   NOT NULL,
  created_at              TIMESTAMP      DEFAULT SYSTIMESTAMP,
  updated_at              TIMESTAMP      DEFAULT SYSTIMESTAMP,
  CONSTRAINT pk_charge_item PRIMARY KEY (product_offering_id, charge_item_id)
);

-- product
CREATE TABLE product (
  contract_id                NUMBER(19)   NOT NULL,
  product_offering_id        VARCHAR2(50) NOT NULL,
  effective_start_date_time  TIMESTAMP    NOT NULL,
  effective_end_date_time    TIMESTAMP    NOT NULL,
  subscribed_at              DATE         NOT NULL,
  activated_at               DATE,
  terminated_at              DATE,
  created_at                 TIMESTAMP    DEFAULT SYSTIMESTAMP,
  updated_at                 TIMESTAMP    DEFAULT SYSTIMESTAMP,
  CONSTRAINT pk_product PRIMARY KEY (contract_id, product_offering_id, effective_start_date_time, effective_end_date_time)
);

-- suspension
CREATE TABLE suspension (
  contract_id                   NUMBER(19)   NOT NULL,
  suspension_type_code          VARCHAR2(10) NOT NULL,
  effective_start_date_time     TIMESTAMP    NOT NULL,
  effective_end_date_time       TIMESTAMP    NOT NULL,
  suspension_type_description   VARCHAR2(100) NOT NULL,
  created_at                    TIMESTAMP    DEFAULT SYSTIMESTAMP,
  updated_at                    TIMESTAMP    DEFAULT SYSTIMESTAMP,
  CONSTRAINT pk_suspension PRIMARY KEY (contract_id, suspension_type_code, effective_start_date_time, effective_end_date_time)
);

-- product_offering
CREATE TABLE product_offering (
  product_offering_id   VARCHAR2(50)  PRIMARY KEY,
  product_offering_name VARCHAR2(200) NOT NULL,
  created_at            TIMESTAMP     DEFAULT SYSTIMESTAMP,
  updated_at            TIMESTAMP     DEFAULT SYSTIMESTAMP
);

-- device_installment_detail
CREATE TABLE device_installment_detail (
  contract_id           NUMBER(19) NOT NULL,
  installment_sequence  NUMBER(19) NOT NULL,
  installment_round     NUMBER(10) NOT NULL,
  installment_amount    NUMBER(10,0) NOT NULL,
  billing_completed_date DATE,
  created_at            TIMESTAMP DEFAULT SYSTIMESTAMP,
  updated_at            TIMESTAMP DEFAULT SYSTIMESTAMP,
  CONSTRAINT pk_device_installment_detail PRIMARY KEY (contract_id, installment_sequence, installment_round)
);

-- device_installment_master
CREATE TABLE device_installment_master (
  contract_id            NUMBER(19)  NOT NULL,
  installment_sequence   NUMBER(19)  NOT NULL,
  installment_start_date DATE        NOT NULL,
  total_installment_amount NUMBER(10,0) NOT NULL,
  installment_months     NUMBER(10)  NOT NULL,
  billed_count           NUMBER(10)  DEFAULT 0 NOT NULL,
  created_at             TIMESTAMP   DEFAULT SYSTIMESTAMP,
  updated_at             TIMESTAMP   DEFAULT SYSTIMESTAMP,
  CONSTRAINT pk_device_installment_master PRIMARY KEY (contract_id, installment_sequence)
);

-- installation_history
CREATE TABLE installation_history (
  contract_id        NUMBER(19)  NOT NULL,
  sequence_number    NUMBER(19)  NOT NULL,
  installation_date  DATE        NOT NULL,
  installation_fee   NUMBER(10,0) NOT NULL,
  billed_flag        CHAR(1)     DEFAULT 'N' NOT NULL,
  created_at         TIMESTAMP   DEFAULT SYSTIMESTAMP,
  updated_at         TIMESTAMP   DEFAULT SYSTIMESTAMP,
  CONSTRAINT pk_installation_history PRIMARY KEY (contract_id, sequence_number)
);

-- contract
CREATE TABLE contract (
  contract_id              NUMBER(19)  PRIMARY KEY,
  subscribed_at            DATE        NOT NULL,
  initially_subscribed_at  DATE        NOT NULL,
  terminated_at            DATE,
  preffered_termination_date DATE,
  created_at               TIMESTAMP   DEFAULT SYSTIMESTAMP,
  updated_at               TIMESTAMP   DEFAULT SYSTIMESTAMP
);

-- contract_discount
CREATE TABLE contract_discount (
  contract_id             NUMBER(19)   NOT NULL,
  discount_id             VARCHAR2(50) NOT NULL,
  discount_start_date     DATE         NOT NULL,
  discount_end_date       DATE         NOT NULL,
  product_offering_id     VARCHAR2(50) NOT NULL,
  discount_aply_unit      VARCHAR2(10) NOT NULL,
  discount_amt            NUMBER(19,0),
  discount_rate           NUMBER(15,2),
  discount_applied_amount NUMBER(15,2),
  CONSTRAINT pk_contract_discount PRIMARY KEY (contract_id, discount_id, discount_start_date, discount_end_date)
);

CREATE INDEX idx_contract_discount_contract_id ON contract_discount(contract_id);
CREATE INDEX idx_contract_discount_date_range ON contract_discount(discount_start_date, discount_end_date);

-- calculation_result
CREATE TABLE calculation_result (
  calculation_result_id NUMBER(19) GENERATED BY DEFAULT AS IDENTITY PRIMARY KEY,
  contract_id           NUMBER(19)   NOT NULL,
  billing_start_date    DATE         NOT NULL,
  billing_end_date      DATE         NOT NULL,
  product_offering_id   VARCHAR2(50) NOT NULL,
  charge_item_id        VARCHAR2(50) NOT NULL,
  revenue_item_id       VARCHAR2(50) NOT NULL,
  effective_start_date  DATE         NOT NULL,
  effective_end_date    DATE         NOT NULL,
  suspension_type       VARCHAR2(30),
  fee                   NUMBER(15,2) NOT NULL,
  balance               NUMBER(15,2) NOT NULL,
  created_at            TIMESTAMP    DEFAULT SYSTIMESTAMP
);

-- Optional index to speed up contract queries
CREATE INDEX idx_calculation_result_contract ON calculation_result(contract_id);

COMMIT;
