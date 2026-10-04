# 06-03. 메뉴관리 API

기능 명세: [04-features/03-menu.md](../04-features/03-menu.md) · 화면: [05-screens/03-menu.md](../05-screens/03-menu.md) · 메뉴 코드: `MENU`

## API 목록

| ID | 메서드 | URL | 권한 | 기능 |
|---|---|---|---|---|
| API-MNU-01 | GET | `/menus/tree` | `READ` | MNU-01 전체 메뉴 트리 |
| API-MNU-02 | GET | `/menus/{menuId}` | `READ` | MNU-02 상세 |
| API-MNU-03 | GET | `/menus/check-menu-cd?menuCd=` | `CREATE` | 메뉴 코드 중복 확인 |
| API-MNU-04 | POST | `/menus` | `CREATE` | MNU-03 등록 |
| API-MNU-05 | PUT | `/menus/{menuId}` | `UPDATE` | MNU-04 수정 |
| API-MNU-06 | PATCH | `/menus/{menuId}/parent` | `UPDATE` | MNU-05 상위 메뉴 변경 |
| API-MNU-07 | PUT | `/menus/order` | `UPDATE` | MNU-06 순서 저장 |
| API-MNU-08 | DELETE | `/menus/{menuId}` | `DELETE` | MNU-07 삭제 |

모든 API의 화면은 SCR-MNU-01이다.

## 트리 (API-MNU-01)

**항목** (노드마다): `menuId`, `parentMenuId`, `menuCd`, `menuNm`, `menuTypeCd`, `menuUrl`, `depth`, `sortOrd`, `useYn`, `systemYn`, `boardAutoYn`(게시판 자동 메뉴 여부), `children`

## 상세 (API-MNU-02)

**항목**: 트리 노드 항목 + `icon`, `actions`(사용 액션 목록), `modDt`

## 등록·수정 (API-MNU-04, 05)

```json
{
  "parentMenuId": 1,
  "menuCd": "COMPANY",
  "menuNm": "기업정보관리",
  "menuTypeCd": "PAGE",
  "menuUrl": "/companies",
  "actions": ["READ", "CREATE", "UPDATE", "DELETE", "EXCEL"],
  "icon": null,
  "useYn": "Y",
  "modDt": null
}
```

- 등록: 새 메뉴는 같은 상위 메뉴의 맨 뒤 순서가 된다. 응답 201 `{ "menuId": 2 }`
- 수정: `parentMenuId`, `menuCd`, `menuTypeCd`는 무시한다.
- 수정에서 액션을 빼면 회수되는 역할이 있는지 먼저 확인해야 한다. `?dryRun=Y`로 요청하면 저장하지 않고 `{ "revokedRoles": [ { "roleId": 3, "roleNm": "회원운영자", "actions": ["DELETE"] } ] }`만 돌려준다. 프론트는 이 결과로 확인창을 띄운 뒤 `dryRun` 없이 다시 보낸다.

## 상위 메뉴 변경 (API-MNU-06)

```json
{ "parentMenuId": 5, "modDt": "2026-10-04T14:30:15" }
```

- 옮긴 메뉴는 새 상위 메뉴의 맨 뒤 순서가 된다.

## 순서 저장 (API-MNU-07)

[순서 저장]을 누를 때 바뀐 상위 메뉴별로 하위 메뉴 순서를 한 번에 보낸다.

```json
{
  "orders": [
    { "parentMenuId": null, "menuIds": [1, 9, 4, 13] },
    { "parentMenuId": 4,    "menuIds": [6, 5, 7, 8] }
  ]
}
```

- `parentMenuId`가 `null`이면 최상위 메뉴다.
- `menuIds`에는 그 상위 메뉴의 하위 메뉴가 **빠짐없이** 있어야 한다. 빠지거나 다른 상위 메뉴의 메뉴가 섞이면 409 `MENU_ORDER_MISMATCH` (그사이 다른 관리자가 메뉴를 추가·이동한 경우). 프론트는 트리를 다시 불러오도록 안내한다.
- 전부 한 트랜잭션으로 저장한다.

## 업무 오류 코드

| 코드 | 상황 |
|---|---|
| `DUPLICATE` | 메뉴 코드 중복 |
| `MENU_DEPTH_EXCEEDED` | 3단계 초과 |
| `MENU_PARENT_NOT_FOLDER` | 화면 메뉴 아래에 추가·이동 |
| `MENU_HAS_CHILDREN` | 하위 메뉴가 있어 삭제 불가 |
| `MENU_SYSTEM_PROTECTED` | 시스템 메뉴 삭제·사용 안 함·이동 시도 |
| `MENU_BOARD_MANAGED` | 게시판 자동 메뉴를 메뉴관리에서 삭제·이동·코드 변경 시도 |
| `MENU_ORDER_MISMATCH` | 순서 저장 요청이 현재 트리와 맞지 않음 |
