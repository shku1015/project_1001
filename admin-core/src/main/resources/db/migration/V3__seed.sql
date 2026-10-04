-- 운영 필수 초기 데이터 (docs/03-initial-data.md)
--   코드: docs/03-domain-erd.md 5절
--   메뉴·권한: docs/05-ia-screens.md 1절
--   역할·역할 권한: docs/02-access-model.md 6절 (기계 판독용 사본: docs/data/permission-matrix.csv)
--   마스킹 기본값: docs/04-features/10-masking.md 4절
-- 최초 관리자(admin)는 비밀번호를 환경 변수로 받아야 하므로 인증 단계에서 애플리케이션이 만든다.

-- ===================================================================
-- 코드
-- ===================================================================
INSERT INTO tb_code_group (group_cd, group_nm, system_yn) VALUES
    ('COMPANY_STATUS', '기업 상태', 'Y'),
    ('USER_TYPE', '회원 구분', 'Y'),
    ('USER_STATUS', '회원 상태', 'Y'),
    ('ADMIN_STATUS', '관리자 상태', 'Y'),
    ('MENU_TYPE', '메뉴 종류', 'Y'),
    ('ACTION', '액션', 'Y'),
    ('BOARD_TYPE', '게시판 유형', 'Y'),
    ('WRITER_TYPE', '작성자 구분', 'Y'),
    ('ANSWER_STATUS', '답변 상태', 'Y'),
    ('PRIVACY_FIELD', '개인정보 항목', 'Y'),
    ('PRIVACY_REASON', '개인정보 열람 사유', 'N'),
    ('AUTH_TYPE', '인증 방식', 'Y'),
    ('LOGIN_RESULT', '로그인 결과', 'Y');

INSERT INTO tb_code (group_cd, code, code_nm, sort_ord) VALUES
    ('COMPANY_STATUS', 'ACTIVE', '정상', 1),
    ('COMPANY_STATUS', 'SUSPENDED', '정지', 2),
    ('USER_TYPE', 'PERSONAL', '개인', 1),
    ('USER_TYPE', 'CORPORATE', '기업', 2),
    ('USER_STATUS', 'ACTIVE', '정상', 1),
    ('USER_STATUS', 'DORMANT', '휴면', 2),
    ('USER_STATUS', 'SUSPENDED', '정지', 3),
    ('USER_STATUS', 'WITHDRAWN', '탈퇴', 4),
    ('ADMIN_STATUS', 'ACTIVE', '사용', 1),
    ('ADMIN_STATUS', 'LOCKED', '잠금', 2),
    ('ADMIN_STATUS', 'DISABLED', '사용중지', 3),
    ('MENU_TYPE', 'FOLDER', '폴더', 1),
    ('MENU_TYPE', 'PAGE', '화면', 2),
    ('ACTION', 'READ', '조회', 1),
    ('ACTION', 'CREATE', '등록', 2),
    ('ACTION', 'UPDATE', '수정', 3),
    ('ACTION', 'DELETE', '삭제', 4),
    ('ACTION', 'EXCEL', '엑셀', 5),
    ('ACTION', 'PRIVACY', '개인정보열람', 6),
    ('BOARD_TYPE', 'NOTICE', '공지', 1),
    ('BOARD_TYPE', 'FAQ', 'FAQ', 2),
    ('BOARD_TYPE', 'QNA', 'QnA', 3),
    ('BOARD_TYPE', 'INQUIRY', '1:1문의', 4),
    ('BOARD_TYPE', 'GENERAL', '일반', 5),
    ('WRITER_TYPE', 'USER', '회원', 1),
    ('WRITER_TYPE', 'ADMIN', '관리자', 2),
    ('ANSWER_STATUS', 'WAITING', '답변대기', 1),
    ('ANSWER_STATUS', 'ANSWERED', '답변완료', 2),
    ('PRIVACY_FIELD', 'USER_NM', '이름', 1),
    ('PRIVACY_FIELD', 'EMAIL', '이메일', 2),
    ('PRIVACY_FIELD', 'MOBILE_NO', '휴대폰 번호', 3),
    ('PRIVACY_FIELD', 'BIRTH_DATE', '생년월일', 4),
    ('PRIVACY_REASON', 'CS_INQUIRY', '고객 문의 응대', 1),
    ('PRIVACY_REASON', 'IDENTITY_CHECK', '본인 확인', 2),
    ('PRIVACY_REASON', 'DATA_CORRECTION', '정보 정정', 3),
    ('PRIVACY_REASON', 'ETC', '기타', 99),
    ('AUTH_TYPE', 'SESSION', '세션', 1),
    ('AUTH_TYPE', 'TOKEN', '토큰', 2),
    ('LOGIN_RESULT', 'SUCCESS', '성공', 1),
    ('LOGIN_RESULT', 'FAIL_PWD', '비밀번호 오류', 2),
    ('LOGIN_RESULT', 'FAIL_LOCKED', '잠긴 계정', 3),
    ('LOGIN_RESULT', 'FAIL_DISABLED', '사용중지 계정', 4),
    ('LOGIN_RESULT', 'FAIL_NO_ID', '없는 아이디', 5);

