# 관리자 서비스

사용자 서비스(개인·기업 회원)를 운영하기 위한 관리자 서비스입니다. 학습 목적으로 같은 화면 명세를 세 가지 방식으로 구현합니다.

| 프론트 | 경로 | 인증 |
|---|---|---|
| ① React | `/react` | 토큰 (JWT) |
| ② JSP + REST API | `/jsp` | 토큰 (JWT) |
| ③ JSP 서버 렌더링 | `/ssr` | 세션 |

- 백엔드: 전자정부프레임워크 5.0.2 (Spring Boot 3.5.6, Java 17), PostgreSQL 16
- 기획 문서: [docs/](docs/00-roadmap.md) · 결정 기록: [docs/adr/](docs/adr/README.md)

## 시작하기

필요한 것: JDK 17 이상, Node.js 22, Docker

```bash
cp .env.example .env          # DB_PASSWORD 등 값 채우기
docker compose up -d          # 개발용 PostgreSQL
./mvnw -pl admin-web -am spring-boot:run   # 서버: http://localhost:8080
cd admin-react && npm install && npm run dev  # React 개발 서버
```

## 검증

```bash
./verify.sh            # 전체 (CI와 같은 검사)
./verify.sh backend    # 백엔드: 빌드·테스트 (Docker 필요)
./verify.sh frontend   # 프론트: lint·타입 검사·테스트·빌드
```
