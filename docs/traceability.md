# 추적표

기능이 명세·화면·API·테스트·세 프론트에 빠짐없이 구현됐는지 한곳에서 본다.
기능 모듈을 구현하면(`/implement-feature`) 해당 줄의 상태를 갱신한다.

- 상태: ✅ 완료 / 🏗 진행 중 / ⬜ 예정 / `-` 해당 없음
- 세 프론트: ① React, ② JSP + API, ③ JSP SSR

## 1. 모듈별 진행 현황

| 모듈 | 기능 명세 | 화면 명세 | API 명세 | 서버 | ① | ② | ③ | 테스트 | 마일스톤 |
|---|---|---|---|---|---|---|---|---|---|
| 로그인·내 정보 (AUTH) | ✅ | ✅ | ✅ | ✅ | ✅ | ✅ | ✅ | ✅ | M1 |
| 공통 레이아웃·홈 | ✅ | ✅ | ✅ | ✅ | ✅ | ✅ | ✅ | ✅ | M1 |
| 공통 코드 조회 (common) | ✅ | - | ✅ | ✅ | ✅ | ✅ | ✅ | ✅ | M1 |
| 코드관리 (COD) | ✅ | ✅ | ✅ | ✅ | ✅ | ✅ | ✅ | ✅ | M2 |
| 메뉴관리 (MNU) | ✅ | ✅ | ✅ | ✅ | ✅ | ✅ | ✅ | ✅ | M2 |
| 역할관리 (ROL) | ✅ | ✅ | ✅ | ✅ | ✅ | ✅ | ✅ | ✅ | M3 |
| 권한관리 (PRM) | ✅ | ✅ | ✅ | ✅ | ✅ | ✅ | ✅ | ✅ | M3 |
| 관리자관리 (ADM) | ✅ | ✅ | ✅ | ✅ | ✅ | ✅ | ✅ | ✅ | M3 |
| 기업정보관리 (COM) | ✅ | ✅ | ✅ | ✅ | ✅ | ✅ | ✅ | ✅ | M4 |
| 사용자관리 (USR) | ✅ | ✅ | ✅ | ⬜ | ⬜ | ⬜ | ⬜ | ⬜ | M4 |
| 마스킹 설정 (MSK) | ✅ | ✅ | ✅ | ⬜ | ⬜ | ⬜ | ⬜ | ⬜ | M4 |
| 게시판 관리 (BRD) | ✅ | ✅ | ✅ | ⬜ | ⬜ | ⬜ | ⬜ | ⬜ | M5 |
| 게시글 관리 (PST) | ✅ | ✅ | ✅ | ⬜ | ⬜ | ⬜ | ⬜ | ⬜ | M5 |
| 감사로그·로그인 이력 (LOG) | ✅ | ✅ | ✅ | ⬜ | ⬜ | ⬜ | ⬜ | ⬜ | M6 |
| 배치 (BAT) | ✅ | - | - | ⬜ | - | - | - | ⬜ | M6 |

> 목록 공통(CMP-02 정렬 머리글, CMP-03 페이징)은 관리자관리에서 처음 만들었다: 서버 `PageQuery`·`PageResult`, ① `Pager.tsx`, ② `admin-pager.js`, ③ `pager.tag`·`sortTh.tag`. 이후 목록 화면은 이것을 쓴다.

> 기업정보관리에서 처음 만든 공통: 엑셀(`ExcelWriter`, `ExcelResponse`, ①② 파일 받기 `download`), 마스킹(`Masking`, `MaskingService`), 기간 선택(CMP-05), 사유 입력창(CMP-07), 우편번호 검색(CMP-16, ②③ `admin-form-kit.js`, ① `DateRange.tsx`·`lib/postcode.ts`). 회원 상세·목록 링크(`/users`)는 사용자관리(USR)에서 연결된다.

> 공통 코드 조회는 M1에서 서버·계약 테스트만 만들었다. 코드 콤보로 쓰는 화면(코드관리 등)을 만들 때 프론트에서 호출한다.

## 2. 기능 ID ↔ 화면 ↔ API 매핑

모듈 문서로 상세를 잇는다. 구현 시 각 기능 명세·화면·API 문서를 함께 본다.

| 모듈 | 기능 명세 | 화면 명세 | API 명세 |
|---|---|---|---|
| AUTH | [09-auth](04-features/09-auth.md) | [00-common](05-screens/00-common.md) | [00-auth](06-api/00-auth.md) |
| COD | [04-code](04-features/04-code.md) | [04-code](05-screens/04-code.md) | [04-code](06-api/04-code.md) |
| MNU | [03-menu](04-features/03-menu.md) | [03-menu](05-screens/03-menu.md) | [03-menu](06-api/03-menu.md) |
| ROL | [08-role](04-features/08-role.md) | [08-role](05-screens/08-role.md) | [08-role](06-api/08-role.md) |
| PRM | [07-permission](04-features/07-permission.md) | [07-permission](05-screens/07-permission.md) | [07-permission](06-api/07-permission.md) |
| ADM | [06-admin](04-features/06-admin.md) | [06-admin](05-screens/06-admin.md) | [06-admin](06-api/06-admin.md) |
| COM | [01-company](04-features/01-company.md) | [01-company](05-screens/01-company.md) | [01-company](06-api/01-company.md) |
| USR | [02-user](04-features/02-user.md) | [02-user](05-screens/02-user.md) | [02-user](06-api/02-user.md) |
| MSK | [10-masking](04-features/10-masking.md) | [10-masking](05-screens/10-masking.md) | [10-masking](06-api/10-masking.md) |
| BRD·PST | [05-board](04-features/05-board.md) | [05-board](05-screens/05-board.md) | [05-board](06-api/05-board.md) |
| LOG | [11-log](04-features/11-log.md) | [11-log](05-screens/11-log.md) | [11-log](06-api/11-log.md) |

## 3. 테스트 유형별 위치

| 유형 | 위치 | 확인 내용 |
|---|---|---|
| 계약 테스트 | `admin-web/.../api/*ContractTest`, `OpenApiContract.assertValid` | 응답이 `api/openapi.yaml`과 같은지 |
| 오류 코드 | `ErrorCodeSpecTest` | Java `ErrorCode` = openapi `ErrorCode` enum |
| 권한 | `PermissionTest` | 역할별 최종 권한 = `docs/data/permission-matrix.csv` |
| 초기·테스트 데이터 | `SeedDataTest`, `TestDataTest` | 초기 데이터가 명세와 같은지 |
| 세 프론트 공통 E2E | `e2e/tests/*.spec.ts` | 같은 시나리오를 `/react`, `/jsp`, `/ssr`로 3회 |
| React 단위 | `admin-react/src/**/*.test.tsx` | 화면 로직 |
