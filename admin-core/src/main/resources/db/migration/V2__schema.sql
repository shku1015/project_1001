-- 관리자 서비스 테이블 (docs/03-domain-erd.md, ADR-0007)
-- 공통 칼럼: reg_id / reg_dt / mod_id / mod_dt. 시스템이 넣은 행은 reg_id가 비어 있다.
-- 동시 수정 방지는 mod_dt 비교로 한다 (docs/06-api-spec.md 7절).

-- ===================================================================
-- 코드·설정
-- ===================================================================
CREATE TABLE tb_code_group (
    group_cd     VARCHAR(50)  PRIMARY KEY,
    group_nm     VARCHAR(100) NOT NULL,
    description  VARCHAR(500),
    system_yn    CHAR(1)      NOT NULL DEFAULT 'N' CHECK (system_yn IN ('Y', 'N')),
    use_yn       CHAR(1)      NOT NULL DEFAULT 'Y' CHECK (use_yn IN ('Y', 'N')),
    reg_id       BIGINT,
    reg_dt       TIMESTAMP    NOT NULL DEFAULT now(),
    mod_id       BIGINT,
    mod_dt       TIMESTAMP    NOT NULL DEFAULT now()
);

CREATE TABLE tb_code (
    group_cd     VARCHAR(50)  NOT NULL REFERENCES tb_code_group (group_cd),
    code         VARCHAR(50)  NOT NULL,
    code_nm      VARCHAR(100) NOT NULL,
    sort_ord     INT          NOT NULL DEFAULT 0 CHECK (sort_ord >= 0),
    description  VARCHAR(500),
    use_yn       CHAR(1)      NOT NULL DEFAULT 'Y' CHECK (use_yn IN ('Y', 'N')),
    reg_id       BIGINT,
    reg_dt       TIMESTAMP    NOT NULL DEFAULT now(),
    mod_id       BIGINT,
    mod_dt       TIMESTAMP    NOT NULL DEFAULT now(),
    PRIMARY KEY (group_cd, code)
);

CREATE TABLE tb_masking_policy (
    field_cd        VARCHAR(50) PRIMARY KEY,
    screen_mask_yn  CHAR(1)     NOT NULL DEFAULT 'Y' CHECK (screen_mask_yn IN ('Y', 'N')),
    excel_mask_yn   CHAR(1)     NOT NULL DEFAULT 'Y' CHECK (excel_mask_yn IN ('Y', 'N')),
    mod_id          BIGINT,
    mod_dt          TIMESTAMP   NOT NULL DEFAULT now()
);

-- ===================================================================
-- 회원
-- ===================================================================
CREATE TABLE tb_company (
    company_id   BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    company_nm   VARCHAR(100) NOT NULL,
    biz_reg_no   VARCHAR(10)  NOT NULL UNIQUE CHECK (biz_reg_no ~ '^[0-9]{10}$'),
    ceo_nm       VARCHAR(50)  NOT NULL,
    biz_type     VARCHAR(100),
    biz_item     VARCHAR(100),
    tel_no       VARCHAR(20),
    zip_cd       VARCHAR(5),
    addr         VARCHAR(200),
    addr_dtl     VARCHAR(200),
    status_cd    VARCHAR(20)  NOT NULL DEFAULT 'ACTIVE',
    del_yn       CHAR(1)      NOT NULL DEFAULT 'N' CHECK (del_yn IN ('Y', 'N')),
    reg_id       BIGINT,
    reg_dt       TIMESTAMP    NOT NULL DEFAULT now(),
    mod_id       BIGINT,
    mod_dt       TIMESTAMP    NOT NULL DEFAULT now()
);

