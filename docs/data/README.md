# 기계 판독용 명세 데이터

사람이 읽는 표(Markdown)를 테스트가 읽을 수 있는 형태로 옮긴 파일이다. 테스트가 이 파일과 실제 DB 초기 데이터·코드를 대조한다.
원본 표를 바꾸면 이 파일도 함께 바꾼다.

| 파일 | 원본 | 쓰는 테스트 |
|---|---|---|
| [permission-matrix.csv](permission-matrix.csv) | [02-access-model.md](../02-access-model.md) 6절 권한 매트릭스 | `SeedDataTest` (초기 데이터의 역할 권한과 일치하는지), 이후 권한 403 테스트 |

## permission-matrix.csv

- 한 줄 = 역할 하나가 메뉴 하나에서 가진 액션 목록 (공백 구분).
- 슈퍼관리자(`SUPER_ADMIN`)는 권한 체크 예외라 넣지 않는다 (ADR-0002).
- 줄이 없는 역할·메뉴 조합은 권한 없음이다.
