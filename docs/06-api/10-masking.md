# 06-10. 마스킹 설정 API

기능 명세: [04-features/10-masking.md](../04-features/10-masking.md) · 화면: [05-screens/10-masking.md](../05-screens/10-masking.md) · 메뉴 코드: `MASKING`

## API 목록

| ID | 메서드 | URL | 권한 | 화면 | 기능 |
|---|---|---|---|---|---|
| API-MSK-01 | GET | `/masking-policies` | `READ` | SCR-MSK-01 | MSK-01 설정 조회 |
| API-MSK-02 | PUT | `/masking-policies` | `UPDATE` | SCR-MSK-01 | MSK-02 설정 변경 |

## 조회 (API-MSK-01)

배열로 준다.

```json
[
  { "fieldCd": "USER_NM", "fieldNm": "이름", "example": "홍*동", "screenMaskYn": "Y", "excelMaskYn": "Y", "modNm": "최초관리자", "modDt": "..." },
  { "fieldCd": "MOBILE_NO", "fieldNm": "휴대폰 번호", "example": "010-****-5678", "screenMaskYn": "Y", "excelMaskYn": "Y", "modNm": "최초관리자", "modDt": "..." }
]
```

## 변경 (API-MSK-02)

바뀐 항목만 보낸다.

```json
{
  "policies": [
    { "fieldCd": "EMAIL", "screenMaskYn": "N", "excelMaskYn": "Y", "modDt": "..." }
  ]
}
```

- 항목마다 `modDt`를 비교한다. 하나라도 다르면 전체를 저장하지 않고 409 `CONFLICT_MODIFIED`.