CREATE TABLE tb_user (
    user_id        BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    login_id       VARCHAR(50)  NOT NULL UNIQUE,
    password       VARCHAR(200) NOT NULL,
    pwd_temp_yn    CHAR(1)      NOT NULL DEFAULT 'N' CHECK (pwd_temp_yn IN ('Y', 'N')),
    user_type_cd   VARCHAR(20)  NOT NULL,
    company_id     BIGINT       REFERENCES tb_company (company_id),
    user_nm        VARCHAR(50)  NOT NULL,
    email          VARCHAR(100) NOT NULL,
    mobile_no      VARCHAR(20),
    birth_date     DATE,
    dept_nm        VARCHAR(100),
    position_nm    VARCHAR(50),
    status_cd      VARCHAR(20)  NOT NULL DEFAULT 'ACTIVE',
    join_dt        TIMESTAMP    NOT NULL DEFAULT now(),
    last_login_dt  TIMESTAMP,
    withdraw_dt    TIMESTAMP,
    del_yn         CHAR(1)      NOT NULL DEFAULT 'N' CHECK (del_yn IN ('Y', 'N')),
    reg_id         BIGINT,
    reg_dt         TIMESTAMP    NOT NULL DEFAULT now(),
    mod_id         BIGINT,
    mod_dt         TIMESTAMP    NOT NULL DEFAULT now(),
    -- 기업 회원은 소속 기업 필수, 개인 회원은 소속 기업 없음
    CONSTRAINT ck_user_company CHECK (
        (user_type_cd = 'CORPORATE' AND company_id IS NOT NULL)
        OR (user_type_cd = 'PERSONAL' AND company_id IS NULL)
    )
);

CREATE TABLE tb_user_status_hist (
    hist_id           BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    user_id           BIGINT       NOT NULL REFERENCES tb_user (user_id),
    before_status_cd  VARCHAR(20)  NOT NULL,
    after_status_cd   VARCHAR(20)  NOT NULL,
    reason            VARCHAR(500) NOT NULL,
    reg_id            BIGINT,
    reg_dt            TIMESTAMP    NOT NULL DEFAULT now()
);

-- ===================================================================
-- 관리자·접근제어
-- ===================================================================
CREATE TABLE tb_admin (
    admin_id        BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    login_id        VARCHAR(50)  NOT NULL UNIQUE,
    password        VARCHAR(200) NOT NULL,
    admin_nm        VARCHAR(50)  NOT NULL,
    email           VARCHAR(100) NOT NULL,
    mobile_no       VARCHAR(20),
    dept_nm         VARCHAR(100),
    status_cd       VARCHAR(20)  NOT NULL DEFAULT 'ACTIVE',
    pwd_temp_yn     CHAR(1)      NOT NULL DEFAULT 'Y' CHECK (pwd_temp_yn IN ('Y', 'N')),
    login_fail_cnt  INT          NOT NULL DEFAULT 0 CHECK (login_fail_cnt >= 0),
    last_login_dt   TIMESTAMP,
    pwd_changed_dt  TIMESTAMP,
    reg_id          BIGINT,
    reg_dt          TIMESTAMP    NOT NULL DEFAULT now(),
    mod_id          BIGINT,
    mod_dt          TIMESTAMP    NOT NULL DEFAULT now()
);

CREATE TABLE tb_role (
    role_id      BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    role_cd      VARCHAR(50)  NOT NULL UNIQUE CHECK (role_cd ~ '^[A-Z0-9_]+$'),
    role_nm      VARCHAR(100) NOT NULL,
    description  VARCHAR(500),
    system_yn    CHAR(1)      NOT NULL DEFAULT 'N' CHECK (system_yn IN ('Y', 'N')),
    use_yn       CHAR(1)      NOT NULL DEFAULT 'Y' CHECK (use_yn IN ('Y', 'N')),
    reg_id       BIGINT,
    reg_dt       TIMESTAMP    NOT NULL DEFAULT now(),
    mod_id       BIGINT,
    mod_dt       TIMESTAMP    NOT NULL DEFAULT now()
);

CREATE TABLE tb_admin_role (
    admin_id  BIGINT    NOT NULL REFERENCES tb_admin (admin_id),
    role_id   BIGINT    NOT NULL REFERENCES tb_role (role_id),
    reg_id    BIGINT,
    reg_dt    TIMESTAMP NOT NULL DEFAULT now(),
    PRIMARY KEY (admin_id, role_id)
);

