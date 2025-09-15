#!/bin/bash

# testgen 실행 스크립트 (메모리 최적화 버전)

set -e

# 기본값 설정
DEFAULT_CONTRACT_COUNT=100000
DEFAULT_MEMORY="4g"

# 사용법 출력
usage() {
    echo "사용법: $0 [옵션] <계약_개수>"
    echo ""
    echo "옵션:"
    echo "  -m, --memory <크기>    JVM 최대 메모리 크기 (기본값: ${DEFAULT_MEMORY})"
    echo "  -h, --help            도움말 출력"
    echo ""
    echo "예시:"
    echo "  $0 100000                    # 10만 건 생성 (기본 메모리)"
    echo "  $0 -m 8g 1000000            # 100만 건 생성 (8GB 메모리)"
    echo "  $0 --memory 2g 50000        # 5만 건 생성 (2GB 메모리)"
    exit 1
}

# 파라미터 파싱
MEMORY=$DEFAULT_MEMORY
CONTRACT_COUNT=""

while [[ $# -gt 0 ]]; do
    case $1 in
        -m|--memory)
            MEMORY="$2"
            shift 2
            ;;
        -h|--help)
            usage
            ;;
        -*)
            echo "알 수 없는 옵션: $1"
            usage
            ;;
        *)
            if [ -z "$CONTRACT_COUNT" ]; then
                CONTRACT_COUNT="$1"
            else
                echo "계약 개수는 하나만 지정할 수 있습니다: $1"
                usage
            fi
            shift
            ;;
    esac
done

# 계약 개수 검증
if [ -z "$CONTRACT_COUNT" ]; then
    echo "계약 개수를 입력해주세요."
    usage
fi

# 숫자인지 검증
if ! [[ "$CONTRACT_COUNT" =~ ^[0-9]+$ ]]; then
    echo "계약 개수는 숫자여야 합니다: $CONTRACT_COUNT"
    exit 1
fi

# 계약 개수 범위 검증
if [ "$CONTRACT_COUNT" -lt 1 ]; then
    echo "계약 개수는 1 이상이어야 합니다: $CONTRACT_COUNT"
    exit 1
fi

if [ "$CONTRACT_COUNT" -gt 10000000 ]; then
    echo "계약 개수가 너무 큽니다. 1000만 건 이하로 설정해주세요: $CONTRACT_COUNT"
    exit 1
fi

# JAR 파일 경로
JAR_FILE="testgen/build/libs/testgen-0.0.1-SNAPSHOT.jar"

# JAR 파일 존재 확인
if [ ! -f "$JAR_FILE" ]; then
    echo "JAR 파일이 존재하지 않습니다: $JAR_FILE"
    echo "gradle 빌드를 먼저 실행해주세요: ./gradlew :testgen:bootJar"
    exit 1
fi

# 메모리 크기에 따른 권장 사항 출력
if [ "$CONTRACT_COUNT" -ge 1000000 ]; then
    echo "⚠️  대용량 데이터 생성 모드 (100만 건 이상)"
    echo "   권장 메모리: 8GB 이상"
    echo "   예상 소요 시간: 30분 이상"

    if [[ "$MEMORY" =~ ^[1-3]g$ ]]; then
        echo "⚠️  경고: 메모리가 부족할 수 있습니다. 8GB 이상 권장"
        read -p "계속 진행하시겠습니까? (y/N): " -r
        if [[ ! $REPLY =~ ^[Yy]$ ]]; then
            echo "실행이 취소되었습니다."
            exit 1
        fi
    fi
elif [ "$CONTRACT_COUNT" -ge 100000 ]; then
    echo "📊 중간 규모 데이터 생성 모드 (10만~100만 건)"
    echo "   권장 메모리: 4GB 이상"
    echo "   예상 소요 시간: 5~30분"
else
    echo "🚀 소규모 데이터 생성 모드 (10만 건 미만)"
    echo "   권장 메모리: 2GB"
    echo "   예상 소요 시간: 5분 이내"
fi

echo ""
echo "=== TestGen 실행 준비 ==="
echo "계약 개수: $CONTRACT_COUNT 건"
echo "JVM 메모리: $MEMORY"
echo "JAR 파일: $JAR_FILE"
echo ""

# 실행 시작 시간
START_TIME=$(date +%s)

echo "=== TestGen 실행 시작 ==="
echo "시작 시간: $(date '+%Y-%m-%d %H:%M:%S')"
echo ""

# Java 실행 (극도의 메모리 최적화)
java \
    -Xms256m \
    -Xmx$MEMORY \
    -XX:+UseG1GC \
    -XX:MaxGCPauseMillis=100 \
    -XX:G1HeapRegionSize=16m \
    -XX:+UseStringDeduplication \
    -Xlog:gc*:gc.log \
    -XX:NewRatio=2 \
    -XX:SurvivorRatio=8 \
    -XX:MaxTenuringThreshold=1 \
    -Dspring.main.keep-alive=false \
    -jar "$JAR_FILE" \
    "$CONTRACT_COUNT"

# 실행 완료 후 결과 출력
EXIT_CODE=$?
END_TIME=$(date +%s)
DURATION=$((END_TIME - START_TIME))

echo ""
echo "=== TestGen 실행 완료 ==="
echo "종료 시간: $(date '+%Y-%m-%d %H:%M:%S')"
HOURS=$((DURATION / 3600))
MINUTES=$(((DURATION % 3600) / 60))
SECONDS=$((DURATION % 60))
echo "총 소요 시간: $(printf '%02d:%02d:%02d' $HOURS $MINUTES $SECONDS)"

if [ $EXIT_CODE -eq 0 ]; then
    echo "✅ 테스트 데이터 생성이 성공적으로 완료되었습니다!"
    echo "📊 생성된 계약 수: $CONTRACT_COUNT 건"

    # 성능 통계
    if [ $DURATION -gt 0 ]; then
        RATE=$((CONTRACT_COUNT / DURATION))
        echo "⚡ 처리 속도: $RATE 건/초"
    fi
else
    echo "❌ 테스트 데이터 생성 중 오류가 발생했습니다. (종료 코드: $EXIT_CODE)"
    exit $EXIT_CODE
fi