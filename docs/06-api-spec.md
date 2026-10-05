# 06. API 명세

① React와 ② JSP + API가 호출하는 REST API의 공통 규약을 정의한다.
모듈별 API 목록과 요청·응답 항목은 [06-api/](06-api/) 아래 문서에 있다.

> **명세 우선 방식** ([ADR-0020](adr/0020-openapi-spec-first.md)): 개발 단계에서는 `api/openapi.yaml`이 API 요청·응답 형식의 **정본**이다. 이 Markdown 문서들은 규약, 업무 설명, 화면·권한 매핑을 담당한다. API를 바꿀 때는 `openapi.yaml`을 먼저 고치고, 규약·매핑이 바뀌면 이 문서도 함께 고친다. 모듈별 문서의 요청·응답 예시는 설계 당시의 기준이며, 필드가 다르면 `openapi.yaml`을 따른다.

> ③ JSP SSR은 이 API를 쓰지 않는다. 컨트롤러가 Service를 직접 호출한다 ([00-roadmap.md](00-roadmap.md) 2절).
> 다만 ③의 컨트롤러도 같은 Service와 같은 검증·오류 코드를 쓰므로, 4~6절의 오류 코드는 ③에서도 화면 메시지로 그대로 쓴다.

## 1. 기본 규칙

| 항목 | 규칙 |
|---|---|
| 기본 URL | `/api/v1` |
| 데이터 형식 | 요청·응답 모두 JSON (`Content-Type: application/json; charset=UTF-8`). 파일 업로드만 `multipart/form-data` |
| 필드 이름 | camelCase. DB 칼럼 이름을 camelCase로 바꾼 것을 기본으로 한다 (`COMPANY_NM` → `companyNm`, `USE_YN` → `useYn`) |
| 여부 값 | 문자열 `"Y"` / `"N"` (DB와 같게) |
| 일시 | ISO 8601 문자열, 한국 시간 기준, **초 단위, 시간대 표기 없음** `"2026-10-04T14:30:15"` (`openapi.yaml`의 `DateTime`) |
| 날짜 | `"2026-10-04"` |
| 코드값 | 코드만 보낸다 (`"statusCd": "ACTIVE"`). 응답에는 코드명을 함께 준다 (`"statusNm": "정상"`) |
| 빈 값 | `null`. 빈 문자열은 서버에서 `null`로 바꾼다 |
| 문자 인코딩 | UTF-8 |

## 2. URL과 메서드

| 작업 | 메서드 | URL 예 | 성공 상태 |
|---|---|---|---|
| 목록 조회 | `GET` | `/api/v1/companies?page=1&size=20` | 200 |
| 상세 조회 | `GET` | `/api/v1/companies/12` | 200 |
| 등록 | `POST` | `/api/v1/companies` | 201 |
| 수정 (전체) | `PUT` | `/api/v1/companies/12` | 200 |
| 부분 변경 (상태 등) | `PATCH` | `/api/v1/companies/12/status` | 200 |
| 삭제 | `DELETE` | `/api/v1/companies/12` | 200 |
| 동작 (초기화, 복사 등) | `POST` | `/api/v1/admins/5/password-reset` | 200 |
| 엑셀 다운로드 | `GET` | `/api/v1/companies/excel?status=ACTIVE` | 200 (파일) |

- 자원 이름은 복수형 kebab-case (`/code-groups`, `/audit-logs`).
- 사유가 필요한 삭제는 요청 본문에 사유를 담는다 (`DELETE` + JSON 본문).

## 3. 인증

토큰 방식만 쓴다 ([04-features/09-auth.md](04-features/09-auth.md) 1.1).

| 항목 | 규칙 |
|---|---|
| Access Token | 요청 헤더 `Authorization: Bearer {accessToken}` |
| Refresh Token | HttpOnly 쿠키 `refreshToken`. 경로 `/api/v1/auth/token`에만 전송 |
| 인증이 필요 없는 API | 로그인(`POST /auth/token`), 토큰 재발급(`POST /auth/token/refresh`) |
| 토큰 없음·잘못됨 | 401 `UNAUTHORIZED` |
| Access Token 만료 | 401 `TOKEN_EXPIRED` → 프론트는 재발급 후 원래 요청을 한 번 다시 보낸다 |
| 재발급 실패 | 401 `REFRESH_FAILED` → 로그인 화면으로 |
| 임시 비밀번호 상태 | 비밀번호 변경·로그아웃·내 정보 조회 외의 API는 403 `PASSWORD_CHANGE_REQUIRED` |