CREATE TABLE tb_menu (
    menu_id         BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    parent_menu_id  BIGINT       REFERENCES tb_menu (menu_id),
    menu_cd         VARCHAR(50)  NOT NULL UNIQUE CHECK (menu_cd ~ '^[A-Z0-9_]+$'),
    menu_nm         VARCHAR(100) NOT NULL,
    menu_type_cd    VARCHAR(20)  NOT NULL CHECK (menu_type_cd IN ('FOLDER', 'PAGE')),
    menu_url        VARCHAR(200),
    depth           INT          NOT NULL CHECK (depth BETWEEN 1 AND 3),
    sort_ord        INT          NOT NULL DEFAULT 0,
    icon            VARCHAR(50),
    use_yn          CHAR(1)      NOT NULL DEFAULT 'Y' CHECK (use_yn IN ('Y', 'N')),
    system_yn       CHAR(1)      NOT NULL DEFAULT 'N' CHECK (system_yn IN ('Y', 'N')),
    reg_id          BIGINT,
    reg_dt          TIMESTAMP    NOT NULL DEFAULT now(),
    mod_id          BIGINT,
    mod_dt          TIMESTAMP    NOT NULL DEFAULT now(),
    -- 화면 메뉴는 URL 필수, 폴더 메뉴는 URL 없음
    CONSTRAINT ck_menu_url CHECK (
        (menu_type_cd = 'PAGE' AND menu_url IS NOT NULL)
        OR (menu_type_cd = 'FOLDER' AND menu_url IS NULL)
    )
);

CREATE TABLE tb_permission (
    perm_id    BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    menu_id    BIGINT      NOT NULL REFERENCES tb_menu (menu_id),
    action_cd  VARCHAR(20) NOT NULL CHECK (action_cd IN ('READ', 'CREATE', 'UPDATE', 'DELETE', 'EXCEL', 'PRIVACY')),
    reg_dt     TIMESTAMP   NOT NULL DEFAULT now(),
    UNIQUE (menu_id, action_cd)
);

CREATE TABLE tb_role_permission (
    role_id  BIGINT    NOT NULL REFERENCES tb_role (role_id),
    perm_id  BIGINT    NOT NULL REFERENCES tb_permission (perm_id),
    reg_id   BIGINT,
    reg_dt   TIMESTAMP NOT NULL DEFAULT now(),
    PRIMARY KEY (role_id, perm_id)
);

-- ===================================================================
-- 게시판
-- ===================================================================
CREATE TABLE tb_board_master (
    board_id            BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    board_cd            VARCHAR(50)  NOT NULL UNIQUE CHECK (board_cd ~ '^[A-Z0-9_]+$'),
    board_nm            VARCHAR(100) NOT NULL,
    board_type_cd       VARCHAR(20)  NOT NULL,
    category_group_cd   VARCHAR(50)  REFERENCES tb_code_group (group_cd),
    comment_yn          CHAR(1)      NOT NULL DEFAULT 'N' CHECK (comment_yn IN ('Y', 'N')),
    attach_yn           CHAR(1)      NOT NULL DEFAULT 'N' CHECK (attach_yn IN ('Y', 'N')),
    attach_max_cnt      INT          CHECK (attach_max_cnt BETWEEN 1 AND 10),
    attach_max_size_mb  INT          CHECK (attach_max_size_mb BETWEEN 1 AND 50),
    secret_yn           CHAR(1)      NOT NULL DEFAULT 'N' CHECK (secret_yn IN ('Y', 'N')),
    reply_yn            CHAR(1)      NOT NULL DEFAULT 'N' CHECK (reply_yn IN ('Y', 'N')),
    user_write_yn       CHAR(1)      NOT NULL DEFAULT 'N' CHECK (user_write_yn IN ('Y', 'N')),
    menu_id             BIGINT       REFERENCES tb_menu (menu_id),
    use_yn              CHAR(1)      NOT NULL DEFAULT 'Y' CHECK (use_yn IN ('Y', 'N')),
    del_yn              CHAR(1)      NOT NULL DEFAULT 'N' CHECK (del_yn IN ('Y', 'N')),
    reg_id              BIGINT,
    reg_dt              TIMESTAMP    NOT NULL DEFAULT now(),
    mod_id              BIGINT,
    mod_dt              TIMESTAMP    NOT NULL DEFAULT now()
);

