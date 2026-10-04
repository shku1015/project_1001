# 06-11. 감사로그·로그인 이력 API

기능 명세: [04-features/11-log.md](../04-features/11-log.md) · 화면: [05-screens/11-log.md](../05-screens/11-log.md) · 메뉴 코드: `AUDIT_LOG`

## API 목록

| ID | 메서드 | URL | 권한 | 화면 | 기능 |
|---|---|---|---|---|---|
| API-LOG-01 | GET | `/audit-logs` | `READ` | SCR-LOG-01 | LOG-01 감사로그 목록 |
| API-LOG-02 | GET | `/audit-logs/{logId}` | `READ` | SCR-LOG-01-P | LOG-02 감사로그 상세 |
| API-LOG-03 | GET | `/audit-logs/excel` | `EXCEL` | SCR-LOG-01 | LOG-03 엑셀 |
| API-LOG-04 | GET | `/login-histories` | `READ` | SCR-LOG-02 | LOG-04 로그인 이력 목록 |
| API-LOG-05 | GET | `/login-histories/excel` | `EXCEL` | SCR-LOG-02 | LOG-05 엑셀 |

## 감사로그 목록 (API-LOG-01)

**파라미터**: `regDtFrom`(필수), `regDtTo`(필수), `admin`(아이디 또는 이름), `menuCd`, `actionCd`, `targetType`, `targetId`, `page`, `size`
**정렬**: `regDt,desc` 고정
**항목**: `logId`, `regDt`, `adminId`, `loginId`, `adminNm`, `menuCd`, `menuNm`, `actionCd`, `actionNm`, `targetType`, `targetId`, `summary`, `ipAddr`

- 기간이 없거나 3개월을 넘으면 400 `VALIDATION_ERROR`.

## 감사로그 상세 (API-LOG-02)

**항목**: 목록 항목 + `reason`, `changes`

```json
"changes": [
  { "field": "statusCd", "label": "상태", "before": "정상", "after": "정지", "changed": true },
  { "field": "companyNm", "label": "기업명", "before": "(주)테스트상사", "after": "(주)테스트상사", "changed": false }
]
```

- 서버가 `BEFORE_DATA`, `AFTER_DATA`를 비교해 항목별 행으로 만들어 준다. 프론트는 `changed`로 강조한다.

## 로그인 이력 목록 (API-LOG-04)

**파라미터**: `regDtFrom`(필수), `regDtTo`(필수), `loginId`, `resultCd`, `authTypeCd`, `ipAddr`, `page`, `size`
**항목**: `histId`, `regDt`, `loginId`, `adminNm`, `resultCd`, `resultNm`, `authTypeCd`, `authTypeNm`, `ipAddr`, `userAgentSummary`