## 4. 응답 형식

모든 응답(파일 다운로드 제외)은 같은 틀을 쓴다.

**성공**

```json
{
  "success": true,
  "data": { "companyId": 12, "companyNm": "(주)테스트상사" },
  "error": null
}
```

**목록 성공**

```json
{
  "success": true,
  "data": {
    "items": [ { "companyId": 12, "companyNm": "(주)테스트상사" } ],
    "page": 1,
    "size": 20,
    "totalCount": 125,
    "totalPages": 7
  },
  "error": null
}
```

**실패**

```json
{
  "success": false,
  "data": null,
  "error": {
    "code": "VALIDATION_ERROR",
    "message": "입력값을 확인하세요.",
    "fieldErrors": [
      { "field": "bizRegNo", "message": "사업자등록번호는 숫자 10자리입니다." }
    ]
  }
}
```

- `fieldErrors`는 입력 오류일 때만 채운다. `field`는 요청 필드 이름과 같다.
- `message`는 화면에 그대로 보여 줄 수 있는 문장이다.

## 5. 목록·페이징·정렬

| 파라미터 | 설명 | 기본값 |
|---|---|---|
| `page` | 페이지 번호 (1부터) | 1 |
| `size` | 페이지 크기: 10, 20, 50 | 20 |
| `sort` | `필드,방향`. 여러 개면 반복 (`sort=regDt,desc&sort=companyNm,asc`) | 기능별 기본 정렬 |
| 검색 조건 | 필드 이름 그대로 (`companyNm=테스트&statusCd=ACTIVE`) | |
| 기간 | `{필드}From`, `{필드}To` (`regDtFrom=2026-10-01&regDtTo=2026-10-04`) | |

- 정렬할 수 있는 필드는 화면 명세의 "정렬 ○" 칼럼만이다. 다른 필드를 주면 400.
- 페이징이 없는 목록(코드, 역할 등)은 `data`에 배열을 바로 담는다.

## 6. HTTP 상태와 오류 코드

| 상태 | 오류 코드 | 상황 |
|---|---|---|
| 400 | `VALIDATION_ERROR` | 입력값 검증 실패 (`fieldErrors` 포함) |
| 400 | `INVALID_REQUEST` | 형식이 잘못된 요청 (JSON 오류, 알 수 없는 정렬 필드 등) |
| 401 | `UNAUTHORIZED`, `TOKEN_EXPIRED`, `REFRESH_FAILED` | 인증 (3절) |
| 401 | `LOGIN_FAILED` | 아이디 또는 비밀번호 오류 |
| 403 | `FORBIDDEN` | 권한 없음 |
| 403 | `PASSWORD_CHANGE_REQUIRED` | 임시 비밀번호 상태 |
| 403 | `ACCOUNT_LOCKED`, `ACCOUNT_DISABLED` | 잠금·사용중지 계정 로그인 |
| 404 | `NOT_FOUND` | 대상이 없거나 삭제됨 |
| 409 | `DUPLICATE` | 중복 값 (로그인 아이디, 사업자등록번호, 코드 등) |
| 409 | `CONFLICT_MODIFIED` | 다른 관리자가 먼저 수정함 (7절) |
| 409 | 업무 오류 코드 | 업무 규칙 위반. 예: `COMPANY_HAS_MEMBERS`, `LAST_SUPER_ADMIN`, `SELF_ROLE_CHANGE`, `ROLE_IN_USE` (모듈 문서에 정리) |
| 413 | `FILE_TOO_LARGE` | 첨부 크기 초과 |
| 500 | `INTERNAL_ERROR` | 서버 오류. 메시지는 일반 문구만 ("일시적인 오류가 발생했습니다") |

## 7. 동시 수정 방지

