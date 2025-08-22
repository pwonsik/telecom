# 텔레콤 빌링 시스템 DDL

이 디렉토리에는 텔레콤 빌링 시스템의 데이터베이스 스키마와 샘플 데이터가 포함되어 있습니다.

## 파일 구성

- `create_tables.sql`: 테이블 생성 DDL 스크립트
- `sample_data.sql`: 테스트용 샘플 데이터
- `README.md`: 이 파일

## 테이블 구조

### 1. contractWithProductsAndSuspensions (계약 정보)
- **Primary Key**: `contract_id`
- 계약의 기본 정보와 가입/해지 날짜 관리

### 2. product_offering (상품 오퍼링)
- **Primary Key**: `product_offering_id`  
- 텔레콤 상품의 기본 정보 관리

### 3. product (상품 정보)
- **Primary Key**: `contract_id`, `product_offering_id`, `effective_start_date_time`, `effective_end_date_time`
- 계약별 가입 상품과 유효 기간 관리

### 4. suspension (정지 정보)
- **Primary Key**: `contract_id`, `suspension_type_code`, `effective_start_date_time`, `effective_end_date_time`
- 계약별 서비스 정지 이력 관리

### 5. monthly_charge_item (월정액 과금 항목)
- **Primary Key**: `product_offering_id`, `charge_item_id`
- 상품별 과금 항목과 가격 정책 관리

## 데이터베이스 설정 방법

```bash
# 1. MySQL 접속
mysql -u root -p

# 2. 데이터베이스 생성
CREATE DATABASE telecom_billing DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
USE telecom_billing;

# 3. 테이블 생성
SOURCE create_tables.sql;

# 4. 샘플 데이터 삽입 (선택사항)
SOURCE sample_data.sql;
```

## 주요 특징

- **복합 키 구조**: 이력성 데이터의 정확한 관리를 위한 복합 Primary Key 사용
- **외래키 제약조건**: 데이터 무결성 보장
- **인덱스 최적화**: 조회 성능 향상을 위한 적절한 인덱스 구성
- **일할계산 지원**: 날짜/시간 기반의 정밀한 빌링 계산 지원

## 비즈니스 규칙

1. **가입일 포함, 해지일 제외**: 일할계산에서 가입일은 포함하되 해지일은 제외
2. **정지 기간 과금**: `suspension_charge_ratio`에 따른 정지 기간 과금 적용
3. **유효 기간 검증**: `effective_start_date_time < effective_end_date_time` 조건 보장
4. **다양한 가격 정책**: FLAT_RATE, MATCHING_FACTOR, UNIT_PRICE_FACTOR 등 지원