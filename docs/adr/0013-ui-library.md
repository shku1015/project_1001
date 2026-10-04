# ADR-0013. UI는 Bootstrap 5 + Tabler

- 상태: 승인
- 날짜: 2026-10-04
- 관련 문서: [08-architecture.md](../08-architecture.md) 2.2

## 맥락

세 프론트(React, JSP + API, JSP SSR)가 같은 화면 명세를 구현하므로, 겉모습도 같아야 비교가 쉽다. 그래서 UI 라이브러리는 **React와 일반 HTML(JSP) 양쪽에서 같은 모양으로 쓸 수 있어야** 한다.

## 검토한 대안

| 대안 | 장점 | 단점 |
|---|---|---|
| **Bootstrap 5 + Tabler 테마** | 자료가 많음. 모달 등 JS 동작 내장. Tabler로 관리자 화면 모양이 갖춰짐 | 디자인이 흔함 |
| Tailwind CSS + daisyUI | 최신 방식, 디자인 자유도 | JSP에도 CSS 빌드 단계 필요. 동작은 직접 구현 |
| Bulma | 가볍고 단순 | JS 동작 없음, 사용자 층 작음 |
| Web Components (Shoelace 계열) | 같은 컴포넌트를 React·HTML에 그대로 사용 | 국내 자료 적음 |
| Ant Design, MUI | 관리자용 컴포넌트가 강력 | **React 전용**이라 JSP와 모양을 맞출 수 없음 → 제외 |

## 결정

- 세 프론트 모두 **Bootstrap 5 + Tabler**. React는 Tabler CSS + React-Bootstrap 컴포넌트.
- 함께 정한 공통 라이브러리: 에디터 **Quill**, 트리 드래그 **SortableJS**, 우편번호 **카카오 우편번호 서비스**.

## 결과

- 좋은 점: 세 프론트가 같은 클래스·같은 외부 라이브러리를 써서 결과 화면을 나란히 비교하기 쉽다.
- 감수할 점: React에서 Quill은 래퍼 패키지 대신 직접 연결 컴포넌트를 만들어야 한다. 카카오 우편번호 서비스 때문에 CSP에 외부 도메인 예외가 생긴다.
