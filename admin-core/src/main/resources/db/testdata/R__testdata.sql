-- 개발·테스트용 데이터 (docs/03-initial-data.md 3절). local 프로필에서만 적용한다. 운영에는 넣지 않는다.
-- 모든 값은 가상이다. 이메일은 example.com, 휴대폰은 0100000xxxx.
-- 이미 들어가 있으면 건너뛴다. 다시 넣으려면 DB를 초기화한다 (docker compose down -v).
-- 테스트 관리자 비밀번호는 Flyway placeholder ${testAdminPassword}로 받아 pgcrypto로 BCrypt 해시한다.

CREATE EXTENSION IF NOT EXISTS pgcrypto;

DO $$
DECLARE
    v_pwd        TEXT := '${testAdminPassword}';
    v_hash       TEXT;
    a_member     BIGINT;
    a_content    BIGINT;
    c1 BIGINT; c2 BIGINT; c3 BIGINT; c4 BIGINT;
    b_notice BIGINT; b_faq BIGINT; b_qna BIGINT; b_inquiry BIGINT; b_free BIGINT;
    m_parent BIGINT;
    q BIGINT; r1 BIGINT; r2 BIGINT; r3 BIGINT; p BIGINT; cm BIGINT;
    i INT;
BEGIN
    IF EXISTS (SELECT 1 FROM tb_admin WHERE login_id = 't_super') THEN
        RAISE NOTICE '테스트 데이터가 이미 있어 건너뜀';
        RETURN;
    END IF;
    IF v_pwd = '' THEN
        RAISE EXCEPTION '테스트 관리자 비밀번호(TEST_ADMIN_PASSWORD)가 없습니다';
    END IF;
    v_hash := crypt(v_pwd, gen_salt('bf', 10));

    -- ---------------------------------------------------------------
    -- 3.1 테스트 관리자
    -- ---------------------------------------------------------------
    INSERT INTO tb_admin (login_id, password, admin_nm, email, dept_nm, status_cd, pwd_temp_yn, pwd_changed_dt)
    VALUES
        ('t_super',    v_hash, '테스트슈퍼',     't_super@example.com',    '운영팀', 'ACTIVE',   'N', now()),
        ('t_system',   v_hash, '테스트시스템',   't_system@example.com',   '운영팀', 'ACTIVE',   'N', now()),
        ('t_member',   v_hash, '테스트회원운영', 't_member@example.com',   'CS팀',   'ACTIVE',   'N', now()),
        ('t_content',  v_hash, '테스트콘텐츠',   't_content@example.com',  '콘텐츠팀', 'ACTIVE', 'N', now()),
        ('t_viewer',   v_hash, '테스트조회',     't_viewer@example.com',   '감사팀', 'ACTIVE',   'N', now()),
        ('t_multi',    v_hash, '테스트다중역할', 't_multi@example.com',    'CS팀',   'ACTIVE',   'N', now()),
        ('t_norole',   v_hash, '테스트역할없음', 't_norole@example.com',   '운영팀', 'ACTIVE',   'N', now()),
        ('t_locked',   v_hash, '테스트잠김',     't_locked@example.com',   '감사팀', 'LOCKED',   'N', now()),
        ('t_disabled', v_hash, '테스트중지',     't_disabled@example.com', '감사팀', 'DISABLED', 'N', now());
    UPDATE tb_admin SET login_fail_cnt = 5 WHERE login_id = 't_locked';

    INSERT INTO tb_admin_role (admin_id, role_id)
    SELECT a.admin_id, r.role_id
    FROM (VALUES
        ('t_super', 'SUPER_ADMIN'), ('t_system', 'SYSTEM_ADMIN'), ('t_member', 'MEMBER_OPERATOR'),
        ('t_content', 'CONTENT_OPERATOR'), ('t_viewer', 'VIEWER'),
        ('t_multi', 'MEMBER_OPERATOR'), ('t_multi', 'CONTENT_OPERATOR'),
        ('t_locked', 'VIEWER'), ('t_disabled', 'VIEWER')
    ) AS v (login_id, role_cd)
    JOIN tb_admin a ON a.login_id = v.login_id
    JOIN tb_role r ON r.role_cd = v.role_cd;

    SELECT admin_id INTO a_member FROM tb_admin WHERE login_id = 't_member';
    SELECT admin_id INTO a_content FROM tb_admin WHERE login_id = 't_content';

    -- ---------------------------------------------------------------
    -- 3.2 기업
    -- ---------------------------------------------------------------
    INSERT INTO tb_company (company_nm, biz_reg_no, ceo_nm, biz_type, biz_item, tel_no, status_cd, reg_id)
    VALUES ('(주)테스트상사', '1000000001', '김대표', '도소매', '전자제품', '0212340001', 'ACTIVE', a_member)
    RETURNING company_id INTO c1;
    INSERT INTO tb_company (company_nm, biz_reg_no, ceo_nm, status_cd, reg_id)
    VALUES ('테스트솔루션(주)', '1000000002', '이대표', 'ACTIVE', a_member) RETURNING company_id INTO c2;
    INSERT INTO tb_company (company_nm, biz_reg_no, ceo_nm, status_cd, reg_id)
    VALUES ('테스트물산', '1000000003', '박대표', 'ACTIVE', a_member) RETURNING company_id INTO c3;
    INSERT INTO tb_company (company_nm, biz_reg_no, ceo_nm, status_cd, reg_id)
    VALUES ('테스트유통(주)', '1000000004', '최대표', 'SUSPENDED', a_member) RETURNING company_id INTO c4;
    INSERT INTO tb_company (company_nm, biz_reg_no, ceo_nm, status_cd, reg_id)
    VALUES ('테스트빈기업', '1000000005', '정대표', 'ACTIVE', a_member);

    -- ---------------------------------------------------------------
    -- 3.3 회원 (이름은 마스킹 확인용으로 2·3·4자를 섞는다. 비밀번호는 로그인 불가 값)
    -- ---------------------------------------------------------------
    INSERT INTO tb_user (login_id, password, user_type_cd, company_id, user_nm, email, mobile_no, birth_date,
                         dept_nm, position_nm, status_cd, pwd_temp_yn, withdraw_dt, del_yn, reg_id)
    VALUES
        ('p_user01', 'TEST-NOT-USABLE', 'PERSONAL', NULL, '김하나',   'p_user01@example.com', '01000000001', '1990-01-01', NULL, NULL, 'ACTIVE',    'N', NULL, 'N', NULL),
        ('p_user02', 'TEST-NOT-USABLE', 'PERSONAL', NULL, '이둘',     'p_user02@example.com', '01000000002', '1991-02-02', NULL, NULL, 'ACTIVE',    'N', NULL, 'N', NULL),
        ('p_user03', 'TEST-NOT-USABLE', 'PERSONAL', NULL, '박세나라', 'p_user03@example.com', '01000000003', '1992-03-03', NULL, NULL, 'ACTIVE',    'N', NULL, 'N', NULL),
        ('p_user04', 'TEST-NOT-USABLE', 'PERSONAL', NULL, '최넷',     'p_user04@example.com', '01000000004', NULL,         NULL, NULL, 'ACTIVE',    'N', NULL, 'N', NULL),
        ('p_user05', 'TEST-NOT-USABLE', 'PERSONAL', NULL, '정다섯',   'p_user05@example.com', NULL,          NULL,         NULL, NULL, 'ACTIVE',    'N', NULL, 'N', NULL),
        ('p_user06', 'TEST-NOT-USABLE', 'PERSONAL', NULL, '강여섯',   'p_user06@example.com', '01000000006', '1985-06-06', NULL, NULL, 'DORMANT',   'N', NULL, 'N', NULL),
        ('p_user07', 'TEST-NOT-USABLE', 'PERSONAL', NULL, '조일곱',   'p_user07@example.com', '01000000007', '1987-07-07', NULL, NULL, 'SUSPENDED', 'N', NULL, 'N', NULL),
        ('p_user08', 'TEST-NOT-USABLE', 'PERSONAL', NULL, '윤여덟',   'p_user08@example.com', '01000000008', '1988-08-08', NULL, NULL, 'WITHDRAWN', 'N', now() - interval '10 days', 'N', NULL),
        ('p_user09', 'TEST-NOT-USABLE', 'PERSONAL', NULL, '장아홉',   'p_user09@example.com', '01000000009', '1989-09-09', NULL, NULL, 'ACTIVE',    'Y', NULL, 'N', a_member),
        ('p_user10', 'TEST-NOT-USABLE', 'PERSONAL', NULL, '임열',     'p_user10@example.com', '01000000010', '1990-10-10', NULL, NULL, 'ACTIVE',    'N', NULL, 'Y', a_member),
        ('c_user01', 'TEST-NOT-USABLE', 'CORPORATE', c1, '한기업',   'c_user01@example.com', '01000000011', NULL, '영업팀', '대리', 'ACTIVE',    'N', NULL, 'N', NULL),
        ('c_user02', 'TEST-NOT-USABLE', 'CORPORATE', c1, '오상사',   'c_user02@example.com', '01000000012', NULL, '영업팀', '과장', 'ACTIVE',    'N', NULL, 'N', NULL),
        ('c_user03', 'TEST-NOT-USABLE', 'CORPORATE', c1, '서영업부', 'c_user03@example.com', '01000000013', NULL, '기획팀', '사원', 'ACTIVE',    'N', NULL, 'N', NULL),
        ('c_user04', 'TEST-NOT-USABLE', 'CORPORATE', c1, '신퇴사',   'c_user04@example.com', '01000000014', NULL, '기획팀', '사원', 'WITHDRAWN', 'N', now() - interval '20 days', 'N', NULL),
        ('c_user05', 'TEST-NOT-USABLE', 'CORPORATE', c2, '권솔',     'c_user05@example.com', '01000000015', NULL, '개발팀', '팀장', 'ACTIVE',    'N', NULL, 'N', NULL),
        ('c_user06', 'TEST-NOT-USABLE', 'CORPORATE', c2, '황루션',   'c_user06@example.com', '01000000016', NULL, '개발팀', '선임', 'ACTIVE',    'N', NULL, 'N', NULL),
        ('c_user07', 'TEST-NOT-USABLE', 'CORPORATE', c2, '안관리자', 'c_user07@example.com', '01000000017', NULL, '지원팀', '사원', 'ACTIVE',    'Y', NULL, 'N', a_member),
        ('c_user08', 'TEST-NOT-USABLE', 'CORPORATE', c3, '송물산',   'c_user08@example.com', '01000000018', NULL, '구매팀', '대리', 'ACTIVE',    'N', NULL, 'N', NULL),
        ('c_user09', 'TEST-NOT-USABLE', 'CORPORATE', c3, '전정지',   'c_user09@example.com', '01000000019', NULL, '구매팀', '사원', 'SUSPENDED', 'N', NULL, 'N', NULL),
        ('c_user10', 'TEST-NOT-USABLE', 'CORPORATE', c4, '홍유통',   'c_user10@example.com', '01000000020', NULL, '물류팀', '과장', 'ACTIVE',    'N', NULL, 'N', NULL);

    INSERT INTO tb_user_status_hist (user_id, before_status_cd, after_status_cd, reason, reg_id)
    SELECT user_id, 'ACTIVE', 'SUSPENDED', '약관 위반 신고 확인 (테스트)', a_member
    FROM tb_user WHERE login_id IN ('p_user07', 'c_user09');

    -- 페이징·엑셀 확인용 대량 회원 100명
    INSERT INTO tb_user (login_id, password, user_type_cd, user_nm, email, mobile_no, status_cd, join_dt)
    SELECT 'bulk_' || lpad(n::text, 4, '0'), 'TEST-NOT-USABLE', 'PERSONAL', '대량' || n,
           'bulk_' || lpad(n::text, 4, '0') || '@example.com', '0101' || lpad(n::text, 7, '0'),
           'ACTIVE', now() - make_interval(days => n)
    FROM generate_series(1, 100) AS n;

    -- ---------------------------------------------------------------
    -- 3.4 게시판, 게시판별 관리 메뉴, 게시글
    -- ---------------------------------------------------------------
    INSERT INTO tb_code_group (group_cd, group_nm, system_yn) VALUES ('FAQ_CATEGORY', 'FAQ 분류', 'N');
    INSERT INTO tb_code (group_cd, code, code_nm, sort_ord) VALUES
        ('FAQ_CATEGORY', 'ACCOUNT', '계정', 1),
        ('FAQ_CATEGORY', 'SERVICE', '서비스 이용', 2);

    INSERT INTO tb_board_master (board_cd, board_nm, board_type_cd, category_group_cd, user_write_yn, comment_yn,
                                 attach_yn, attach_max_cnt, attach_max_size_mb, secret_yn, reply_yn, reg_id)
    VALUES ('NOTICE', '공지사항', 'NOTICE', NULL, 'N', 'N', 'Y', 5, 10, 'N', 'N', a_content) RETURNING board_id INTO b_notice;
    INSERT INTO tb_board_master (board_cd, board_nm, board_type_cd, category_group_cd, user_write_yn, comment_yn,
                                 attach_yn, secret_yn, reply_yn, reg_id)
    VALUES ('FAQ', '자주 묻는 질문', 'FAQ', 'FAQ_CATEGORY', 'N', 'N', 'N', 'N', 'N', a_content) RETURNING board_id INTO b_faq;
    INSERT INTO tb_board_master (board_cd, board_nm, board_type_cd, user_write_yn, comment_yn,
                                 attach_yn, attach_max_cnt, attach_max_size_mb, secret_yn, reply_yn, reg_id)
    VALUES ('QNA', '묻고 답하기', 'QNA', 'Y', 'N', 'Y', 5, 10, 'Y', 'Y', a_content) RETURNING board_id INTO b_qna;
    INSERT INTO tb_board_master (board_cd, board_nm, board_type_cd, user_write_yn, comment_yn,
                                 attach_yn, attach_max_cnt, attach_max_size_mb, secret_yn, reply_yn, reg_id)
    VALUES ('INQUIRY', '1:1 문의', 'INQUIRY', 'Y', 'N', 'Y', 5, 10, 'Y', 'Y', a_content) RETURNING board_id INTO b_inquiry;
    INSERT INTO tb_board_master (board_cd, board_nm, board_type_cd, user_write_yn, comment_yn,
                                 attach_yn, attach_max_cnt, attach_max_size_mb, secret_yn, reply_yn, reg_id)
    VALUES ('FREE', '자유게시판', 'GENERAL', 'Y', 'Y', 'Y', 5, 10, 'N', 'N', a_content) RETURNING board_id INTO b_free;

    -- 게시판별 관리 메뉴 (FREE는 만들지 않음)
    SELECT menu_id INTO m_parent FROM tb_menu WHERE menu_cd = 'POST_BY_BOARD';
    INSERT INTO tb_menu (parent_menu_id, menu_cd, menu_nm, menu_type_cd, menu_url, depth, sort_ord)
    SELECT m_parent, 'POST_' || b.board_cd, b.board_nm || ' 관리', 'PAGE', '/posts/' || b.board_cd, 3,
           row_number() OVER (ORDER BY b.board_id)
    FROM tb_board_master b WHERE b.board_cd IN ('NOTICE', 'FAQ', 'QNA', 'INQUIRY');
    UPDATE tb_board_master b SET menu_id = m.menu_id FROM tb_menu m WHERE m.menu_cd = 'POST_' || b.board_cd;
    INSERT INTO tb_permission (menu_id, action_cd)
    SELECT m.menu_id, a.action_cd
    FROM tb_menu m CROSS JOIN unnest(ARRAY['READ', 'CREATE', 'UPDATE', 'DELETE']) AS a (action_cd)
    WHERE m.parent_menu_id = m_parent;

    -- 공지: 관리자 글 5건 (상단 고정 1, 숨김 1)
    FOR i IN 1..5 LOOP
        INSERT INTO tb_post (board_id, title, content, writer_type_cd, writer_id, top_fixed_yn, display_yn, reg_id)
        VALUES (b_notice, '공지 ' || i, '<p>공지 내용 ' || i || '</p>', 'ADMIN', a_content,
                CASE WHEN i = 1 THEN 'Y' ELSE 'N' END, CASE WHEN i = 5 THEN 'N' ELSE 'Y' END, a_content);
    END LOOP;

    -- FAQ: 관리자 글 6건 (분류 2개에 3건씩)
    FOR i IN 1..6 LOOP
        INSERT INTO tb_post (board_id, category_cd, title, content, writer_type_cd, writer_id, reg_id)
        VALUES (b_faq, CASE WHEN i <= 3 THEN 'ACCOUNT' ELSE 'SERVICE' END, 'FAQ 질문 ' || i,
                '<p>FAQ 답변 ' || i || '</p>', 'ADMIN', a_content, a_content);
    END LOOP;

    -- QnA: 답글 구조 확인용 5건 (ADR-0009)
    -- 질문 1: 답글 없음
    INSERT INTO tb_post (board_id, title, content, writer_type_cd, writer_id, secret_yn, answer_status_cd)
    VALUES (b_qna, '환불은 어떻게 하나요?', '<p>질문 1</p>', 'USER', (SELECT user_id FROM tb_user WHERE login_id = 'p_user01'), 'Y', 'WAITING');
    -- 질문 2: 관리자 답글 1
    INSERT INTO tb_post (board_id, title, content, writer_type_cd, writer_id, secret_yn, answer_status_cd)
    VALUES (b_qna, '배송 기간이 궁금합니다', '<p>질문 2</p>', 'USER', (SELECT user_id FROM tb_user WHERE login_id = 'p_user02'), 'Y', 'ANSWERED')
    RETURNING post_id INTO q;
    INSERT INTO tb_post (board_id, root_post_id, parent_post_id, depth, thread_ord, title, content, writer_type_cd, writer_id, secret_yn, reg_id)
    VALUES (b_qna, q, q, 1, 2, 'RE: 배송 기간이 궁금합니다', '<p>답변 2</p>', 'ADMIN', a_content, 'Y', a_content);
    -- 질문 3: 관리자 답글 → 회원 재답글
    INSERT INTO tb_post (board_id, title, content, writer_type_cd, writer_id, secret_yn, answer_status_cd)
    VALUES (b_qna, '메뉴가 보이지 않습니다', '<p>질문 3</p>', 'USER', (SELECT user_id FROM tb_user WHERE login_id = 'p_user03'), 'Y', 'WAITING')
    RETURNING post_id INTO q;
    INSERT INTO tb_post (board_id, root_post_id, parent_post_id, depth, thread_ord, title, content, writer_type_cd, writer_id, secret_yn, reg_id)
    VALUES (b_qna, q, q, 1, 2, 'RE: 메뉴가 보이지 않습니다', '<p>답변 3</p>', 'ADMIN', a_content, 'Y', a_content) RETURNING post_id INTO r1;
    INSERT INTO tb_post (board_id, root_post_id, parent_post_id, depth, thread_ord, title, content, writer_type_cd, writer_id, secret_yn)
    VALUES (b_qna, q, r1, 2, 3, 'RE: RE: 메뉴가 보이지 않습니다', '<p>재질문 3</p>', 'USER', (SELECT user_id FROM tb_user WHERE login_id = 'p_user03'), 'Y');
    -- 질문 4: 관리자 → 회원 → 관리자 (깊이 3)
    INSERT INTO tb_post (board_id, title, content, writer_type_cd, writer_id, secret_yn, answer_status_cd)
    VALUES (b_qna, '결제가 두 번 되었습니다', '<p>질문 4</p>', 'USER', (SELECT user_id FROM tb_user WHERE login_id = 'p_user04'), 'Y', 'ANSWERED')
    RETURNING post_id INTO q;
    INSERT INTO tb_post (board_id, root_post_id, parent_post_id, depth, thread_ord, title, content, writer_type_cd, writer_id, secret_yn, reg_id)
    VALUES (b_qna, q, q, 1, 2, 'RE: 결제가 두 번 되었습니다', '<p>답변 4</p>', 'ADMIN', a_content, 'Y', a_content) RETURNING post_id INTO r1;
    INSERT INTO tb_post (board_id, root_post_id, parent_post_id, depth, thread_ord, title, content, writer_type_cd, writer_id, secret_yn)
    VALUES (b_qna, q, r1, 2, 3, 'RE: RE: 결제가 두 번 되었습니다', '<p>재질문 4</p>', 'USER', (SELECT user_id FROM tb_user WHERE login_id = 'p_user04'), 'Y') RETURNING post_id INTO r2;
    INSERT INTO tb_post (board_id, root_post_id, parent_post_id, depth, thread_ord, title, content, writer_type_cd, writer_id, secret_yn, reg_id)
    VALUES (b_qna, q, r2, 3, 4, 'RE: RE: RE: 결제가 두 번 되었습니다', '<p>재답변 4</p>', 'ADMIN', a_content, 'Y', a_content) RETURNING post_id INTO r3;
    -- 질문 5: 원글 삭제 + 관리자 답글 ("삭제된 글입니다" 표시 확인)
    INSERT INTO tb_post (board_id, title, content, writer_type_cd, writer_id, secret_yn, answer_status_cd, del_yn)
    VALUES (b_qna, '삭제된 질문', '<p>질문 5</p>', 'USER', (SELECT user_id FROM tb_user WHERE login_id = 'p_user05'), 'Y', 'ANSWERED', 'Y')
    RETURNING post_id INTO q;
    INSERT INTO tb_post (board_id, root_post_id, parent_post_id, depth, thread_ord, title, content, writer_type_cd, writer_id, secret_yn, reg_id)
    VALUES (b_qna, q, q, 1, 2, 'RE: 삭제된 질문', '<p>답변 5</p>', 'ADMIN', a_content, 'Y', a_content);

    -- 1:1 문의: 회원 글 3건 (답변대기 2, 답변완료 1)
    FOR i IN 1..3 LOOP
        INSERT INTO tb_post (board_id, title, content, writer_type_cd, writer_id, secret_yn, answer_status_cd)
        VALUES (b_inquiry, '문의 ' || i, '<p>문의 내용 ' || i || '</p>', 'USER',
                (SELECT user_id FROM tb_user WHERE login_id = 'c_user0' || i), 'Y',
                CASE WHEN i = 3 THEN 'ANSWERED' ELSE 'WAITING' END)
        RETURNING post_id INTO q;
        IF i = 3 THEN
            INSERT INTO tb_post (board_id, root_post_id, parent_post_id, depth, thread_ord, title, content, writer_type_cd, writer_id, secret_yn, reg_id)
            VALUES (b_inquiry, q, q, 1, 2, 'RE: 문의 3', '<p>문의 답변 3</p>', 'ADMIN', a_content, 'Y', a_content);
        END IF;
    END LOOP;

    -- 자유게시판: 회원 글 5건, 댓글·대댓글, 첨부 1건 (첨부는 DB 행만 있고 실제 파일은 없다)
    FOR i IN 1..5 LOOP
        INSERT INTO tb_post (board_id, title, content, writer_type_cd, writer_id)
        VALUES (b_free, '자유글 ' || i, '<p>자유글 내용 ' || i || '</p>', 'USER',
                (SELECT user_id FROM tb_user WHERE login_id = 'p_user0' || i))
        RETURNING post_id INTO p;
        IF i = 1 THEN
            INSERT INTO tb_comment (post_id, content, writer_type_cd, writer_id)
            VALUES (p, '첫 댓글', 'USER', (SELECT user_id FROM tb_user WHERE login_id = 'p_user02'))
            RETURNING comment_id INTO cm;
            INSERT INTO tb_comment (post_id, parent_comment_id, content, writer_type_cd, writer_id, reg_id)
            VALUES (p, cm, '관리자 대댓글', 'ADMIN', a_content, a_content);
            INSERT INTO tb_comment (post_id, content, writer_type_cd, writer_id)
            VALUES (p, '두 번째 댓글', 'USER', (SELECT user_id FROM tb_user WHERE login_id = 'p_user03'));
            INSERT INTO tb_attachment (ref_type_cd, ref_id, orig_file_nm, stored_file_nm, file_path, file_size, content_type)
            VALUES ('POST', p, '안내문.pdf', 'testdata-0001.pdf', '2026/10/testdata-0001.pdf', 1258291, 'application/pdf');
        END IF;
    END LOOP;

    -- 원글은 root_post_id가 자기 자신
    UPDATE tb_post SET root_post_id = post_id WHERE depth = 0 AND root_post_id IS NULL;
END $$;
