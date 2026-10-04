# ADR-0018. GitHub Flow와 GitHub Actions CI

- 상태: 승인
- 날짜: 2026-10-05
- 관련 문서: [08-architecture.md](../08-architecture.md) 8.2

## 맥락

하네스 엔지니어링으로 개발한다. 에이전트가 작업한 결과를 사람이 일일이 실행해 보지 않아도, 매 변경마다 빌드·테스트가 자동으로 돌고 결과가 남아야 한다. 저장소는 GitHub(`shku1015/project_1001`, 공개)를 쓴다. 혼자 개발하며 에이전트가 함께 작업한다.

## 검토한 대안

**브랜치 전략**

| 대안 | 장점 | 단점 |
|---|---|---|
| `main`에 직접 커밋 | 가장 단순 | 깨진 코드가 바로 `main`에 들어감. CI가 사후 확인만 함 |
| **GitHub Flow** (`main` + 작업 브랜치 + PR) | 단순하면서 PR마다 CI로 확인 후 병합 | PR을 만드는 단계가 추가됨 |
| Git Flow (`develop`, `release` 등) | 배포 주기 관리에 적합 | 1인 학습 프로젝트에는 과함 |

**CI**

| 대안 | 판단 |
|---|---|
| **GitHub Actions** | 저장소와 같은 곳. 공개 저장소는 무료·무제한. Docker가 있어 Testcontainers가 그대로 동작 |
| 외부 CI (Jenkins 등) | 서버를 따로 운영해야 함 |

## 결정

- **GitHub Flow**를 쓴다. 작업 브랜치 이름은 `feat/{마일스톤}-{기능}`, `fix/...`, `docs/...`.
- `main` 보호: PR 필수, 강제 push·삭제 금지, **관리자도 예외 없음**. 혼자 개발하므로 승인 리뷰 수는 0으로 둔다 (본인 PR을 본인이 승인할 수 없기 때문).
- 병합은 CI 통과 후 Squash merge.
- CI는 GitHub Actions. 작업은 `backend`, `frontend`, `e2e`(세 프론트 3회 실행), 선택으로 `api-snapshot`.
- CI 통과를 병합 필수 조건으로 거는 것은 CI 파일을 만든 뒤에 추가한다.

## 결과

- 좋은 점: `main`이 항상 동작하는 상태로 유지된다. 에이전트는 `gh pr checks`, `gh run view --log-failed`로 실패를 스스로 읽고 고칠 수 있다.
- 감수할 점: 문서 한 줄을 고쳐도 브랜치와 PR이 필요하다. 관리자 예외가 없으므로 급할 때도 보호 규칙을 잠시 풀어야 직접 push할 수 있다.