-- ===================================================================
-- 메뉴 트리
-- ===================================================================
INSERT INTO tb_menu (menu_cd, menu_nm, menu_type_cd, menu_url, depth, sort_ord, icon, system_yn) VALUES
    ('MEMBER_ROOT', '회원관리',   'FOLDER', NULL, 1, 1, 'users',    'N'),
    ('BOARD_ROOT',  '게시판관리', 'FOLDER', NULL, 1, 2, 'messages', 'N'),
    ('SYSTEM_ROOT', '시스템관리', 'FOLDER', NULL, 1, 3, 'settings', 'Y'),
    ('ADMIN_ROOT',  '관리자관리', 'FOLDER', NULL, 1, 4, 'shield',   'Y');

INSERT INTO tb_menu (parent_menu_id, menu_cd, menu_nm, menu_type_cd, menu_url, depth, sort_ord, system_yn)
SELECT p.menu_id, v.menu_cd, v.menu_nm, v.menu_type_cd, v.menu_url, 2, v.sort_ord, v.system_yn
FROM (VALUES
    ('MEMBER_ROOT', 'COMPANY',       '기업정보관리',     'PAGE',   '/companies',   1, 'N'),
    ('MEMBER_ROOT', 'USER',          '사용자관리',       'PAGE',   '/users',       2, 'N'),
    ('BOARD_ROOT',  'BOARD',         '게시판 관리',      'PAGE',   '/boards',      1, 'N'),
    ('BOARD_ROOT',  'POST',          '통합 게시글 관리', 'PAGE',   '/posts',       2, 'N'),
    ('BOARD_ROOT',  'POST_BY_BOARD', '게시판별 게시글',  'FOLDER', NULL,           3, 'Y'),
    ('SYSTEM_ROOT', 'CODE',          '코드관리',         'PAGE',   '/codes',       1, 'N'),
    ('SYSTEM_ROOT', 'MENU',          '메뉴관리',         'PAGE',   '/menus',       2, 'Y'),
    ('SYSTEM_ROOT', 'MASKING',       '마스킹 설정',      'PAGE',   '/masking',     3, 'Y'),
    ('SYSTEM_ROOT', 'AUDIT_LOG',     '감사로그',         'PAGE',   '/audit-logs',  4, 'Y'),
    ('ADMIN_ROOT',  'ADMIN',         '관리자',           'PAGE',   '/admins',      1, 'Y'),
    ('ADMIN_ROOT',  'ROLE',          '역할',             'PAGE',   '/roles',       2, 'Y'),
    ('ADMIN_ROOT',  'PERMISSION',    '권한',             'PAGE',   '/permissions', 3, 'Y')
) AS v (parent_cd, menu_cd, menu_nm, menu_type_cd, menu_url, sort_ord, system_yn)
JOIN tb_menu p ON p.menu_cd = v.parent_cd;

