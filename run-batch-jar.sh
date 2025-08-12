#!/bin/bash

# JAR 파일로 배치 실행 스크립트

echo "=== 배치 JAR 실행 스크립트 ==="

# 기본 파라미터 설정
DEFAULT_START_DATE="2025-03-01"
DEFAULT_END_DATE="2025-03-31"
THREAD_COUNT="8"

# 파라미터 입력 받기
echo "청구 시작일을 입력하세요 (기본값: $DEFAULT_START_DATE):"
read START_DATE
START_DATE=${START_DATE:-$DEFAULT_START_DATE}

echo "청구 종료일을 입력하세요 (기본값: $DEFAULT_END_DATE):"
read END_DATE
END_DATE=${END_DATE:-$DEFAULT_END_DATE}

echo "계약 ID를 입력하세요 (선택사항, 전체 조회하려면 엔터):"
read CONTRACT_ID

echo ""
echo "=== 배치 파라미터 ==="
echo "청구 시작일: $START_DATE"
echo "청구 종료일: $END_DATE"
echo "계약 ID: ${CONTRACT_ID:-전체}"
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

# 배치 실행 명령어 생성
if [ -z "$CONTRACT_ID" ]; then
    BATCH_COMMAND="java -jar $JAR_FILE --billingStartDate=$START_DATE --billingEndDate=$END_DATE --threadCount=$THREAD_COUNT"
else
    BATCH_COMMAND="java -jar $JAR_FILE --billingStartDate=$START_DATE --billingEndDate=$END_DATE --contractId=$CONTRACT_ID  --threadCount=$THREAD_COUNT"
fi

echo "실행 명령어: $BATCH_COMMAND"
echo ""

# 배치 실행
echo "=== 배치 실행 시작 ==="
$BATCH_COMMAND

echo ""
echo "=== 배치 실행 완료 ==="