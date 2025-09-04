# Telecom Billing System Architecture

이 디렉토리는 통신 과금 시스템의 아키텍처를 시각화한 Mermaid 다이어그램들을 포함합니다.

## 📋 문서 목록

### [01-system-class-diagram.md](./01-system-class-diagram.md)
**전체 시스템 클래스 다이어그램**
- 시스템의 전체 구조와 클래스 간 관계
- API, Application, Domain, Batch, Infrastructure 레이어
- 헥사고날 아키텍처 패턴 적용

### [02-batch-sequence-diagram.md](./02-batch-sequence-diagram.md)  
**배치 처리 시퀀스 다이어그램**
- Spring Batch 기반 처리 플로우
- 6단계 계산 과정 (월정액 → 일회성 → 구간분리 → 할인 → 통합 → VAT)
- 청크 기반 멀티스레딩 처리

### [03-onetimecharge-extension-diagram.md](./03-onetimecharge-extension-diagram.md)
**OneTimeCharge 확장 구조 다이어그램**
- Spring DI를 활용한 플러그인 아키텍처
- 제로 코드 확장성 (Zero-Code Extensibility)
- 마커 인터페이스와 제네릭을 통한 타입 안전성

## 🏗️ 시스템 아키텍처 개요

### 핵심 설계 원칙

1. **헥사고날 아키텍처**
   - Port & Adapter 패턴
   - 도메인 중심 설계
   - 인프라스트럭처 의존성 격리

2. **확장 가능한 계산기 패턴**
   - `Calculator<T>` 통합 인터페이스
   - Spring `@Order` 기반 실행 순서 제어
   - Stream API를 활용한 함수형 처리

3. **제로 코드 확장성**
   - OneTimeCharge 타입 추가 시 기존 코드 수정 불필요
   - Spring DI의 `@Component` 스캐닝 활용
   - Map 기반 자동 타입 매칭

### 주요 처리 플로우

```
계약 데이터 로딩 
    ↓
월정액 계산 (BaseFeeCalculator)
    ↓
일회성 과금 계산 (OneTimeChargeCalculators)
    ↓
구간 분리 (CalculationResultProrater.prorate)
    ↓
할인 적용 (DiscountCalculator)
    ↓
구간 통합 (CalculationResultProrater.consolidate) ⭐NEW⭐
    ↓
VAT 계산 (VatCalculator)
    ↓
결과 저장
```

## 🚀 최신 업데이트

### 구간 통합 기능 추가 (2025)
- **CalculationResultProrater.consolidate()** 메서드 추가
- `contract_id` + `revenue_item_id` 기준 그룹화
- `fee`와 `balance` 자동 합계 계산
- 분산된 계산 결과의 통합 처리

### Spring Boot 3 & Java 21 적용
- Jakarta EE 마이그레이션 완료
- Record 패턴 적극 활용
- Stream API와 함수형 프로그래밍

### 성능 최적화
- 청크 기반 배치 처리 (기본 100건)
- MyBatis Cursor 기반 대용량 데이터 처리
- HikariCP 커넥션 풀 최적화
- Bulk Insert/Update 패턴

## 🔧 개발 가이드

### 새로운 OneTimeCharge 타입 추가하기

1. **Domain 객체 생성** (`OneTimeChargeDomain` 구현)
2. **DataLoader 생성** (`@Component` 등록)
3. **Calculator 생성** (`@Component`, `@Order` 지정)

→ 기존 코드 수정 없이 자동으로 처리 플로우에 통합됨

### 새로운 Calculator 추가하기

1. **Calculator 인터페이스 구현**
2. **@Order 어노테이션으로 실행 순서 지정**
3. **Spring Boot 재시작**

→ 자동으로 계산 파이프라인에 포함됨

## 📊 성능 지표

- **처리 속도**: 계약당 평균 50ms 이내
- **메모리 사용량**: 청크 단위 처리로 일정 유지
- **확장성**: 새 타입 추가 시 성능 영향 없음
- **안정성**: 타입 안전성과 컴파일 타임 검증

---

> 📝 **Note**: 모든 다이어그램은 Mermaid 형식으로 작성되어 GitHub, GitLab, VS Code 등에서 바로 렌더링됩니다.