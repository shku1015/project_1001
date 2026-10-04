# 06-05. 통합게시판관리 API

기능 명세: [04-features/05-board.md](../04-features/05-board.md) · 화면: [05-screens/05-board.md](../05-screens/05-board.md) · 메뉴 코드: `BOARD`, `POST`, `POST_{게시판 코드}`

## 1. 게시판 관리

| ID | 메서드 | URL | 권한 | 화면 | 기능 |
|---|---|---|---|---|---|
| API-BRD-01 | GET | `/boards` | `BOARD` `READ` | SCR-BRD-01 | BRD-01 목록 |
| API-BRD-02 | GET | `/boards/{boardId}` | `BOARD` `READ` | SCR-BRD-02, 03 | BRD-02 상세 |
| API-BRD-03 | GET | `/boards/check-board-cd?boardCd=` | `BOARD` `CREATE` | SCR-BRD-03 | 게시판 코드 중복 확인 |
| API-BRD-04 | GET | `/boards/type-defaults/{boardTypeCd}` | `BOARD` `CREATE` | SCR-BRD-03 | 유형별 옵션 기본값 |
| API-BRD-05 | POST | `/boards` | `BOARD` `CREATE` | SCR-BRD-03 | BRD-03 등록 |
| API-BRD-06 | PUT | `/boards/{boardId}` | `BOARD` `UPDATE` | SCR-BRD-03 | BRD-04 수정 |
| API-BRD-07 | DELETE | `/boards/{boardId}` | `BOARD` `DELETE` | SCR-BRD-02 | BRD-05 삭제 |
| API-BRD-08 | POST | `/boards/{boardId}/menu` | `BOARD` `UPDATE` | SCR-BRD-02 | BRD-06 관리 메뉴 생성 |
| API-BRD-09 | DELETE | `/boards/{boardId}/menu` | `BOARD` `UPDATE` | SCR-BRD-02 | BRD-06 관리 메뉴 삭제 |
| API-BRD-10 | GET | `/boards/options` | `POST` 또는 `POST_*` 중 하나라도 `READ` | SCR-PST-01 | 게시판 선택 상자용. 내가 볼 수 있는 게시판만 |

**목록 파라미터**: `boardNm`, `boardTypeCd`, `useYn`, `page`, `size`, `sort` (정렬: `boardCd`, `boardNm`, `postCnt`, `regDt`)
**목록 항목**: `boardId`, `boardCd`, `boardNm`, `boardTypeCd`, `boardTypeNm`, `postCnt`, `menuYn`, `useYn`, `regDt`

**등록·수정 요청**

```json
{
  "boardCd": "NOTICE",
  "boardNm": "공지사항",
  "boardTypeCd": "NOTICE",
  "categoryGroupCd": null,
  "userWriteYn": "N",
  "commentYn": "N",
  "attachYn": "Y",
  "attachMaxCnt": 5,
  "attachMaxSizeMb": 10,
  "secretYn": "N",
  "replyYn": "N",
  "createMenuYn": "Y",
  "useYn": "Y",
  "modDt": null
}
```

- `createMenuYn`은 등록 때만 쓴다. 수정에서는 `boardCd`, `boardTypeCd`, `createMenuYn`을 무시한다.
- 상세 응답에는 위 항목 + `postCnt`, `menuId`, `menuNm`, 등록·수정 정보를 준다.
- 관리 메뉴 삭제(API-BRD-09)는 `?dryRun=Y`로 회수될 역할 목록을 먼저 받을 수 있다 (메뉴 API와 같은 방식).

## 2. 게시글

게시글 API의 권한은 **`POST` 또는 `POST_{게시판 코드}` 중 하나**의 액션이 있으면 통과한다. 아래 표의 권한 칸은 액션만 적는다.