-- 원글과 답글을 한 테이블에 둔다 (ADR-0009)
CREATE TABLE tb_post (
    post_id           BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    board_id          BIGINT       NOT NULL REFERENCES tb_board_master (board_id),
    root_post_id      BIGINT       REFERENCES tb_post (post_id),   -- 원글이면 자기 자신 (INSERT 후 채움)
    parent_post_id    BIGINT       REFERENCES tb_post (post_id),
    depth             INT          NOT NULL DEFAULT 0 CHECK (depth >= 0),
    thread_ord        INT          NOT NULL DEFAULT 1,
    category_cd       VARCHAR(50),
    title             VARCHAR(200) NOT NULL,
    content           TEXT         NOT NULL,
    writer_type_cd    VARCHAR(20)  NOT NULL CHECK (writer_type_cd IN ('USER', 'ADMIN')),
    writer_id         BIGINT       NOT NULL,
    top_fixed_yn      CHAR(1)      NOT NULL DEFAULT 'N' CHECK (top_fixed_yn IN ('Y', 'N')),
    secret_yn         CHAR(1)      NOT NULL DEFAULT 'N' CHECK (secret_yn IN ('Y', 'N')),
    display_yn        CHAR(1)      NOT NULL DEFAULT 'Y' CHECK (display_yn IN ('Y', 'N')),
    view_cnt          INT          NOT NULL DEFAULT 0,
    answer_status_cd  VARCHAR(20),
    del_yn            CHAR(1)      NOT NULL DEFAULT 'N' CHECK (del_yn IN ('Y', 'N')),
    reg_id            BIGINT,
    reg_dt            TIMESTAMP    NOT NULL DEFAULT now(),
    mod_id            BIGINT,
    mod_dt            TIMESTAMP    NOT NULL DEFAULT now(),
    CONSTRAINT ck_post_parent CHECK ((depth = 0) = (parent_post_id IS NULL))
);

CREATE TABLE tb_comment (
    comment_id         BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    post_id            BIGINT        NOT NULL REFERENCES tb_post (post_id),
    parent_comment_id  BIGINT        REFERENCES tb_comment (comment_id),
    content            VARCHAR(1000) NOT NULL,
    writer_type_cd     VARCHAR(20)   NOT NULL CHECK (writer_type_cd IN ('USER', 'ADMIN')),
    writer_id          BIGINT        NOT NULL,
    del_yn             CHAR(1)       NOT NULL DEFAULT 'N' CHECK (del_yn IN ('Y', 'N')),
    reg_id             BIGINT,
    reg_dt             TIMESTAMP     NOT NULL DEFAULT now(),
    mod_id             BIGINT,
    mod_dt             TIMESTAMP     NOT NULL DEFAULT now()
);

-- 게시글 첨부파일과 본문 이미지 (ADR-0012)
CREATE TABLE tb_attachment (
    file_id         BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    ref_type_cd     VARCHAR(20)  NOT NULL CHECK (ref_type_cd IN ('POST', 'POST_IMAGE')),
    ref_id          BIGINT,      -- 본문 이미지는 글 저장 전까지 비어 있다
    orig_file_nm    VARCHAR(255) NOT NULL,
    stored_file_nm  VARCHAR(255) NOT NULL UNIQUE,
    file_path       VARCHAR(500) NOT NULL,
    file_size       BIGINT       NOT NULL CHECK (file_size >= 0),
    content_type    VARCHAR(100),
    sort_ord        INT          NOT NULL DEFAULT 0,
    del_yn          CHAR(1)      NOT NULL DEFAULT 'N' CHECK (del_yn IN ('Y', 'N')),
    reg_id          BIGINT,
    reg_dt          TIMESTAMP    NOT NULL DEFAULT now(),
    mod_id          BIGINT,
    mod_dt          TIMESTAMP    NOT NULL DEFAULT now(),
    CONSTRAINT ck_attachment_ref CHECK (ref_type_cd = 'POST_IMAGE' OR ref_id IS NOT NULL)
);

-- ===================================================================
-- 인증
-- ===================================================================
CREATE TABLE tb_admin_refresh_token (
    token_id    BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    admin_id    BIGINT       NOT NULL REFERENCES tb_admin (admin_id),
    token_hash  VARCHAR(100) NOT NULL UNIQUE,
    expires_dt  TIMESTAMP    NOT NULL,
    used_yn     CHAR(1)      NOT NULL DEFAULT 'N' CHECK (used_yn IN ('Y', 'N')),
    revoked_yn  CHAR(1)      NOT NULL DEFAULT 'N' CHECK (revoked_yn IN ('Y', 'N')),
    ip_addr     VARCHAR(45)  NOT NULL,
    user_agent  VARCHAR(500),
    reg_dt      TIMESTAMP    NOT NULL DEFAULT now()
);

