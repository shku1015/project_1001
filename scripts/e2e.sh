#!/usr/bin/env bash
# 세 프론트 공통 E2E: React 빌드 → 서버 패키징 → 서버 실행(local 프로필, 테스트 데이터) → Playwright → 서버 종료.
# ./verify.sh e2e 와 CI의 e2e 작업이 이 스크립트를 실행한다.
#
# 필요한 것: DB(PostgreSQL)가 떠 있어야 한다 (로컬: docker compose up -d, CI: services).
# 환경 변수: DB_HOST/DB_PORT/DB_NAME/DB_USERNAME/DB_PASSWORD, JWT_SECRET, TEST_ADMIN_PASSWORD
#           (로컬은 루트의 .env를 읽는다). 포트는 E2E_PORT (기본 18080).
set -euo pipefail

ROOT="$(cd "$(dirname "$0")/.." && pwd)"
PORT="${E2E_PORT:-18080}"

# .env의 값은 환경 변수로 이미 지정되지 않은 것만 쓴다 (CI·임시 DB로 실행할 때 덮어쓰지 않도록)
if [ -f "$ROOT/.env" ]; then
    while IFS='=' read -r key value; do
        case "$key" in ''|\#*) continue ;; esac
        if [ -z "${!key+x}" ]; then
            export "$key=$value"
        fi
    done < "$ROOT/.env"
fi
: "${TEST_ADMIN_PASSWORD:?TEST_ADMIN_PASSWORD가 필요합니다 (.env 또는 환경 변수)}"
: "${JWT_SECRET:?JWT_SECRET가 필요합니다 (.env 또는 환경 변수)}"

echo "== e2e: React 빌드"
(cd "$ROOT/admin-react" && { [ -d node_modules ] || npm ci --no-audit --no-fund; } && npm run build)

echo "== e2e: 서버 패키징"
(cd "$ROOT" && ./mvnw -B -ntp -q ${MVN_ARGS:-} -DskipTests package)

echo "== e2e: 서버 실행 (포트 $PORT)"
mkdir -p "$ROOT/logs"
SPRING_PROFILES_ACTIVE=local COOKIE_SECURE=false REACT_DIST="file:$ROOT/admin-react/dist/" \
    java -jar "$ROOT/admin-web/target/admin-web.war" --server.port="$PORT" > "$ROOT/logs/e2e-server.log" 2>&1 &
SERVER_PID=$!
trap 'kill $SERVER_PID 2>/dev/null || true' EXIT

for _ in $(seq 1 90); do
    if curl -sf "http://localhost:$PORT/actuator/health" > /dev/null; then
        break
    fi
    if ! kill -0 $SERVER_PID 2>/dev/null; then
        echo "서버가 시작하지 못했습니다. logs/e2e-server.log를 확인하세요." >&2
        tail -40 "$ROOT/logs/e2e-server.log" >&2
        exit 1
    fi
    sleep 1
done
curl -sf "http://localhost:$PORT/actuator/health" > /dev/null || { echo "서버 헬스 체크 실패" >&2; exit 1; }

echo "== e2e: Playwright (/react, /jsp, /ssr)"
cd "$ROOT/e2e"
[ -d node_modules ] || npm ci --no-audit --no-fund
E2E_BASE_URL="http://localhost:$PORT" npx playwright test "$@"