| ID | 메서드 | URL | 권한 | 화면 | 기능 |
|---|---|---|---|---|---|
| API-PST-01 | GET | `/boards/{boardCd}/posts` | `READ` | SCR-PST-01 | PST-01 목록 (원글만) |
| API-PST-02 | GET | `/posts/{postId}` | `READ` | SCR-PST-02 | PST-02 상세 (원글 + 답글 트리 + 첨부) |
| API-PST-03 | POST | `/boards/{boardCd}/posts` | `CREATE` | SCR-PST-03 | PST-03 원글 작성 (multipart) |
| API-PST-04 | POST | `/posts/{postId}/replies` | `CREATE` | SCR-PST-03 | PST-08 답글 작성 (multipart) |
| API-PST-05 | PUT | `/posts/{postId}` | `UPDATE` | SCR-PST-03 | PST-04 수정 (multipart) |
| API-PST-06 | PATCH | `/posts/{postId}/display` | `UPDATE` | SCR-PST-02 | PST-05 게시 여부 |
| API-PST-07 | PATCH | `/posts/{postId}/top-fixed` | `UPDATE` | SCR-PST-02 | PST-06 상단 고정 |
| API-PST-08 | DELETE | `/posts/{postId}` | `DELETE` | SCR-PST-02 | PST-07 삭제 |
| API-PST-09 | GET | `/posts/{postId}/comments` | `READ` | SCR-PST-02 | 댓글 목록 |
| API-PST-10 | POST | `/posts/{postId}/comments` | `CREATE` | SCR-PST-02 | PST-09 댓글·대댓글 작성 |
| API-PST-11 | DELETE | `/comments/{commentId}` | `DELETE` | SCR-PST-02 | PST-10 댓글 삭제 |
| API-PST-12 | GET | `/attachments/{fileId}` | `READ` | SCR-PST-02 | PST-11 첨부 다운로드 |
| API-PST-13 | POST | `/boards/{boardCd}/images` | `CREATE` 또는 `UPDATE` | SCR-PST-03 | PST-12 본문 이미지 업로드 (multipart, 파일 1개) |

### 본문 이미지 업로드 (API-PST-13)

- 요청: `multipart/form-data`, 파트 `file` 1개.
- 응답: `{ "fileId": 51, "url": "/files/images/3f2a...e9.png" }`. 프론트는 이 `url`을 에디터 본문에 `<img src>`로 넣는다.
- 이미지는 인증 없이 `GET /files/images/{UUID}.{확장자}`로 열린다 (`/api/v1` 밖의 경로, [07-nonfunctional.md](../07-nonfunctional.md) NF-FL-13).
- 게시글을 저장하면 서버가 본문의 이미지 주소를 찾아 해당 이미지를 그 글에 연결한다.
- ③ JSP SSR은 같은 기능을 세션 인증 경로 `POST /ssr/boards/{boardCd}/images`(CSRF 토큰 포함)로 제공한다. 응답 형식은 같다.
- 오류: `FILE_TYPE_NOT_ALLOWED`(이미지 아님), 413 `FILE_TOO_LARGE`(5MB 초과).

- `{postId}`, `{commentId}`, `{fileId}`로 부르는 API는 서버가 그 글이 속한 게시판 코드를 찾아 권한을 확인한다.

### 목록 (API-PST-01)

**파라미터**: `categoryCd`, `searchType`(`TITLE` / `CONTENT` / `TITLE_CONTENT` / `WRITER`), `keyword`, `writerTypeCd`, `displayYn`, `answerStatusCd`, `regDtFrom`, `regDtTo`, `page`, `size`, `sort` (정렬: `viewCnt`, `regDt`)

**응답**: 페이징 목록 + 상단 고정 글 목록

```json
{
  "success": true,
  "data": {
    "topFixed": [ { "postId": 1, "title": "서비스 점검 안내" } ],
    "items": [
      {
        "postId": 31, "categoryCd": null, "categoryNm": null, "title": "환불은 어떻게 하나요?",
        "secretYn": "Y", "attachCnt": 1, "commentCnt": 0, "replyCnt": 3,
        "writerTypeCd": "USER", "writerNm": "p_user01",
        "answerStatusCd": "WAITING", "answerStatusNm": "답변대기",
        "displayYn": "Y", "viewCnt": 12, "regDt": "2026-10-04T14:30:00"
      }
    ],
    "page": 1, "size": 20, "totalCount": 45, "totalPages": 3
  },
  "error": null
}
```

