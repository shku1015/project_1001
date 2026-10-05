---
description: 기능 모듈 하나를 명세→서버→세 프론트→테스트 순서로 구현한다
argument-hint: "<기능 ID 접두어> (예: COD 코드관리, MNU 메뉴관리)"
---

기능 모듈 **$1**을 아래 순서로 구현한다. 각 단계에서 관련 기획 문서를 먼저 읽고, 이미 있는 공통 코드(인증·권한·응답·마스킹·감사로그·레이아웃)를 재사용한다. 한 프론트만 고치지 않는다.

## 사전 확인
1. `docs/04-features/`에서 이 기능의 기능 명세(기능 ID, 비즈니스 규칙 BR, 상태 전이, 감사로그 대상)를 읽는다.
2. `docs/05-screens/`에서 화면 명세(검색 조건, 목록 칼럼, 입력 항목·검증, 버튼별 권한·노출 조건)를 읽는다.
3. `docs/06-api/`에서 API 목록과 요청·응답, 업무 오류 코드를 읽는다.
4. `docs/traceability.md`에서 이 기능의 현재 상태를 확인한다.

## 구현 순서 (ADR-0020 명세 우선, ADR-0003 Service 하나)
1. **명세**: `api/openapi.yaml`에 이 모듈의 경로·스키마를 먼저 추가하고 `./verify.sh api`로 검사. 새 오류 코드는 `ErrorCode`(Java)와 openapi `ErrorCode` enum을 함께 추가.
2. **DB**: 구조 변경이 필요하면 새 Flyway 버전(`V5__...` 등)을 추가한다. 기존 파일은 고치지 않는다.
3. **admin-core**: Mapper(+XML), Service(업무 규칙·검증·마스킹·감사로그). 역할·권한·메뉴·관리자 상태를 바꾸면 `AdminAuthInfoService.evict/evictAll`.
4. **API 컨트롤러**(`web.api`): `@RestController` + 메서드마다 `@RequirePermission`/`@LoginOnly`. 공통 응답·오류 처리기를 쓴다.
5. **① React**(`admin-react/src/pages`, `api`): `npm run gen:api`로 타입 생성 후 화면·API 호출.
6. **② JSP + API**(`views/jsp`): 같은 레이아웃을 `admin-jsp.js`가 API로 채운다.
7. **③ JSP SSR**(`web.ssr` + `views/ssr`): `@Controller`가 Service를 직접 호출. 같은 마크업·문구·입력 칸 id.
8. **테스트**: API 계약 테스트(`OpenApiContract.assertValid`), 권한 테스트, 데이터 규칙 테스트, E2E 시나리오(`e2e/tests`, 세 프론트 공통).
9. **추적표**: `docs/traceability.md`에서 이 기능의 서버·①·②·③·테스트 칸을 갱신.

## 마무리
- `./verify.sh`(전체)와 화면을 바꿨으면 `./verify.sh e2e`가 통과하는지 확인한다.
- 커밋·PR은 사용자가 요청하거나 승인할 때만 한다. 작업 브랜치 이름은 `feat/{마일스톤}-$1`.
