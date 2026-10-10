# 06-02. 사용자관리 API

기능 명세: [04-features/02-user.md](../04-features/02-user.md) · 화면: [05-screens/02-user.md](../05-screens/02-user.md) · 메뉴 코드: `USER`

## API 목록

| ID | 메서드 | URL | 권한 | 화면 | 기능 |
|---|---|---|---|---|---|
| API-USR-01 | GET | `/users` | `READ` | SCR-USR-01 | USR-01 목록 |
| API-USR-02 | GET | `/users/excel` | `EXCEL` | SCR-USR-01 | USR-11 엑셀 |
| API-USR-03 | GET | `/users/{userId}` | `READ` | SCR-USR-02 | USR-02 상세 (마스킹 적용) |
| API-USR-04 | POST | `/users/{userId}/privacy` | `PRIVACY` | SCR-USR-02 | USR-03 개인정보 원문 보기 |
| API-USR-05 | GET | `/users/{userId}/form` | `UPDATE` + `PRIVACY` | SCR-USR-03 | 수정 화면용 원문 조회 |
| API-USR-06 | GET | `/users/check-login-id?loginId=` | `CREATE` | SCR-USR-03 | 아이디 중복 확인 |
| API-USR-07 | POST | `/users` | `CREATE` + `PRIVACY` | SCR-USR-03 | USR-04 등록 |
| API-USR-08 | PUT | `/users/{userId}` | `UPDATE` + `PRIVACY` | SCR-USR-03 | USR-05, 06 정보·소속 기업 수정 |
| API-USR-09 | PATCH | `/users/{userId}/status` | `UPDATE` | SCR-USR-02 | USR-07 상태 변경 |
| API-USR-10 | POST | `/users/{userId}/password-reset` | `UPDATE` | SCR-USR-02 | USR-08 비밀번호 초기화 |
| API-USR-11 | DELETE | `/users/{userId}` | `DELETE` | SCR-USR-02 | USR-09 삭제 |
| API-USR-12 | GET | `/users/{userId}/status-histories` | `READ` | SCR-USR-02 | USR-10 상태 변경 이력 |
| API-USR-13 | GET | `/users/company-options?keyword=` | `READ` | SCR-USR-03 | 소속 기업 선택 목록 (정상 기업만, 최대 100건) |

## 목록 (API-USR-01)

**검색 파라미터**: `userTypeCd`, `loginId`, `userNm`, `email`, `mobileNo`, `companyId`(기업 상세의 [전체 보기]), `companyNm`, `statusCd`, `joinPath`(`USER_SERVICE` / `ADMIN`), `joinDtFrom`, `joinDtTo`, `page`, `size`, `sort`
**정렬 가능**: `loginId`, `userNm`, `joinDt` (기본 `joinDt,desc`)

**항목**: `userId`, `userTypeCd`, `userTypeNm`, `loginId`, `userNm`, `email`, `mobileNo`, `companyId`, `companyNm`, `statusCd`, `statusNm`, `joinDt`

- 개인정보 항목은 화면 마스킹 설정에 따라 가린 값으로 준다.

## 상세 (API-USR-03)

**항목**: 목록 항목 + `birthDate`, `deptNm`, `positionNm`, `joinPath`, `pwdTempYn`, `lastLoginDt`, `withdrawDt`, `maskedFields`(가려진 항목 이름 목록), `regNm`, `regDt`, `modNm`, `modDt`

- `maskedFields`가 비어 있지 않고 `PRIVACY` 권한이 있으면 프론트는 [원문 보기] 버튼을 활성화한다.

## 개인정보 원문 보기 (API-USR-04)

**요청**

```json
{ "reasonCd": "CS_INQUIRY", "reasonEtc": null }
```

- `reasonCd`가 `ETC`면 `reasonEtc` 필수.

**응답**: `userNm`, `email`, `mobileNo`, `birthDate` 원문

- 호출할 때마다 감사로그를 남긴다. 응답은 캐시하지 않도록 `Cache-Control: no-store`.

## 수정 화면용 조회 (API-USR-05)

- 상세 항목을 원문으로 준다. 감사로그(개인정보 원문 보기, 사유 `DATA_CORRECTION`)를 남긴다.
- 탈퇴 회원이면 409 `USER_WITHDRAWN`.

## 등록·수정 (API-USR-07, 08)

**요청**

```json
{
  "userTypeCd": "CORPORATE",
  "loginId": "c_user11",
  "userNm": "테스트11",
  "email": "c_user11@example.com",
  "mobileNo": "01000000011",
  "birthDate": "1990-05-01",
  "companyId": 12,
  "deptNm": "영업팀",
  "positionNm": "대리",
  "modDt": null
}
```

- 등록 응답 201: `{ "userId": 120, "tempPassword": "Xy7#..." }`. 임시 비밀번호는 이 응답에서만 준다.
- 수정: `userTypeCd`, `loginId`는 무시한다. `modDt` 필수.

## 상태 변경 (API-USR-09)

```json
{ "statusCd": "SUSPENDED", "reason": "약관 위반 신고 확인", "modDt": "2026-10-04T14:30:15" }
```

- 바꿀 수 있는 상태 조합은 [04-features/02-user.md](../04-features/02-user.md) 5절 상태 전이를 따른다.

## 비밀번호 초기화 (API-USR-10)

- 본문 없음. 응답: `{ "tempPassword": "Xy7#..." }`

## 삭제 (API-USR-11)

```json
{ "reason": "중복 등록 정리" }
```

## 업무 오류 코드

| 코드 | 상황 |
|---|---|
| `DUPLICATE` | 로그인 아이디 중복 |
| `USER_WITHDRAWN` | 탈퇴 회원 수정·상태 변경 시도 |
| `INVALID_STATUS_CHANGE` | 허용되지 않은 상태 전이 |
| `COMPANY_NOT_ACTIVE` | 정상이 아닌 기업을 소속 기업으로 지정 |
| `COMPANY_REQUIRED` | 기업 회원인데 소속 기업 없음 |
