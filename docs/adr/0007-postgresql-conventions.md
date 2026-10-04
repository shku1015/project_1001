# ADR-0007. PostgreSQL과 테이블 작성 규칙

- 상태: 승인
- 날짜: 2026-10-04
- 관련 문서: [03-domain-erd.md](../03-domain-erd.md) 1절, [08-architecture.md](../08-architecture.md) 7절

## 맥락

DB는 PostgreSQL로 정했다. 전자정부프레임워크 관례(대문자 칼럼, `_YN` 칼럼)와 PostgreSQL의 특성을 어떻게 맞출지 정해야 했다.

## 검토한 대안

| 항목 | 대안 | 판단 |
|---|---|---|
| 여부 칼럼 | **`CHAR(1)` `'Y'/'N'`** / PostgreSQL `BOOLEAN` | `CHAR(1)`: 전자정부프레임워크 관례, MyBatis 매핑 단순, API 값과 같게 유지 |
| 기본키 | **`BIGINT GENERATED ALWAYS AS IDENTITY`** / `SERIAL` / UUID | IDENTITY: PostgreSQL 표준 방식 |
| 이름 | 문서는 대문자, **실제 DDL은 소문자** | PostgreSQL은 따옴표 없는 이름을 소문자로 저장 |
| 변경 전·후 데이터 | `TEXT` / **`JSONB`** | JSONB: 구조 확인, 필요 시 조회 가능 |
| 부분 검색 | 일반 인덱스 / **`pg_trgm` GIN** | `LIKE '%값%'`에 인덱스를 쓰려면 pg_trgm 필요 |
| 감사로그 | 단일 테이블 / **월별 파티션** | 2년 보관. 지난 달은 파티션째 삭제 |

## 결정

위 표의 굵은 항목으로 정한다. 테이블 변경은 Flyway SQL 파일로 관리한다.

## 결과

- 좋은 점: 대량 로그 삭제가 빠르고(파티션 삭제), 부분 일치 검색에도 인덱스를 쓴다.
- 감수할 점: 감사로그 기본키가 `(LOG_ID, REG_DT)` 복합키가 된다. 다음 달 파티션을 배치로 미리 만들어야 한다. `CHAR(1)`에는 CHECK 제약을 따로 걸어야 한다.
