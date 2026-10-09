# 06-06. 관리자 API

기능 명세: [04-features/06-admin.md](../04-features/06-admin.md) · 화면: [05-screens/06-admin.md](../05-screens/06-admin.md) · 메뉴 코드: `ADMIN`

## API 목록

| ID | 메서드 | URL | 권한 | 화면 | 기능 |
|---|---|---|---|---|---|
| API-ADM-01 | GET | `/admins` | `READ` | SCR-ADM-01 | ADM-01 목록 |
| API-ADM-02 | GET | `/admins/{adminId}` | `READ` | SCR-ADM-02, 03 | ADM-02 상세 |
| API-ADM-03 | GET | `/admins/check-login-id?loginId=` | `CREATE` | SCR-ADM-03 | 아이디 중복 확인 |
| API-ADM-04 | POST | `/admins` | `CREATE` | SCR-ADM-03 | ADM-03 등록 |
| API-ADM-05 | PUT | `/admins/{adminId}` | `UPDATE` | SCR-ADM-03 | ADM-04 정보 수정 |
| API-ADM-06 | POST | `/admins/{adminId}/roles` | `UPDATE` | SCR-ADM-02 | ADM-05 역할 부여 |
| API-ADM-07 | DELETE | `/admins/{adminId}/roles/{roleId}` | `UPDATE` | SCR-ADM-02 | ADM-05 역할 회수 |
| API-ADM-08 | POST | `/admins/{adminId}/unlock` | `UPDATE` | SCR-ADM-02 | ADM-06 잠금 해제 |
| API-ADM-09 | POST | `/admins/{adminId}/password-reset` | `UPDATE` | SCR-ADM-02 | ADM-07 비밀번호 초기화 |
| API-ADM-10 | PATCH | `/admins/{adminId}/status` | `DELETE` | SCR-ADM-02 | ADM-08 사용중지 / 재사용 |
| API-ADM-11 | GET | `/admins/{adminId}/login-histories` | `READ` | SCR-ADM-02 | ADM-09 최근 로그인 이력 20건 |
| API-ADM-12 | GET | `/admins/role-options` | `READ` | SCR-ADM-01~03 | 역할 선택 목록 (목록 검색 상자, 역할 부여·등록) |

## 목록 (API-ADM-01)

**파라미터**: `loginId`, `adminNm`, `deptNm`, `roleId`, `statusCd`, `page`, `size`, `sort` (정렬: `loginId`, `adminNm`, `lastLoginDt`, `regDt`)
- `keyword`: 로그인 아이디·이름 중 하나라도 포함 (관리자 선택창 CMP-12용).
**항목**: `adminId`, `loginId`, `adminNm`, `deptNm`, `roles`(`roleId`, `roleNm` 배열), `statusCd`, `statusNm`, `lastLoginDt`, `regDt`

## 상세 (API-ADM-02)

**항목**: `adminId`, `loginId`, `adminNm`, `email`, `mobileNo`, `deptNm`, `statusCd`, `statusNm`, `loginFailCnt`, `pwdTempYn`, `pwdChangedDt`, `lastLoginDt`, `roles`(`roleId`, `roleNm`, `regNm`, `regDt` 배열), `self`(본인 여부), `regNm`, `regDt`, `modNm`, `modDt`

- `self`가 `true`면 프론트는 역할 추가·회수, 사용중지 버튼을 숨긴다.

## 등록 (API-ADM-04)

```json
{
  "loginId": "t_new",
  "adminNm": "신규관리자",
  "email": "t_new@example.com",
  "mobileNo": null,
  "deptNm": "운영팀",
  "roleIds": [3]
}
```

- 응답 201: `{ "adminId": 10, "tempPassword": "Xy7#..." }`

## 정보 수정 (API-ADM-05)

**요청**: `adminNm`, `email`, `mobileNo`, `deptNm`, `modDt`

## 역할 부여·회수 (API-ADM-06, 07)

- 부여 요청: `{ "roleId": 4 }`
- 회수: 본문 없음

## 비밀번호 초기화 (API-ADM-09)

- 응답: `{ "tempPassword": "Xy7#..." }`. 대상 관리자의 로그인(Refresh Token, 세션)을 모두 끊는다.

## 상태 변경 (API-ADM-10)

```json
{ "statusCd": "DISABLED", "modDt": "..." }
```

- `statusCd`는 `DISABLED`(사용중지) 또는 `ACTIVE`(재사용)만 받는다. 잠금 해제는 API-ADM-08.

## 역할 선택 목록 (API-ADM-12)

**항목**: `roleId`, `roleCd`, `roleNm`, `useYn`, `assignable`

- `assignable`: 사용 중이고, 역할의 권한이 모두 내 권한 안에 있는 역할 (R5). 슈퍼관리자 역할은 슈퍼관리자만 줄 수 있다. 슈퍼관리자면 사용 중인 역할 전체.
- 관리자관리 권한만 있고 역할관리 권한이 없는 관리자도 역할을 고를 수 있도록 역할 API(`/roles`) 대신 이 API를 쓴다.

## 업무 오류 코드

| 코드 | 상황 |
|---|---|
| `DUPLICATE` | 로그인 아이디 중복 |
| `ROLE_REQUIRED` | 역할 0개로 등록하거나 마지막 역할 회수 |
| `SELF_ROLE_CHANGE` | 본인 역할 변경 (R4) |
| `SELF_DISABLE` | 본인 사용중지 |
| `PRIVILEGE_ESCALATION` | 내가 갖지 않은 권한이 든 역할 부여 (R5) |
| `LAST_SUPER_ADMIN` | 마지막 슈퍼관리자 사용중지·역할 회수 (R3) |
| `ROLE_NOT_USABLE` | 사용 안 함 역할 부여 |
| `INVALID_STATUS_CHANGE` | 허용되지 않은 상태 변경 |
