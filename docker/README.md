# Docker 환경 설정 가이드

이 디렉토리에는 텔레콤 빌링 시스템의 Docker 환경 설정이 포함되어 있습니다.

## 📋 포함된 서비스

- **MySQL 8.0**: 메인 데이터베이스 (영구 볼륨 마운트)
- **Redis 7**: 캐시 및 세션 저장소 (영구 볼륨 마운트)
- **Adminer**: 데이터베이스 관리 도구 (웹 UI)

## 🚀 사용 방법

### 1. Docker 컨테이너 시작
```bash
# 백그라운드에서 모든 서비스 시작
docker-compose up -d

# 로그 확인
docker-compose logs -f

# 특정 서비스 로그 확인
docker-compose logs -f mysql
docker-compose logs -f redis
```

### 2. 서비스 접속 정보

#### MySQL
- **호스트**: localhost
- **포트**: 3306
- **데이터베이스**: telecom_billing
- **사용자**: telecom_user
- **비밀번호**: telecom_pass
- **루트 비밀번호**: telecom123!

#### Redis
- **호스트**: localhost
- **포트**: 6379
- **비밀번호**: 없음 (개발환경)

#### Adminer (데이터베이스 관리)
- **URL**: http://localhost:8080
- **시스템**: MySQL
- **서버**: mysql (컨테이너 이름)
- **사용자**: telecom_user 또는 root
- **비밀번호**: telecom_pass 또는 telecom123!

### 3. 초기 데이터 설정

컨테이너 시작 시 자동으로 실행됩니다:
- `ddl/create_tables.sql`: 테이블 생성
- `ddl/sample_data.sql`: 기본 샘플 데이터

### 4. 테스트 데이터 생성

```bash
# Docker 프로파일로 testgen 실행
./gradlew :testgen:bootRun --args="1000" -Dspring.profiles.active=docker

# 또는 JAR 실행
java -jar testgen/build/libs/testgen.jar 1000 --spring.profiles.active=docker
```

### 5. 컨테이너 관리

```bash
# 컨테이너 상태 확인
docker-compose ps

# 컨테이너 중지
docker-compose stop

# 컨테이너 완전 삭제 (데이터는 보존됨)
docker-compose down

# 컨테이너와 볼륨까지 삭제 (데이터 완전 삭제)
docker-compose down -v

# 서비스 재시작
docker-compose restart mysql
docker-compose restart redis
```

## 📁 데이터 영속화

### MySQL 데이터
- **볼륨 이름**: mysql_data
- **위치**: Docker 볼륨 (자동 관리)
- **확인**: `docker volume inspect telecom_mysql_data`

### Redis 데이터
- **볼륨 이름**: redis_data
- **위치**: Docker 볼륨 (자동 관리)
- **확인**: `docker volume inspect telecom_redis_data`

## ⚙️ 설정 파일

### MySQL 설정
- **파일**: `mysql/conf.d/mysql.cnf`
- **내용**: 문자셋, 시간대, 성능 최적화 설정

### Redis 설정
- **파일**: `redis/redis.conf`
- **내용**: 메모리, 영속화, 네트워크 설정

### 애플리케이션 설정
- **testgen**: `application-docker.yml`
- **domain**: `application-docker.yml`

## 🔧 문제 해결

### 포트 충돌 시
```bash
# 사용 중인 포트 확인
lsof -i :3306
lsof -i :6379
lsof -i :8080

# docker-compose.yml에서 포트 변경
ports:
  - "3307:3306"  # 호스트포트:컨테이너포트
```

### 볼륨 초기화
```bash
# 모든 데이터 삭제하고 재시작
docker-compose down -v
docker-compose up -d
```

### 컨테이너 로그 확인
```bash
# 실시간 로그
docker-compose logs -f

# 최근 로그 (마지막 100줄)
docker-compose logs --tail=100
```

## 🏗️ 개발 환경 통합

Spring Boot 애플리케이션에서 Docker 환경 사용:
```bash
# 프로파일 지정으로 실행
-Dspring.profiles.active=docker

# 또는 환경변수로 설정
export SPRING_PROFILES_ACTIVE=docker
```