# 06-08. 역할 API

기능 명세: [04-features/08-role.md](../04-features/08-role.md) · 화면: [05-screens/08-role.md](../05-screens/08-role.md) · 메뉴 코드: `ROLE`

## API 목록

| ID | 메서드 | URL | 권한 | 화면 | 기능 |
|---|---|---|---|---|---|
| API-ROL-01 | GET | `/roles` | `READ` | SCR-ROL-01 | ROL-01 목록 (페이징 없음) |
| API-ROL-02 | GET | `/roles/{roleId}` | `READ` | SCR-ROL-02, 03 | ROL-02 상세 |
| API-ROL-03 | GET | `/roles/{roleId}/permissions` | `READ` | SCR-ROL-02 | 권한 탭: 메뉴 트리 × 액션 |
| API-ROL-04 | GET | `/roles/{roleId}/admins` | `READ` | SCR-ROL-02 | 관리자 탭 |
| API-ROL-05 | GET | `/roles/check-role-cd?roleCd=` | `CREATE` | SCR-ROL-03 | 역할 코드 중복 확인 |
| API-ROL-06 | POST | `/roles` | `CREATE` | SCR-ROL-03 | ROL-03 등록 |
| API-ROL-07 | POST | `/roles/{roleId}/copy` | `CREATE` | SCR-ROL-02 | ROL-04 복사 |
| API-ROL-08 | PUT | `/roles/{roleId}` | `UPDATE` | SCR-ROL-03 | ROL-05 정보 수정 |
| API-ROL-09 | PUT | `/roles/{roleId}/permissions` | `UPDATE` | SCR-ROL-02 | ROL-06 권한 설정 |
| API-ROL-10 | DELETE | `/roles/{roleId}` | `DELETE` | SCR-ROL-02 | ROL-07 삭제 |

## 목록·상세

**목록 파라미터**: `keyword`(역할 코드·이름), `useYn`
**목록 항목**: `roleId`, `roleCd`, `roleNm`, `description`, `adminCnt`, `systemYn`, `useYn`
**상세 항목**: 목록 항목 + `mine`(내가 가진 역할인지), `editable`(시스템 역할·내 역할이면 `false`. 슈퍼관리자는 내 역할도 `true`), `regNm`, `regDt`, `modNm`, `modDt`

## 권한 탭 (API-ROL-03)

메뉴 트리 노드마다:

```json
{
  "menuId": 3, "menuNm": "사용자관리", "menuTypeCd": "PAGE", "depth": 2,
  "actions": ["READ", "CREATE", "UPDATE", "DELETE", "EXCEL", "PRIVACY"],
  "granted": ["READ", "CREATE", "UPDATE", "EXCEL", "PRIVACY"],
  "grantableActions": ["READ", "CREATE", "UPDATE", "DELETE", "EXCEL", "PRIVACY"],
  "children": []
}
```

- `grantableActions`: 내가 가진 액션 (BR-05). 슈퍼관리자면 전체.

## 등록·수정 (API-ROL-06, 08)

```json
{ "roleCd": "NOTICE_OPERATOR", "roleNm": "공지운영자", "description": "공지사항만 관리", "useYn": "Y", "modDt": null }
```

- 수정: `roleCd`는 무시한다.
- 사용 안 함으로 바꿀 때 확인창의 관리자 수는 상세의 `adminCnt`를 쓴다 (별도 미리보기 API 없음).

## 복사 (API-ROL-07)

```json
{ "roleCd": "MEMBER_OPERATOR_2", "roleNm": "회원운영자2" }
```

- 응답 201: `{ "roleId": 12 }`. 원본의 권한을 그대로 가진다. 설명은 복사하고 사용 여부는 `Y`.
- 원본 권한 중 내가 갖지 않은 권한이 있으면 409 `PRIVILEGE_ESCALATION` (슈퍼관리자 제외).

## 권한 설정 (API-ROL-09)

권한이 있는 메뉴만, 메뉴별 최종 액션 목록을 보낸다. 목록에 없는 메뉴는 권한을 모두 회수한다.

```json
{
  "permissions": [
    { "menuId": 2, "actions": ["READ", "CREATE", "UPDATE", "EXCEL"] },
    { "menuId": 3, "actions": ["READ", "UPDATE", "PRIVACY"] }
  ],
  "modDt": "..."
}
```

- 서버도 `READ` 자동 부여 규칙을 다시 적용한다.
- 응답: 추가·제거된 권한 목록.

## 업무 오류 코드

| 코드 | 상황 |
|---|---|
| `DUPLICATE` | 역할 코드 중복 |
| `ROLE_NOT_EDITABLE` | 시스템 역할 또는 내 역할 수정·권한 설정 |
| `PRIVILEGE_ESCALATION` | 내가 갖지 않은 권한 설정·복사 |
| `ROLE_IN_USE` | 관리자에게 부여된 역할 삭제 (R6) |
| `ROLE_SYSTEM_PROTECTED` | 시스템 역할 삭제 |
