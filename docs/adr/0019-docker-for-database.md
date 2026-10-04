# ADR-0019. Docker는 개발·테스트용 PostgreSQL에만 사용

- 상태: 승인
- 날짜: 2026-10-05
- 관련 문서: [08-architecture.md](../08-architecture.md) 8.1

## 맥락

개발과 테스트에 PostgreSQL이 필요하다. 이 프로젝트는 JSONB, `pg_trgm`, 월별 파티션 같은 PostgreSQL 전용 기능을 쓴다. 로컬과 CI에서 같은 방식으로 테스트해야 에이전트의 검증 결과를 믿을 수 있다.

## 검토한 대안

| 대안 | 장점 | 단점 |
|---|---|---|
| PC에 PostgreSQL 직접 설치 | Docker 불필요 | 사람마다 버전이 다를 수 있음. 테스트마다 깨끗한 DB를 만들기 어려움 |
| 테스트에 H2(메모리 DB) | 빠름, 설치 불필요 | PostgreSQL 전용 기능을 검증할 수 없음 |
| **Docker Compose(개발) + Testcontainers(테스트)** | 버전 통일. 테스트마다 새 DB. CI와 같은 방식 | Docker가 필요함 |
| 앱까지 Docker 이미지로 실행 | 배포 환경과 비슷 | 지금 단계에는 필요 없음 (배포 범위 밖) |

## 결정

- 개발용 DB는 `docker-compose.yml`로 PostgreSQL 16을 띄운다.
- 테스트는 Testcontainers로 테스트마다 PostgreSQL 컨테이너를 만든다. H2는 쓰지 않는다.
- 앱 자체를 Docker 이미지로 만드는 것은 이번 범위에서 제외한다.

## 결과

- 좋은 점: 로컬과 CI(GitHub Actions)가 같은 테스트를 같은 방식으로 실행한다. PostgreSQL 전용 기능까지 검증한다.
- 감수할 점: 개발 PC에 Docker가 필요하다 (현재 PC는 설치·`sudo` 없이 실행 확인함). 컨테이너를 띄우는 만큼 통합 테스트 시작이 몇 초 느리다.
