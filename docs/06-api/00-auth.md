# 06-00. 인증·내 정보 API

기능 명세: [04-features/09-auth.md](../04-features/09-auth.md) · 화면: [05-screens/00-common.md](../05-screens/00-common.md)

## API 목록

| ID | 메서드 | URL | 권한 | 화면 | 설명 |
|---|---|---|---|---|---|
| API-AUTH-01 | POST | `/auth/token` | 없음 | SCR-AUTH-01 | 로그인. Access Token 응답 + Refresh Token 쿠키 |
| API-AUTH-02 | POST | `/auth/token/refresh` | Refresh Token 쿠키 | 모든 화면 | 토큰 재발급 |
| API-AUTH-03 | DELETE | `/auth/token` | 로그인 | 헤더 | 로그아웃. Refresh Token 폐기, 쿠키 삭제 (쿠키 경로 안에 두어 Refresh Token이 함께 오게 함) |
| API-AUTH-04 | GET | `/auth/me` | 로그인 | 모든 화면 | 내 정보 + 메뉴 트리 + 권한 목록 |
| API-AUTH-05 | PUT | `/me` | 로그인 | SCR-MY-01 | 내 정보 수정 |
| API-AUTH-06 | PUT | `/me/password` | 로그인 | SCR-AUTH-02 | 비밀번호 변경 |

## API-AUTH-01 로그인

**요청**

```json
{ "loginId": "t_member", "password": "********" }
```

**응답** (200) + `Set-Cookie: refreshToken=...; HttpOnly; Secure; Path=/api/v1/auth/token`

```json
{
  "success": true,
  "data": {
    "accessToken": "eyJhbGciOi...",
    "expiresIn": 1800,
    "pwdChangeRequired": false
  },
  "error": null
}
```

- `pwdChangeRequired`가 `true`면 프론트는 비밀번호 변경 화면으로 보낸다.

**오류**: 401 `LOGIN_FAILED`, 403 `ACCOUNT_LOCKED`, 403 `ACCOUNT_DISABLED`

## API-AUTH-02 토큰 재발급

- 요청 본문 없음. 쿠키의 Refresh Token을 쓴다.
- 응답은 API-AUTH-01과 같다. 새 Refresh Token 쿠키도 내려간다.
- 오류: 401 `REFRESH_FAILED` (만료, 폐기, 이미 사용한 토큰)

## API-AUTH-04 내 정보·메뉴·권한

**응답**

```json
{
  "success": true,
  "data": {
    "adminId": 3,
    "loginId": "t_member",
    "adminNm": "테스트회원운영",
    "roles": [ { "roleCd": "MEMBER_OPERATOR", "roleNm": "회원운영자" } ],
    "superAdmin": false,
    "lastLoginDt": "2026-10-03T18:20:00",
    "lastLoginIp": "10.0.0.5",
    "pwdChangedDt": "2026-09-01T09:00:00",
    "pwdChangeRequired": false,
    "menus": [
      {
        "menuId": 1, "menuCd": "MEMBER_ROOT", "menuNm": "회원관리", "menuTypeCd": "FOLDER", "icon": "users",
        "children": [
          { "menuId": 2, "menuCd": "COMPANY", "menuNm": "기업정보관리", "menuTypeCd": "PAGE", "menuUrl": "/companies", "children": [] }
        ]
      }
    ],
    "permissions": {
      "COMPANY": ["READ", "CREATE", "UPDATE", "EXCEL"],
      "USER": ["READ", "CREATE", "UPDATE", "EXCEL", "PRIVACY"]
    }
  },
  "error": null
}
```

- `menus`는 노출 규칙을 적용한 내 메뉴 트리다 ([02-access-model.md](../02-access-model.md) 3.2).
- `permissions`는 메뉴 코드별 액션 목록이다. 버튼 활성·비활성에 쓴다. 슈퍼관리자는 `superAdmin: true`이고 `permissions`는 비운다 (프론트는 모든 버튼을 활성).

## API-AUTH-05 내 정보 수정

**요청**: `adminNm`, `email`, `mobileNo`, `deptNm`, `modDt`

## API-AUTH-06 비밀번호 변경

**요청**

```json
{ "currentPassword": "********", "newPassword": "********" }
```

- 새 비밀번호 확인 값은 프론트에서만 비교하고 보내지 않는다.
- 오류: 400 `VALIDATION_ERROR` (현재 비밀번호 불일치는 `currentPassword` 필드 오류, 규칙 위반은 `newPassword` 필드 오류)
- 성공하면 다른 곳의 로그인(Refresh Token)을 모두 폐기한다. 현재 로그인은 유지한다.
