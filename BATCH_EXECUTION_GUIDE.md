# 배치 실행 가이드

## 개요
웹서비스가 아닌 순수 배치 프로세스로 월정액 계산을 실행하는 방법을 안내합니다.

## 실행 방법

### 1. JAR 파일로 실행 (권장)

#### 대화형 스크립트
```bash
./run-batch-jar.sh
```

#### 빠른 실행 (기본값)
```bash
./quick-batch-jar.sh
```

#### 수동 실행
```bash
# 1. JAR 빌드
./gradlew :batch:bootJar

# 2. 배치 실행
java -jar batch/build/libs/batch-0.0.1-SNAPSHOT.jar \
  --billingStartDate=2024-03-01 \
  --billingEndDate=2024-03-31

# 3. 특정 계약만 처리
java -jar batch/build/libs/batch-0.0.1-SNAPSHOT.jar \
  --billingStartDate=2024-03-01 \
  --billingEndDate=2024-03-31 \
  --contractId=12345
```

### 2. Gradle bootRun으로 실행

#### 대화형 스크립트
```bash
./run-batch-gradle.sh
```

#### 빠른 실행 (기본값)
```bash
./quick-batch-gradle.sh
```

#### 수동 실행
```bash
# 전체 계약 처리
./gradlew :batch:bootRun --args="--billingStartDate=2024-03-01 --billingEndDate=2024-03-31"

# 특정 계약만 처리  
./gradlew :batch:bootRun --args="--billingStartDate=2024-03-01 --billingEndDate=2024-03-31 --contractId=12345"
```

## 파라미터 설명

### 필수 파라미터
- **billingStartDate**: 청구 시작일 (형식: YYYY-MM-DD)
- **billingEndDate**: 청구 종료일 (형식: YYYY-MM-DD)

### 선택 파라미터
- **contractId**: 특정 계약 ID (숫자)
  - 생략시 전체 계약 처리

## 실행 예시

```bash
# 예시 1: 2024년 3월 전체 계약 처리
java -jar batch/build/libs/batch-0.0.1-SNAPSHOT.jar \
  --billingStartDate=2024-03-01 \
  --billingEndDate=2024-03-31

# 예시 2: 특정 계약(ID: 12345) 처리
java -jar batch/build/libs/batch-0.0.1-SNAPSHOT.jar \
  --billingStartDate=2024-03-01 \
  --billingEndDate=2024-03-31 \
  --contractId=12345

# 예시 3: 2024년 2월 처리
java -jar batch/build/libs/batch-0.0.1-SNAPSHOT.jar \
  --billingStartDate=2024-02-01 \
  --billingEndDate=2024-02-29
```

## 로그 확인

배치 실행 중 로그는 콘솔에 출력됩니다. 로그 레벨은 application.yml에서 조정할 수 있습니다.

```yaml
logging:
  level:
    me.realimpact.telecom: INFO
    org.springframework.batch: INFO
```

## 데이터베이스 설정

### MySQL 사용 (기본값)
application.yml에서 MySQL 연결 정보를 확인하세요:
```yaml
spring:
  datasource:
    url: jdbc:mysql://localhost:3306/telecom_billing?useSSL=false&serverTimezone=Asia/Seoul&characterEncoding=UTF-8
    username: user
    password: 1234
```

### H2 데이터베이스로 테스트
MySQL이 없는 환경에서는 H2로 테스트할 수 있습니다:
```yaml
spring:
  datasource:
    url: jdbc:h2:mem:testdb;DB_CLOSE_DELAY=-1
    username: sa
    password: 
    driver-class-name: org.h2.Driver
```

## 트러블슈팅

### 1. MySQL 연결 오류
```bash
# MySQL 서비스 시작
brew services start mysql

# 또는 Docker로 MySQL 실행
docker run --name mysql-test -e MYSQL_ROOT_PASSWORD=1234 -e MYSQL_DATABASE=telecom_billing -e MYSQL_USER=user -e MYSQL_PASSWORD=1234 -p 3306:3306 -d mysql:8.0
```

### 2. JAR 파일이 없는 경우
```bash
./gradlew :batch:bootJar
```

### 3. 권한 문제
```bash
chmod +x *.sh
```

### 4. 메모리 부족
```bash
java -Xmx2g -jar batch/build/libs/batch-0.0.1-SNAPSHOT.jar --billingStartDate=2024-03-01 --billingEndDate=2024-03-31
```

## 배치 작업 모니터링

Spring Batch는 실행 히스토리를 데이터베이스에 저장합니다:
- BATCH_JOB_INSTANCE
- BATCH_JOB_EXECUTION
- BATCH_STEP_EXECUTION

이 테이블들을 조회해서 배치 실행 상태를 확인할 수 있습니다.