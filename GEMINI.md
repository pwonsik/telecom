# GEMINI.md — Telecom Billing Project Context

## 📘 프로젝트 개요
이 프로젝트는 **통신 요금 계산 시스템**으로, 현재는 계약 단위의 요금 산출 로직 중심으로 구현되어 있습니다.  
이후 확장 목표는 **청구서 생성(batch billing, invoice generation)** 기능까지 포함하는 것입니다.

요금 계산은 Spring Batch를 기반으로 하며, Oracle 23c를 사용합니다.  
특히, `tmth_bill` 테이블에는 `bill_data`를 JSON 컬럼으로 저장하여, 섹터별 요금 정보와 부가 데이터를 구조적으로 관리합니다.

---

## 🧩 기술 스택
- **Java 21*
- **Spring Boot 3.x**
- **Spring Batch**
- **MyBatis
- **Oracle 23c (JSON 컬럼 포함)**
- 헥사고날 아키텍쳐를 사용



# GEMINI.md — Telecom Billing Project Context

## 📘 프로젝트 개요
이 프로젝트는 **통신 요금 계산 시스템**으로, 현재는 계약 단위의 요금 산출 로직 중심으로 구현되어 있습니다.  
이후 확장 목표는 **청구서 생성(batch billing, invoice generation)** 기능까지 포함하는 것입니다.

요금 계산은 Spring Batch를 기반으로 하며, Oracle 23c를 사용합니다.  
특히, `tmth_bill` 테이블에는 `bill_data`를 JSON 컬럼으로 저장하여, 섹터별 요금 정보와 부가 데이터를 구조적으로 관리합니다.

---

## 🧩 기술 스택
- **Java 17**
- **Spring Boot 3.x**
- **Spring Batch**
- **MyBatis + QueryDSL**
- **Oracle 23c (JSON 컬럼 포함)**
- **Docker 기반 로컬 개발 환경**

---

## ⚙️ 주요 모듈
| 모듈 | 설명 |
|------|------|
| `batch` | 배치 작업(Job/Step) 정의. 청구서 생성, 요금 산출, 로그 수집 등 |
| `domain` | 도메인 로직 (요금정책, 계약정보, 할인계산 등) |
| `infrastructure` | DB 접근(MyBatis Mapper, Repository) 및 외부 시스템 연동 |
| `reader`, `writer` | Spring Batch 전용 ItemReader/ItemWriter 구현체들 |
| `calculation-policy-*` | 정책별 계산 알고리즘 모듈화 (예: 월정액, 부가서비스 등) |

---

## 🧠 Gemini 지시 사항
Gemini는 이 프로젝트에서 다음 원칙을 따르도록 합니다:

### 1. 코드 스타일
- **Google Java Style Guide** 준수  
- 체이닝 메서드는 줄바꿈  
- 인덴트 4칸  
- 로깅은 `Slf4j` 사용 (`log.info`, `log.debug`)  
- 주석은 한글 가능, 단 기술용어는 영어 병기  

### 2. 도메인 이해
- `CalculationTarget`, `CalculationParameters`, `BillingContext` 등은 **요금 계산 단위 객체**
- `tmth_bill` 은 **청구서 마스터 테이블**
- 향후 `bill_data`(JSON) 내에는 **섹터별 세부 금액 구조**가 포함됨  
  ```json
  {
    "base_fee": 12000,
    "voice_fee": 3400,
    "data_fee": 7800,
    "discount": 1500,
    "total": 21700
  }


---

## 🧠 Gemini 지시 사항
소스는 C:\Users\user\Documents\GitHub\telecom에 있어.
소스에 대한 개선을 물어볼때 소스를 바로 수정하지 말고 일단 소스를 보여줘.
소스를 보여줄때는 복사하기 쉽게 라인넘버를 빼고 보여줘