- `writerNm`: 회원이면 로그인 아이디, 관리자면 관리자 이름.

### 상세 (API-PST-02)

```json
{
  "success": true,
  "data": {
    "board": { "boardCd": "QNA", "boardNm": "묻고 답하기", "commentYn": "N", "replyYn": "Y", "attachYn": "Y" },
    "post": {
      "postId": 31, "title": "...", "content": "...", "writerTypeCd": "USER", "writerNm": "p_user01",
      "secretYn": "Y", "displayYn": "Y", "topFixedYn": "N", "viewCnt": 12,
      "answerStatusCd": "WAITING", "adminModifiedYn": "N", "delYn": "N",
      "attachments": [ { "fileId": 7, "origFileNm": "영수증.pdf", "fileSize": 1258291 } ],
      "regDt": "...", "modNm": null, "modDt": "..."
    },
    "replies": [
      {
        "postId": 32, "parentPostId": 31, "depth": 1, "content": "...", "writerTypeCd": "ADMIN", "writerNm": "홍길동",
        "delYn": "N", "attachments": [], "regDt": "...", "modDt": "...",
        "children": [ { "postId": 33, "parentPostId": 32, "depth": 2, "children": [] } ]
      }
    ]
  },
  "error": null
}
```

- `replies`는 트리 구조다. 삭제된 답글이 하위 답글을 가지면 `delYn: "Y"`, 내용은 `null`로 준다.
- 관리자 조회는 조회수를 올리지 않는다.

### 작성·수정 (API-PST-03, 04, 05)

`multipart/form-data`

| 파트 | 내용 |
|---|---|
| `data` | JSON (아래) |
| `files` | 새 첨부파일 (여러 개) |

```json
{
  "categoryCd": null,
  "title": "서비스 점검 안내",
  "content": "<p>...</p>",
  "topFixedYn": "N",
  "secretYn": "N",
  "displayYn": "Y",
  "deleteFileIds": [],
  "modifyReason": null,
  "modDt": null
}
```

- 답글(API-PST-04): `categoryCd`, `topFixedYn`, `secretYn`, `displayYn`은 무시한다.
- 수정(API-PST-05): `deleteFileIds`로 기존 첨부를 지운다. 회원 글이면 `modifyReason` 필수.
- 첨부 개수·크기는 게시판 설정으로 검사한다. 허용 확장자는 [07-nonfunctional.md](../07-nonfunctional.md) NF-FL-01.

### 게시 여부·상단 고정·삭제

```json
{ "displayYn": "N", "reason": "광고성 게시글", "modDt": "..." }
```

```json
{ "topFixedYn": "Y", "modDt": "..." }
```

- 삭제(API-PST-08), 댓글 삭제(API-PST-11): 회원 글·댓글이면 본문 `{ "reason": "..." }` 필수.

### 댓글 (API-PST-09, 10)

**목록 항목**: `commentId`, `parentCommentId`, `content`, `writerTypeCd`, `writerNm`, `delYn`, `regDt`, `children`
**작성 요청**: `{ "parentCommentId": null, "content": "확인했습니다." }`

## 3. 업무 오류 코드

| 코드 | 상황 |
|---|---|
| `DUPLICATE` | 게시판 코드 중복 |
| `BOARD_MENU_EXISTS` / `BOARD_MENU_NOT_FOUND` | 관리 메뉴 중복 생성 / 없는 메뉴 삭제 |
| `REPLY_NOT_ALLOWED` | 답글 사용 안 하는 게시판에 답글 |
| `COMMENT_NOT_ALLOWED` | 댓글 사용 안 하는 게시판에 댓글 |
| `COMMENT_DEPTH_EXCEEDED` | 대댓글에 다시 댓글 |
| `ATTACH_NOT_ALLOWED` / `ATTACH_COUNT_EXCEEDED` | 첨부 불가 게시판 / 개수 초과 |
| `FILE_TOO_LARGE` (413) | 파일 크기 초과 |
| `FILE_TYPE_NOT_ALLOWED` | 허용되지 않은 확장자 |
| `REASON_REQUIRED` | 회원 글·댓글 수정·숨김·삭제에 사유 없음 |
