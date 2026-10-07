# CLAUDE.md

## 프로젝트 개요

사용자 서비스(개인·기업 회원 대상)를 운영하기 위한 **관리자 서비스**. 사용자 서비스는 범위 밖이다.

- 사용 주체: 내부 운영자만 로그인 (멀티테넌트 아님)
- 기능: 기업정보관리, 사용자관리(개인·기업), 메뉴 동적관리, 코드관리, 통합게시판관리, 관리자관리, 관리자 권한관리, 관리자 역할관리
- 접근제어: RBAC. 관리자 →(N) 역할 →(N) 권한, **권한 = 메뉴 × 액션**

## 기술 스택

- 백엔드: 전자정부프레임워크(eGovFrame) 5.0.2 (Spring Boot 3.5.6, Java 17). 서버 애플리케이션 1개 (`/api/v1`, `/react`, `/jsp`, `/ssr`)
- DB: PostgreSQL 16, MyBatis, Flyway. 로깅은 Log4j2 (Logback 아님)
- UI: Bootstrap 5 + Tabler (세 프론트 공통), 에디터 Quill
- 인증: ① React, ② JSP + API는 토큰(JWT), ③ JSP SSR은 세션. REST API는 토큰 전용, JSP SSR 화면은 세션 전용
- 프론트엔드 3종 (학습 목적, 같은 화면 명세를 각각 구현)
  1. React + REST API (Vite, TypeScript, Vitest, oxlint)
  2. JSP + REST API (AJAX)
  3. JSP 서버 렌더링 (Spring MVC)
- 비즈니스 로직은 Service 계층 하나에 두고, `@RestController`(1, 2용)와 `@Controller`(3용)가 같은 Service를 호출한다.

## 디렉터리

| 경로 | 내용 |
|---|---|
| `admin-core/` | 업무 로직 (Service, Mapper, 공통). Flyway SQL은 `src/main/resources/db/migration/` |
| `admin-web/` | 실행 앱 (war). 컨트롤러, JSP(`src/main/webapp/WEB-INF/views/`), 설정 |
| `admin-react/` | ① React 프로젝트 (`src/api` API·토큰, `src/auth` 로그인 상태, `src/components` 공통, `src/pages` 화면) |
| `admin-web/src/main/webapp/WEB-INF/views/{ssr,jsp}` | ③ SSR, ② JSP + API 화면. 공통 레이아웃은 `WEB-INF/tags/` |
| `admin-web/src/main/resources/static/common/` | ②③ 공통 CSS·JS (`admin-api.js` 토큰·API, `admin-jsp.js` ② 레이아웃) |
| `e2e/` | 세 프론트 공통 Playwright E2E |
| `api/openapi.yaml` | API 명세 정본. 바꾸면 `cd admin-react && npm run gen:api`로 타입 재생성 |
| `docs/` | 기획 문서, PRD, ADR |
| `docs/data/` | 기계 판독용 명세 데이터 (권한 매트릭스 CSV 등). 원본 표를 바꾸면 함께 바꾼다 |
| `docs/traceability.md` | 기능 모듈별 구현 현황(서버·세 프론트·테스트). 기능을 구현하면 갱신한다 |

## 명령

| 목적 | 명령 |
|---|---|
| **작업 완료 전 검증 (필수)** | `./verify.sh` (전체) / `./verify.sh api` / `./verify.sh backend` / `./verify.sh frontend` |
| 화면을 바꿨을 때 E2E | `./verify.sh e2e` (DB가 떠 있어야 한다. 서버를 18080 포트로 띄우고 세 프론트를 검증한다) |
| 개발용 DB | `docker compose up -d` (`.env` 필요, `.env.example` 참고. 서버 실행에는 `JWT_SECRET`, 로컬은 `COOKIE_SECURE=false`도 필요) |
| 서버 실행 (테스트 데이터 포함) | `SPRING_PROFILES_ACTIVE=local ./mvnw -pl admin-web -am spring-boot:run` |
| React 개발 서버 | `cd admin-react && npm run dev` |

- 백엔드 테스트는 Testcontainers로 실제 PostgreSQL을 띄운다. Docker가 필요하다. H2는 쓰지 않는다.
- 추가 Maven 옵션은 `MVN_ARGS` 환경 변수로 `verify.sh`에 넘긴다.

## 코드 작성 규칙