-- ===================================================================
-- 로그
-- ===================================================================
CREATE TABLE tb_admin_login_hist (
    hist_id       BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    admin_id      BIGINT       REFERENCES tb_admin (admin_id),   -- 없는 아이디로 시도하면 비어 있다
    login_id      VARCHAR(50)  NOT NULL,
    result_cd     VARCHAR(20)  NOT NULL,
    auth_type_cd  VARCHAR(20)  NOT NULL CHECK (auth_type_cd IN ('SESSION', 'TOKEN')),
    ip_addr       VARCHAR(45)  NOT NULL,
    user_agent    VARCHAR(500),
    reg_dt        TIMESTAMP    NOT NULL DEFAULT now()
);

-- 감사로그: 월별 범위 파티션, 기본키 (log_id, reg_dt) (docs/08-architecture.md 7.2)
CREATE TABLE tb_audit_log (
    log_id       BIGINT GENERATED ALWAYS AS IDENTITY,
    admin_id     BIGINT       NOT NULL,
    menu_cd      VARCHAR(50)  NOT NULL,
    action_cd    VARCHAR(20)  NOT NULL,
    target_type  VARCHAR(50)  NOT NULL,
    target_id    VARCHAR(100),
    summary      VARCHAR(500) NOT NULL,
    before_data  JSONB,
    after_data   JSONB,
    reason       VARCHAR(500),
    ip_addr      VARCHAR(45)  NOT NULL,
    reg_dt       TIMESTAMP    NOT NULL DEFAULT now(),
    PRIMARY KEY (log_id, reg_dt)
) PARTITION BY RANGE (reg_dt);

-- 이번 달·다음 달 파티션. 이후 달은 배치(BAT-04)가 미리 만든다. 범위 밖 행은 기본 파티션으로 간다.
DO $$
DECLARE
    m DATE := date_trunc('month', now())::date;
    i INT;
BEGIN
    FOR i IN 0..1 LOOP
        EXECUTE format(
            'CREATE TABLE IF NOT EXISTS tb_audit_log_%s PARTITION OF tb_audit_log FOR VALUES FROM (%L) TO (%L)',
            to_char(m + make_interval(months => i), 'YYYYMM'),
            m + make_interval(months => i),
            m + make_interval(months => i + 1));
    END LOOP;
END $$;
CREATE TABLE tb_audit_log_default PARTITION OF tb_audit_log DEFAULT;

-- ===================================================================
-- 인덱스 (docs/08-architecture.md 7.3)
-- ===================================================================
CREATE INDEX ix_user_company      ON tb_user (company_id);
CREATE INDEX ix_user_status       ON tb_user (status_cd);
CREATE INDEX ix_user_join_dt      ON tb_user (join_dt);
CREATE INDEX ix_user_nm_trgm      ON tb_user USING gin (user_nm gin_trgm_ops);
CREATE INDEX ix_user_email_trgm   ON tb_user USING gin (email gin_trgm_ops);
CREATE INDEX ix_user_mobile_trgm  ON tb_user USING gin (mobile_no gin_trgm_ops);
CREATE INDEX ix_company_nm_trgm   ON tb_company USING gin (company_nm gin_trgm_ops);
CREATE INDEX ix_user_status_hist  ON tb_user_status_hist (user_id, reg_dt);
CREATE INDEX ix_menu_parent       ON tb_menu (parent_menu_id, sort_ord);
CREATE INDEX ix_post_board_reg    ON tb_post (board_id, reg_dt);
CREATE INDEX ix_post_root         ON tb_post (root_post_id, thread_ord);
CREATE INDEX ix_comment_post      ON tb_comment (post_id);
CREATE INDEX ix_attachment_ref    ON tb_attachment (ref_type_cd, ref_id);
CREATE INDEX ix_refresh_admin     ON tb_admin_refresh_token (admin_id);
CREATE INDEX ix_login_hist_dt     ON tb_admin_login_hist (reg_dt);
CREATE INDEX ix_login_hist_id_dt  ON tb_admin_login_hist (login_id, reg_dt);
CREATE INDEX ix_audit_dt          ON tb_audit_log (reg_dt);
CREATE INDEX ix_audit_admin_dt    ON tb_audit_log (admin_id, reg_dt);
CREATE INDEX ix_audit_target      ON tb_audit_log (target_type, target_id);
