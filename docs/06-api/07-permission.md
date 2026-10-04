# 06-07. 권한 API

기능 명세: [04-features/07-permission.md](../04-features/07-permission.md) · 화면: [05-screens/07-permission.md](../05-screens/07-permission.md) · 메뉴 코드: `PERMISSION`

## API 목록

| ID | 메서드 | URL | 권한 | 화면 | 기능 |
|---|---|---|---|---|---|
| API-PRM-01 | GET | `/permissions` | `READ` | SCR-PRM-01 | PRM-01 메뉴 트리별 사용 액션 |
| API-PRM-02 | GET | `/permissions/menus/{menuId}` | `READ` | SCR-PRM-01 | PRM-02 메뉴의 역할 × 액션 표 |
| API-PRM-03 | PUT | `/permissions/menus/{menuId}` | `UPDATE` | SCR-PRM-01 | PRM-03 메뉴 기준 부여·회수 |
| API-PRM-04 | GET | `/permissions/admins/{adminId}` | `READ` | SCR-PRM-02 | PRM-04 관리자별 최종 권한 |

## 메뉴 트리 (API-PRM-01)

메뉴 트리 노드마다 `menuId`, `menuCd`, `menuNm`, `menuTypeCd`, `actions`(사용 액션), `children`.

## 메뉴의 역할 × 액션 (API-PRM-02)

```json
{
  "success": true,
  "data": {
    "menu": { "menuId": 3, "menuCd": "USER", "menuNm": "사용자관리", "actions": ["READ", "CREATE", "UPDATE", "DELETE", "EXCEL", "PRIVACY"] },
    "roles": [
      { "roleId": 1, "roleNm": "슈퍼관리자", "systemYn": "Y", "useYn": "Y", "editable": false, "granted": ["READ", "CREATE", "UPDATE", "DELETE", "EXCEL", "PRIVACY"] },
      { "roleId": 3, "roleNm": "회원운영자", "systemYn": "N", "useYn": "Y", "editable": true, "granted": ["READ", "CREATE", "UPDATE", "EXCEL", "PRIVACY"] }
    ],
    "grantableActions": ["READ", "CREATE", "UPDATE", "DELETE", "EXCEL", "PRIVACY"]
  },
  "error": null
}
```

- `editable`: 시스템 역할이거나 내 역할이면 `false` (BR-03, BR-04).
- `grantableActions`: 내가 이 메뉴에서 가진 액션. 이 밖의 칸은 체크할 수 없다 (BR-05). 슈퍼관리자면 전체.

## 메뉴 기준 부여·회수 (API-PRM-03)

바뀐 역할만 보낸다. 각 역할의 **최종 액션 목록**을 보낸다.

```json
{
  "roles": [
    { "roleId": 3, "actions": ["READ", "UPDATE"] },
    { "roleId": 5, "actions": [] }
  ]
}
```

- 서버도 `READ` 자동 부여·회수 규칙(BR-02)을 다시 적용한다.
- 응답: 역할별로 추가·제거된 액션 목록.

## 관리자별 최종 권한 (API-PRM-04)

```json
{
  "success": true,
  "data": {
    "admin": { "adminId": 7, "loginId": "t_multi", "adminNm": "테스트다중역할" },
    "superAdmin": false,
    "menus": [
      {
        "menuId": 2, "menuNm": "기업정보관리", "depth": 2, "actions": ["READ", "CREATE", "UPDATE", "DELETE", "EXCEL"],
        "granted": { "READ": ["회원운영자"], "UPDATE": ["회원운영자"] }
      }
    ]
  },
  "error": null
}
```

- `granted`: 액션별로 그 권한을 준 역할 이름 목록. 없는 액션은 키가 없다.

## 업무 오류 코드

| 코드 | 상황 |
|---|---|
| `ROLE_NOT_EDITABLE` | 시스템 역할 또는 내 역할 변경 |
| `PRIVILEGE_ESCALATION` | 내가 갖지 않은 권한 부여 |
| `MENU_NOT_PAGE` | 폴더 메뉴의 권한 조회·변경 |
