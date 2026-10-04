#!/usr/bin/env bash
# 통합 검증 명령. 에이전트와 사람 모두 "작업 완료" 전에 이 명령이 통과해야 한다.
# CI(.github/workflows/ci.yml)도 같은 단계를 실행한다.
#
# 사용:
#   ./verify.sh            전체 (백엔드 + 프론트)
#   ./verify.sh backend    백엔드만: 컴파일, 테스트(Testcontainers, Docker 필요)
#   ./verify.sh frontend   프론트만: lint, 타입 검사, 테스트, 빌드
#
# 추가 Maven 옵션은 MVN_ARGS 환경 변수로 넘긴다. 예: MVN_ARGS="-Dmaven.repo.local=/tmp/m2"
set -euo pipefail

ROOT="$(cd "$(dirname "$0")" && pwd)"
TARGET="${1:-all}"

backend() {
    echo "== backend: ./mvnw verify"
    (cd "$ROOT" && ./mvnw -B -ntp ${MVN_ARGS:-} verify)
}

frontend() {
    echo "== frontend: admin-react"
    cd "$ROOT/admin-react"
    if [ ! -d node_modules ]; then
        npm ci --no-audit --no-fund
    fi
    npm run lint
    npm run typecheck
    npm test
    npm run build
}

case "$TARGET" in
    backend) backend ;;
    frontend) frontend ;;
    all) backend; frontend ;;
    *) echo "사용법: $0 [all|backend|frontend]" >&2; exit 2 ;;
esac

echo "== verify: 통과"
