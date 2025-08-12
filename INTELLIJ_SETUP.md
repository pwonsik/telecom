# IntelliJ에서 배치 실행 가이드

## MySQL 드라이버 문제 해결

### 1. Gradle 캐시 정리
```bash
./gradlew clean build --refresh-dependencies
```

### 2. IntelliJ 설정
1. **File > Project Structure > Modules**
2. **batch 모듈 선택**
3. **Dependencies 탭에서 MySQL connector 확인**
4. 없다면 **+ > Library > From Maven** 클릭
5. `com.mysql:mysql-connector-j:8.3.0` 검색하여 추가

### 3. Run Configuration 설정
1. **Run > Edit Configurations...**
2. **+ > Application** 추가
3. 다음과 같이 설정:
   - **Name**: BatchApplication
   - **Main class**: `me.realimpact.telecom.billing.batch.BatchApplication`
   - **Module**: batch
   - **Working directory**: `/Users/a06000/Documents/nova/telecom`
   - **VM options**: `-Dspring.profiles.active=default`
   - **Program arguments**: `--server.port=8080`

### 4. 환경 변수 설정 (필요시)
**Environment variables**에 추가:
```
SPRING_DATASOURCE_URL=jdbc:mysql://localhost:3306/telecom_billing?useSSL=false&serverTimezone=Asia/Seoul&characterEncoding=UTF-8
SPRING_DATASOURCE_USERNAME=user
SPRING_DATASOURCE_PASSWORD=1234
SPRING_DATASOURCE_DRIVER_CLASS_NAME=com.mysql.cj.jdbc.Driver
```

## 배치 실행 방법

### 방법 1: 명령행 스크립트
```bash
# 대화형 스크립트
./run-batch.sh

# 빠른 실행 (기본값)
./quick-batch.sh
```

### 방법 2: 수동 실행
```bash
# 1. 배치 애플리케이션 시작
./gradlew :batch:bootRun

# 2. 별도 터미널에서 배치 실행
curl -X POST "http://localhost:8080/batch/monthly-fee-calculation?billingStartDate=2024-03-01&billingEndDate=2024-03-31"
```

### 방법 3: IntelliJ에서 실행
1. **BatchApplication** Run Configuration으로 실행
2. 애플리케이션이 시작되면 다음 URL로 POST 요청:
   ```
   http://localhost:8080/batch/monthly-fee-calculation?billingStartDate=2024-03-01&billingEndDate=2024-03-31
   ```

## 주요 파라미터

- **billingStartDate**: 청구 시작일 (필수) - 예: 2024-03-01
- **billingEndDate**: 청구 종료일 (필수) - 예: 2024-03-31  
- **contractId**: 특정 계약 ID (선택) - 예: 12345

## 트러블슈팅

### 1. MySQL 연결 오류
```bash
# MySQL 서비스 시작
brew services start mysql

# 또는 H2 데이터베이스 사용
# application.yml에서 datasource 설정을 H2로 변경
```

### 2. 포트 충돌
```bash
# 다른 포트 사용
./gradlew :batch:bootRun --args="--server.port=8081"
```

### 3. 권한 문제
```bash
chmod +x *.sh
```