# CLAUDE.md

## 프로젝트 개요

사용자 서비스(개인·기업 회원 대상)를 운영하기 위한 **관리자 서비스**. 사용자 서비스는 범위 밖이다.

- 사용 주체: 내부 운영자만 로그인 (멀티테넌트 아님)
- 기능: 기업정보관리, 사용자관리(개인·기업), 메뉴 동적관리, 코드관리, 통합게시판관리, 관리자관리, 관리자 권한관리, 관리자 역할관리
- 접근제어: RBAC. 관리자 →(N) 역할 →(N) 권한, **권한 = 메뉴 × 액션**

## 기술 스택

- 백엔드: 전자정부프레임워크(eGovFrame) 5.0.2 (Spring Boot 3 계열, Java 17 이상). 서버 애플리케이션 1개 (`/api/v1`, `/react`, `/jsp`, `/ssr`)
- UI: Bootstrap 5 + Tabler (세 프론트 공통), 에디터 Quill
- DB: PostgreSQL
- 인증: ① React, ② JSP + API는 토큰(JWT), ③ JSP SSR은 세션. REST API는 토큰 전용, JSP SSR 화면은 세션 전용
- 프론트엔드 3종 (학습 목적, 같은 화면 명세를 각각 구현)
  1. React + REST API
  2. JSP + REST API (AJAX)
  3. JSP 서버 렌더링 (Spring MVC)
- 비즈니스 로직은 Service 계층 하나에 두고, `@RestController`(1, 2용)와 `@Controller`(3용)가 같은 Service를 호출한다.

## 현재 단계: 기획 완료 (00~08 문서 확정), 개발 시작 전

- 개발은 [docs/08-architecture.md](docs/08-architecture.md) 9절의 개발 순서를 따른다.
- 기획 문서를 바꿔야 하면 해당 문서와 영향받는 문서를 함께 고친다.
- API는 명세 우선이다. API를 추가·변경할 때는 `api/openapi.yaml`을 먼저 고치고, 서버는 계약 테스트로, React는 생성 타입으로 맞춘다 ([docs/adr/0020-openapi-spec-first.md](docs/adr/0020-openapi-spec-first.md)).
- 제품 요구사항 요약은 [docs/prd.md](docs/prd.md), 설계 결정의 이유는 [docs/adr/](docs/adr/README.md)에 있다.
- 기존 결정을 바꾸거나 중요한 설계 결정을 새로 내리면 ADR을 추가한다. 기존 ADR은 지우지 않고 상태를 "대체됨"으로 바꾼다.

- 기획 문서는 `docs/` 아래 Markdown으로 작성한다. 진행 상태는 [docs/00-roadmap.md](docs/00-roadmap.md)를 기준으로 한다.
- 문서는 단계 순서대로 진행한다: 00 로드맵 → 01 개요 → 02 접근제어 → 03 ERD → 04 기능 명세 → 05 화면 명세 → 06 API 명세 → 07 비기능 → 08 아키텍처
- 각 문서 끝에 "미결 사항" 표를 두고, 결정되면 표에서 지우고 본문에 반영한다.
- 문서를 확정하면 00-roadmap.md의 상태 칸을 갱신한다.

## 작업 규칙

- 기획 단계에서는 **기획 문서만 만들고 수정한다.** 프로젝트 생성, 소스 코드, 빌드 설정, DB 스키마 생성은 사용자가 요청할 때만 한다.
- git 커밋은 사용자가 요청할 때만 한다.
- `main`에 직접 커밋·push하지 않는다. 작업 브랜치(`feat/...`, `fix/...`, `docs/...`)에서 작업하고 PR로 병합한다 ([docs/adr/0018-github-flow-and-ci.md](docs/adr/0018-github-flow-and-ci.md)).
- 저장소가 공개이므로 비밀값(DB 비밀번호, JWT 키, 초기 관리자 비밀번호)을 절대 커밋하지 않는다.
- 문서와 대화는 한국어로 작성한다.
- 기존 문서에서 정의한 용어는 [docs/01-overview.md](docs/01-overview.md)의 용어집을 따른다.
