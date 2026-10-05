# 관리자 서비스

사용자 서비스(개인·기업 회원)를 운영하기 위한 관리자 서비스입니다. 학습 목적으로 같은 화면 명세를 세 가지 방식으로 구현합니다.

| 프론트 | 경로 | 인증 |
|---|---|---|
| ① React | `/react` | 토큰 (JWT) |
| ② JSP + REST API | `/jsp` | 토큰 (JWT) |
| ③ JSP 서버 렌더링 | `/ssr` | 세션 |

- 백엔드: 전자정부프레임워크 5.0.2 (Spring Boot 3.5.6, Java 17), PostgreSQL 16
- 기획 문서: [docs/](docs/00-roadmap.md) · 결정 기록: [docs/adr/](docs/adr/README.md)

## 시작하기

필요한 것: JDK 17 이상, Node.js 22, Docker

```bash
cp .env.example .env          # DB_PASSWORD, JWT_SECRET, TEST_ADMIN_PASSWORD 채우기 (로컬은 COOKIE_SECURE=false)
docker compose up -d          # 개발용 PostgreSQL
(cd admin-react && npm install && npm run build)   # 서버가 /react 로 제공할 빌드
SPRING_PROFILES_ACTIVE=local ./mvnw -pl admin-web -am spring-boot:run   # 서버: http://localhost:8080
```

- http://localhost:8080/ 에서 세 프론트 중 하나를 고른다. 테스트 관리자(`t_super`, `t_system` 등)의 비밀번호는 `TEST_ADMIN_PASSWORD`다.
- React를 고치면서 보려면 `cd admin-react && npm run dev` → http://localhost:5173/react/ (API는 8080 서버로 넘어간다).

## 검증

```bash
./verify.sh            # 전체 (CI와 같은 검사)
./verify.sh backend    # 백엔드: 빌드·테스트 (Docker 필요)
./verify.sh frontend   # 프론트: lint·타입 검사·테스트·빌드
./verify.sh e2e        # 세 프론트 공통 E2E (Playwright, DB 필요)
```
