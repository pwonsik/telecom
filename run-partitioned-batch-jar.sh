#!/bin/bash

# Partitioner 기반 배치 JAR 실행 스크립트

echo "=== Partitioner 기반 배치 JAR 실행 스크립트 ==="

# 기본 파라미터 설정
DEFAULT_START_DATE="2025-03-01"
DEFAULT_END_DATE="2025-03-31"
DEFAULT_THREAD_COUNT="8"
BILLING_CALCULATION_TYPE="B0"
BILLING_CALCULATION_PERIOD="0"
JOB_NAME="partitionedMonthlyFeeCalculationJob"

# 파라미터 입력 받기
echo "청구 시작일을 입력하세요 (기본값: $DEFAULT_START_DATE):"
read START_DATE
START_DATE=${START_DATE:-$DEFAULT_START_DATE}

echo "청구 종료일을 입력하세요 (기본값: $DEFAULT_END_DATE):"
read END_DATE
END_DATE=${END_DATE:-$DEFAULT_END_DATE}

echo "파티션 수 (쓰레드 수)를 입력하세요 (기본값: $DEFAULT_THREAD_COUNT):"
read THREAD_COUNT
THREAD_COUNT=${THREAD_COUNT:-$DEFAULT_THREAD_COUNT}

echo "계약 ID를 입력하세요 (선택사항, 전체 조회하려면 엔터. 여러 계약 처리하려면 ,로 구분):"
read CONTRACT_IDS

echo ""
echo "=== Partitioner 배치 파라미터 ==="
echo "Job 이름: $JOB_NAME"
echo "청구 시작일: $START_DATE"
echo "청구 종료일: $END_DATE"
echo "파티션 수 (쓰레드 수): $THREAD_COUNT"
echo "계약 ID: ${CONTRACT_IDS:-전체}"
echo ""
echo "파티션 로직: contractId % $THREAD_COUNT = partitionKey"
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

# 배치 실행 명령어 생성 (Partitioner Job 지정)
if [ -z "$CONTRACT_IDS" ]; then
    BATCH_COMMAND="java -jar $JAR_FILE --spring.batch.job.names=$JOB_NAME --billingStartDate=$START_DATE --billingEndDate=$END_DATE --threadCount=$THREAD_COUNT --billingCalculationType=$BILLING_CALCULATION_TYPE --billingCalculationPeriod=$BILLING_CALCULATION_PERIOD"
else
    BATCH_COMMAND="java -jar $JAR_FILE --spring.batch.job.names=$JOB_NAME --billingStartDate=$START_DATE --billingEndDate=$END_DATE --contractIds=$CONTRACT_IDS --threadCount=$THREAD_COUNT --billingCalculationType=$BILLING_CALCULATION_TYPE --billingCalculationPeriod=$BILLING_CALCULATION_PERIOD"
fi

echo "실행 명령어: $BATCH_COMMAND"
echo ""

# 실행 전 확인
echo "Partitioner 방식으로 배치를 실행하시겠습니까? (y/n)"
read CONFIRM
if [ "$CONFIRM" != "y" ] && [ "$CONFIRM" != "Y" ]; then
    echo "배치 실행이 취소되었습니다."
    exit 0
fi

# 배치 실행
echo "=== Partitioner 배치 실행 시작 ==="
echo "각 파티션이 독립적으로 처리됩니다..."
echo ""

$BATCH_COMMAND

echo ""
echo "=== Partitioner 배치 실행 완료 ==="

# 실행 후 성능 비교 안내
echo ""
echo "=== 성능 비교를 위한 참고사항 ==="
echo "Thread Pool 방식과 비교하려면 다음 명령어를 사용하세요:"
echo "./run-batch-jar.sh"
echo ""
echo "주요 차이점:"
echo "- Thread Pool: 동적 작업 분배, 메모리 공유"
echo "- Partitioner: 정적 작업 분배 (contractId % $THREAD_COUNT), 파티션 독립성"