- 상세 조회 응답에 `modDt`를 준다.
- 수정(`PUT`, `PATCH`) 요청에 조회했던 `modDt`를 그대로 보낸다.
- 서버의 `MOD_DT`와 **초 단위까지** 비교해 다르면 저장하지 않고 409 `CONFLICT_MODIFIED`를 준다. 프론트는 "다른 관리자가 먼저 수정했습니다. 다시 조회하세요"를 보여 준다.

## 8. 파일

| 작업 | 방식 |
|---|---|
| 업로드 | 게시글 등록·수정 요청을 `multipart/form-data`로 보낸다. `data` 파트(JSON) + `files` 파트(여러 개) |
| 다운로드 | `GET /api/v1/attachments/{fileId}` → 파일. `Content-Disposition`에 원본 파일명(UTF-8 인코딩) |
| 엑셀 | `GET .../excel?{검색 조건}` → `.xlsx` 파일. 파일명: `{메뉴명}_{yyyyMMddHHmm}.xlsx` |

- 파일 응답이 실패하면 파일 대신 4절의 실패 JSON을 준다. 프론트는 응답 `Content-Type`으로 구분한다.
- 엑셀 건수가 10,000건을 넘으면 409 `EXCEL_LIMIT_EXCEEDED`.

## 9. 공통 API

여러 화면에서 쓰는 조회용 API다. 로그인만 되어 있으면 쓸 수 있는 것과, 특정 권한이 필요한 것이 있다.

| 메서드 | URL | 권한 | 설명 |
|---|---|---|---|
| GET | `/common/codes/{groupCd}` | 로그인 | 상세코드 목록 (코드 콤보용). `includeUnused=Y`면 사용 안 함 코드 포함 (표시용) |
| GET | `/common/codes?groups=USER_STATUS,USER_TYPE` | 로그인 | 여러 그룹을 한 번에 |
| GET | `/common/companies?keyword=` | `USER` `CREATE` 또는 `UPDATE` | 정상 기업 선택창용 (최대 20건) |
| GET | `/common/admins?keyword=` | `PERMISSION` `READ` | 관리자 선택창용 (최대 20건) |
| GET | `/common/roles` | `ADMIN` `READ` | 사용 중인 역할 목록 (역할 선택용) |
| GET | `/common/menus` | 로그인 | 화면 메뉴 목록 (감사로그 검색 조건용) |

## 10. 모듈별 API 문서

| 문서 | 내용 |
|---|---|
| [06-api/00-auth.md](06-api/00-auth.md) | 로그인, 토큰, 내 정보 |
| [06-api/01-company.md](06-api/01-company.md) | 기업정보관리 |
| [06-api/02-user.md](06-api/02-user.md) | 사용자관리 |
| [06-api/03-menu.md](06-api/03-menu.md) | 메뉴관리 |
| [06-api/04-code.md](06-api/04-code.md) | 코드관리 |
| [06-api/05-board.md](06-api/05-board.md) | 게시판, 게시글, 댓글, 첨부 |
| [06-api/06-admin.md](06-api/06-admin.md) | 관리자 |
| [06-api/07-permission.md](06-api/07-permission.md) | 권한 |
| [06-api/08-role.md](06-api/08-role.md) | 역할 |
| [06-api/10-masking.md](06-api/10-masking.md) | 마스킹 설정 |
| [06-api/11-log.md](06-api/11-log.md) | 감사로그, 로그인 이력 |

모듈 문서의 표 읽는 법:

- **URL**은 `/api/v1`을 뺀 경로다.
- **권한**은 `메뉴 코드` + `액션`. 로그인만 필요하면 "로그인".
- **화면**은 이 API를 쓰는 화면 ID ([05-ia-screens.md](05-ia-screens.md) 5절).
- 요청·응답 항목의 검증 규칙은 화면 명세의 "입력 항목" 표를 따르고, 여기서는 반복하지 않는다.

## 11. 미결 사항

없음. (API 명세는 `api/openapi.yaml`을 먼저 쓰는 명세 우선 방식이다. [ADR-0020](adr/0020-openapi-spec-first.md))