-- 화면 메뉴별 사용 액션 → 권한
INSERT INTO tb_permission (menu_id, action_cd)
SELECT m.menu_id, a.action_cd
FROM (VALUES
    ('COMPANY',    'READ CREATE UPDATE DELETE EXCEL'),
    ('USER',       'READ CREATE UPDATE DELETE EXCEL PRIVACY'),
    ('BOARD',      'READ CREATE UPDATE DELETE'),
    ('POST',       'READ CREATE UPDATE DELETE'),
    ('CODE',       'READ CREATE UPDATE DELETE'),
    ('MENU',       'READ CREATE UPDATE DELETE'),
    ('MASKING',    'READ UPDATE'),
    ('AUDIT_LOG',  'READ EXCEL'),
    ('ADMIN',      'READ CREATE UPDATE DELETE'),
    ('ROLE',       'READ CREATE UPDATE DELETE'),
    ('PERMISSION', 'READ UPDATE')
) AS v (menu_cd, actions)
JOIN tb_menu m ON m.menu_cd = v.menu_cd
CROSS JOIN LATERAL unnest(string_to_array(v.actions, ' ')) AS a (action_cd);

-- ===================================================================
-- 역할
-- ===================================================================
INSERT INTO tb_role (role_cd, role_nm, description, system_yn) VALUES
    ('SUPER_ADMIN',      '슈퍼관리자',   '시스템 전체 관리. 권한 체크 예외 (ADR-0002)', 'Y'),
    ('SYSTEM_ADMIN',     '시스템관리자', '메뉴·코드·관리자·역할·권한·마스킹·감사로그',  'N'),
    ('MEMBER_OPERATOR',  '회원운영자',   '기업정보, 사용자관리',                       'N'),
    ('CONTENT_OPERATOR', '콘텐츠운영자', '게시판·게시글 운영',                         'N'),
    ('VIEWER',           '조회전용',     '모니터링, 감사',                             'N');

-- 역할별 권한 (docs/02-access-model.md 6절). 슈퍼관리자는 권한 체크 예외라 넣지 않는다.
INSERT INTO tb_role_permission (role_id, perm_id)
SELECT r.role_id, p.perm_id
FROM (VALUES
    ('SYSTEM_ADMIN',     'BOARD',      'READ CREATE UPDATE DELETE'),
    ('SYSTEM_ADMIN',     'CODE',       'READ CREATE UPDATE DELETE'),
    ('SYSTEM_ADMIN',     'MENU',       'READ CREATE UPDATE DELETE'),
    ('SYSTEM_ADMIN',     'ADMIN',      'READ CREATE UPDATE DELETE'),
    ('SYSTEM_ADMIN',     'ROLE',       'READ CREATE UPDATE DELETE'),
    ('SYSTEM_ADMIN',     'PERMISSION', 'READ UPDATE'),
    ('SYSTEM_ADMIN',     'MASKING',    'READ UPDATE'),
    ('SYSTEM_ADMIN',     'AUDIT_LOG',  'READ EXCEL'),
    ('MEMBER_OPERATOR',  'COMPANY',    'READ CREATE UPDATE EXCEL'),
    ('MEMBER_OPERATOR',  'USER',       'READ CREATE UPDATE EXCEL PRIVACY'),
    ('MEMBER_OPERATOR',  'CODE',       'READ'),
    ('CONTENT_OPERATOR', 'BOARD',      'READ'),
    ('CONTENT_OPERATOR', 'POST',       'READ CREATE UPDATE DELETE'),
    ('CONTENT_OPERATOR', 'CODE',       'READ'),
    ('VIEWER',           'COMPANY',    'READ'),
    ('VIEWER',           'USER',       'READ'),
    ('VIEWER',           'BOARD',      'READ'),
    ('VIEWER',           'POST',       'READ'),
    ('VIEWER',           'AUDIT_LOG',  'READ')
) AS v (role_cd, menu_cd, actions)
JOIN tb_role r ON r.role_cd = v.role_cd
JOIN tb_menu m ON m.menu_cd = v.menu_cd
CROSS JOIN LATERAL unnest(string_to_array(v.actions, ' ')) AS a (action_cd)
JOIN tb_permission p ON p.menu_id = m.menu_id AND p.action_cd = a.action_cd;

-- ===================================================================
-- 마스킹 기본값: 모두 마스킹
-- ===================================================================
INSERT INTO tb_masking_policy (field_cd, screen_mask_yn, excel_mask_yn) VALUES
    ('USER_NM', 'Y', 'Y'),
    ('EMAIL', 'Y', 'Y'),
    ('MOBILE_NO', 'Y', 'Y'),
    ('BIRTH_DATE', 'Y', 'Y');
