# 06-04. 코드관리 API

기능 명세: [04-features/04-code.md](../04-features/04-code.md) · 화면: [05-screens/04-code.md](../05-screens/04-code.md) · 메뉴 코드: `CODE`

코드 콤보용 조회는 공통 API(`/common/codes/{groupCd}`, [06-api-spec.md](../06-api-spec.md) 9절)를 쓴다. 아래는 코드관리 화면용 API다.

## API 목록

| ID | 메서드 | URL | 권한 | 기능 |
|---|---|---|---|---|
| API-COD-01 | GET | `/code-groups` | `READ` | COD-01 그룹코드 목록 |
| API-COD-02 | GET | `/code-groups/{groupCd}` | `READ` | 그룹코드 상세 (수정 팝업) |
| API-COD-03 | POST | `/code-groups` | `CREATE` | COD-02 그룹코드 등록 |
| API-COD-04 | PUT | `/code-groups/{groupCd}` | `UPDATE` | COD-03 그룹코드 수정 |
| API-COD-05 | DELETE | `/code-groups/{groupCd}` | `DELETE` | COD-04 그룹코드 삭제 |
| API-COD-06 | GET | `/code-groups/{groupCd}/codes` | `READ` | COD-05 상세코드 목록 |
| API-COD-07 | POST | `/code-groups/{groupCd}/codes` | `CREATE` | COD-06 상세코드 등록 |
| API-COD-08 | PUT | `/code-groups/{groupCd}/codes/{code}` | `UPDATE` | COD-07 상세코드 수정 |
| API-COD-09 | DELETE | `/code-groups/{groupCd}/codes/{code}` | `DELETE` | COD-08 상세코드 삭제 |

모든 API의 화면은 SCR-COD-01이다. 목록은 페이징 없이 배열로 준다.

## 그룹코드

**목록 파라미터**: `keyword`(그룹코드·이름), `useYn`
**목록 항목**: `groupCd`, `groupNm`, `codeCnt`, `systemYn`, `useYn`

**등록·수정 요청**

```json
{ "groupCd": "FAQ_CATEGORY", "groupNm": "FAQ 분류", "description": null, "useYn": "Y", "modDt": null }
```

- 수정: `groupCd`는 무시한다. 시스템 코드면 `useYn`도 무시한다.

## 상세코드

**목록 항목**: `groupCd`, `code`, `codeNm`, `sortOrd`, `description`, `useYn`, `modDt`

**등록·수정 요청**

```json
{ "code": "ACCOUNT", "codeNm": "계정", "sortOrd": 1, "description": null, "useYn": "Y", "modDt": null }
```

## 업무 오류 코드

| 코드 | 상황 |
|---|---|
| `DUPLICATE` | 그룹코드 또는 상세코드 중복 |
| `CODE_SYSTEM_PROTECTED` | 시스템 코드 그룹 삭제, 상세코드 추가·삭제, 사용 여부 변경 시도 |
| `CODE_GROUP_HAS_CODES` | 상세코드가 있어 그룹 삭제 불가 |
