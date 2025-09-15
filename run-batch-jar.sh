#!/bin/bash

# JAR 파일로 배치 실행 스크립트

echo "=== 배치 JAR 실행 스크립트 ==="

# 기본 파라미터 설정
DEFAULT_START_DATE="2025-03-01"
DEFAULT_END_DATE="2025-03-31"
DEFAULT_THREAD_COUNT="8"
DEFAULT_BILLING_CALCULATION_TYPE="B0"
DEFAULT_BILLING_CALCULATION_PERIOD="0"

# 파라미터 입력 받기
echo "청구 시작일을 입력하세요 (기본값: $DEFAULT_START_DATE):"
read START_DATE
START_DATE=${START_DATE:-$DEFAULT_START_DATE}

echo "청구 종료일을 입력하세요 (기본값: $DEFAULT_END_DATE):"
read END_DATE
END_DATE=${END_DATE:-$DEFAULT_END_DATE}

echo "스레드 수를 입력하세요 (기본값: $DEFAULT_THREAD_COUNT, application property 오버라이드):"
read THREAD_COUNT
THREAD_COUNT=${THREAD_COUNT:-$DEFAULT_THREAD_COUNT}

echo "청구 계산 유형을 입력하세요 (기본값: $DEFAULT_BILLING_CALCULATION_TYPE):"
read BILLING_CALCULATION_TYPE
BILLING_CALCULATION_TYPE=${BILLING_CALCULATION_TYPE:-$DEFAULT_BILLING_CALCULATION_TYPE}

echo "청구 계산 기간을 입력하세요 (기본값: $DEFAULT_BILLING_CALCULATION_PERIOD):"
read BILLING_CALCULATION_PERIOD
BILLING_CALCULATION_PERIOD=${BILLING_CALCULATION_PERIOD:-$DEFAULT_BILLING_CALCULATION_PERIOD}

echo "계약 ID를 입력하세요 (선택사항, 전체 조회하려면 엔터. 여러 계약 처리하려면 ,로 구분):"
read CONTRACT_IDS

echo ""
echo "=== 배치 파라미터 ==="
echo "청구 시작일: $START_DATE"
echo "청구 종료일: $END_DATE"
echo "스레드 수: $THREAD_COUNT (application property 오버라이드)"
echo "청구 계산 유형: $BILLING_CALCULATION_TYPE (application property 오버라이드)"
echo "청구 계산 기간: $BILLING_CALCULATION_PERIOD (application property 오버라이드)"
echo "계약 ID: ${CONTRACT_IDS:-전체}"
echo ""

# JAR 파일 빌드
echo "JAR 파일을 빌드합니다..."
./gradlew :batch:bootJar

if [ $? -ne 0 ]; then
    echo "JAR 빌드에 실패했습니다."
    exit 1
fi

# JAR 파일 위치
JAR_FILE="batch/build/libs/batch-0.0.1-SNAPSHOT.jar"

if [ ! -f "$JAR_FILE" ]; then
    echo "JAR 파일을 찾을 수 없습니다: $JAR_FILE"
    exit 1
fi

# 중복 실행 방지를 위한 현재 시각 타임스탬프
TIMESTAMP=$(date +%s)

# 배치 실행 명령어 생성 (모든 파라미터를 Application Property로 전달)
if [ -z "$CONTRACT_IDS" ]; then
    BATCH_COMMAND="java -jar $JAR_FILE --spring.batch.job.names=monthlyFeeCalculationJob timestamp=$TIMESTAMP --billingStartDate=$START_DATE --billingEndDate=$END_DATE --batch.thread-count=$THREAD_COUNT --billingCalculationType=$BILLING_CALCULATION_TYPE --billingCalculationPeriod=$BILLING_CALCULATION_PERIOD "
else
    BATCH_COMMAND="java -jar $JAR_FILE --spring.batch.job.names=monthlyFeeCalculationJob timestamp=$TIMESTAMP --billingStartDate=$START_DATE --billingEndDate=$END_DATE --contractIds=$CONTRACT_IDS --batch.thread-count=$THREAD_COUNT --billingCalculationType=$BILLING_CALCULATION_TYPE --billingCalculationPeriod=$BILLING_CALCULATION_PERIOD"
fi

echo "실행 명령어: $BATCH_COMMAND"
echo ""

# 배치 실행
echo "=== 배치 실행 시작 ==="
$BATCH_COMMAND

echo ""
echo "=== 배치 실행 완료 ==="