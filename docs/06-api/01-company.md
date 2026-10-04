# 06-01. 기업정보관리 API

기능 명세: [04-features/01-company.md](../04-features/01-company.md) · 화면: [05-screens/01-company.md](../05-screens/01-company.md) · 메뉴 코드: `COMPANY`

## API 목록

| ID | 메서드 | URL | 권한 | 화면 | 기능 |
|---|---|---|---|---|---|
| API-COM-01 | GET | `/companies` | `READ` | SCR-COM-01 | COM-01 목록 |
| API-COM-02 | GET | `/companies/excel` | `EXCEL` | SCR-COM-01 | COM-08 엑셀 |
| API-COM-03 | GET | `/companies/{companyId}` | `READ` | SCR-COM-02, 03 | COM-02 상세 |
| API-COM-04 | GET | `/companies/{companyId}/users` | `READ` | SCR-COM-02 | COM-07 소속 회원 |
| API-COM-05 | GET | `/companies/check-biz-reg-no?bizRegNo=` | `CREATE` | SCR-COM-03 | 사업자등록번호 중복 확인 |
| API-COM-06 | POST | `/companies` | `CREATE` | SCR-COM-03 | COM-03 등록 |
| API-COM-07 | PUT | `/companies/{companyId}` | `UPDATE` | SCR-COM-03 | COM-04 수정 |
| API-COM-08 | PATCH | `/companies/{companyId}/status` | `UPDATE` | SCR-COM-02 | COM-05 상태 변경 |
| API-COM-09 | DELETE | `/companies/{companyId}` | `DELETE` | SCR-COM-02 | COM-06 삭제 |

## 목록 (API-COM-01)

**검색 파라미터**: `companyNm`, `bizRegNo`, `ceoNm`, `statusCd`, `regDtFrom`, `regDtTo`, `page`, `size`, `sort`
**정렬 가능**: `companyNm`, `memberCnt`, `regDt` (기본 `regDt,desc`)

**항목**: `companyId`, `companyNm`, `bizRegNo`, `ceoNm`, `memberCnt`, `statusCd`, `statusNm`, `regDt`

## 상세 (API-COM-03)

**항목**: `companyId`, `companyNm`, `bizRegNo`, `ceoNm`, `bizType`, `bizItem`, `telNo`, `zipCd`, `addr`, `addrDtl`, `statusCd`, `statusNm`, `memberCnt`, `regNm`, `regDt`, `modNm`, `modDt`

- `regNm`, `modNm`은 등록·수정한 관리자 이름이다.

## 소속 회원 (API-COM-04)

**파라미터**: `size` (기본 10)
**항목**: `userId`, `loginId`, `userNm`(마스킹 설정 적용), `deptNm`, `positionNm`, `statusCd`, `statusNm`, `joinDt`, 그리고 전체 건수 `totalCount`

## 등록·수정 (API-COM-06, 07)

**요청**

```json
{
  "companyNm": "(주)테스트상사",
  "bizRegNo": "1000000001",
  "ceoNm": "김대표",
  "bizType": "도소매",
  "bizItem": "전자제품",
  "telNo": "0212345678",
  "zipCd": "06236",
  "addr": "서울특별시 강남구 테헤란로 1",
  "addrDtl": "10층",
  "modDt": "2026-10-04T14:30:15"
}
```

- 등록: `modDt` 없음. 응답 201 `{ "companyId": 12 }`
- 수정: `bizRegNo`를 보내도 무시한다. `modDt` 필수.

## 상태 변경 (API-COM-08)

```json
{ "statusCd": "SUSPENDED", "reason": "사업자 휴업 확인", "modDt": "2026-10-04T14:30:15" }
```

## 삭제 (API-COM-09)

- 본문 없음.

## 업무 오류 코드

| 코드 | 상황 |
|---|---|
| `DUPLICATE` | 사업자등록번호 중복 (`fieldErrors`에 `bizRegNo`) |
| `COMPANY_HAS_MEMBERS` | 소속 회원이 있어 삭제 불가 |
| `INVALID_STATUS_CHANGE` | 같은 상태로 변경 요청 |