- 컨트롤러(api, ssr 패키지)의 모든 메서드에 `@RequirePermission(menu, action)`, `@LoginOnly`, `@PublicEndpoint` 중 하나를 붙인다. 없으면 기본 거부(403)된다.
- 업무 규칙 위반은 Service에서 `BusinessException(ErrorCode)`를 던진다. 입력 칸 오류는 `BusinessException.field(...)`.
- 감사로그는 Service에서 `AuditLogService.record(...)`를 같은 트랜잭션 안에서 직접 호출한다. 개인정보는 마스킹한 값, 비밀번호·토큰은 넣지 않는다.
- 역할·권한·메뉴·관리자 상태를 바꾸면 `AdminAuthInfoService.evict/evictAll`로 권한 캐시를 비운다.

- 화면은 세 프론트(`/react`, `/jsp`, `/ssr`)가 같은 마크업 구조·같은 문구·같은 입력 칸 id를 쓴다. 한 프론트만 고치지 않는다.
- ② JSP 화면과 ① React는 API만 쓰고, ③ SSR은 Service를 직접 쓴다.

## 테스트 작성 규칙

- 통합 테스트는 `@IntegrationTest`(실제 PostgreSQL + 운영 필수 데이터 + MockMvc)를 붙인다.
  로그인이 필요하면 `@IntegrationTestWithData`(테스트 관리자 t_* 포함)와 `AuthTestSupport.login(mockMvc, "t_member")`를 쓴다.
  잠금·비밀번호 변경처럼 데이터를 바꾸는 테스트는 t_* 대신 `AuthTestSupport.createAdmin(...)`으로 만든 계정을 쓴다.
- API 테스트는 응답을 `OpenApiContract.assertValid(result)`로 `api/openapi.yaml`과 대조한다 (계약 테스트).
- 오류 코드를 추가하면 `ErrorCode`(Java)와 `openapi.yaml`의 `ErrorCode` enum을 함께 고친다 (`ErrorCodeSpecTest`가 확인).
- 권한 매트릭스를 바꾸면 `docs/02-access-model.md` 6절, `docs/data/permission-matrix.csv`, Flyway 데이터를 함께 고친다 (`SeedDataTest`가 확인).
- DB 구조를 바꿀 때는 기존 Flyway 파일을 고치지 않고 새 버전(`V5__...`)을 추가한다.
- 화면 시나리오는 `e2e/tests/`에 한 번만 쓰고 세 프론트에서 실행한다 (`test.info().project.metadata.prefix`).

## 기능 구현

- 기능 모듈 하나를 구현할 때는 `/implement-feature <기능 ID 접두어>` 명령의 순서(명세→DB→Service→API→세 프론트→테스트→추적표)를 따른다.
- 진행 현황은 [docs/traceability.md](docs/traceability.md)에서 확인하고, 구현 후 갱신한다.

## 현재 단계: M2(코드관리·메뉴관리) 완료, M3 시작 전

- 개발은 [docs/08-architecture.md](docs/08-architecture.md) 9절의 개발 순서를 따른다.
- API는 명세 우선이다. API를 추가·변경할 때는 `api/openapi.yaml`을 먼저 고치고, 서버는 계약 테스트로, React는 생성 타입으로 맞춘다 ([docs/adr/0020-openapi-spec-first.md](docs/adr/0020-openapi-spec-first.md)).
- 제품 요구사항 요약은 [docs/prd.md](docs/prd.md), 설계 결정의 이유는 [docs/adr/](docs/adr/README.md)에 있다.
- 기획 문서를 바꿔야 하면 해당 문서와 영향받는 문서를 함께 고친다.
- 기존 결정을 바꾸거나 중요한 설계 결정을 새로 내리면 ADR을 추가한다. 기존 ADR은 지우지 않고 상태를 "대체됨"으로 바꾼다.
- 각 문서 끝의 "미결 사항" 표는 결정되면 지우고 본문에 반영한다.

## 작업 규칙

- git 커밋은 사용자가 요청하거나 진행을 승인한 단계에서만 한다.
- `main`에 직접 커밋·push하지 않는다. 작업 브랜치(`feat/...`, `fix/...`, `docs/...`)에서 작업하고 PR로 병합한다 ([docs/adr/0018-github-flow-and-ci.md](docs/adr/0018-github-flow-and-ci.md)).
- 저장소가 공개이므로 비밀값(DB 비밀번호, JWT 키, 초기 관리자 비밀번호)을 절대 커밋하지 않는다. `.env`는 `.gitignore`에 있다.
- 작업을 마쳤다고 보고하기 전에 `./verify.sh`를 실행해 통과를 확인한다.
- 문서와 대화는 한국어로 작성한다.
- 용어는 [docs/01-overview.md](docs/01-overview.md)의 용어집을 따른다.
