import pg from 'pg'

/**
 * E2E 전용 계정을 매번 새로 만든다. 비밀번호 변경·내 정보 수정처럼 데이터를 바꾸는 시나리오가
 * 여러 번 실행해도 같은 결과를 내도록, 이전 실행의 계정은 지우고 다시 만든다.
 * 서버와 같은 DB(DB_HOST 등 환경 변수)에 접속한다. 테스트 데이터(local 프로필)가 들어가 있어야 한다.
 */
export const FRONTS = ['react', 'jsp', 'ssr'] as const

export function e2ePassword(): string {
  const password = process.env.TEST_ADMIN_PASSWORD
  if (!password) {
    throw new Error('TEST_ADMIN_PASSWORD 환경 변수가 필요합니다 (서버의 테스트 데이터와 같은 값)')
  }
  return password
}

export default async function globalSetup() {
  const password = e2ePassword()
  const client = new pg.Client({
    host: process.env.DB_HOST ?? 'localhost',
    port: Number(process.env.DB_PORT ?? 5432),
    database: process.env.DB_NAME ?? 'admin',
    user: process.env.DB_USERNAME ?? 'admin',
    password: process.env.DB_PASSWORD,
  })
  await client.connect()
  try {
    await client.query('BEGIN')
    // e2e_*: 시나리오용 계정, e2eadm*: 관리자관리 E2E가 화면에서 등록한 계정 (아이디 규칙상 _를 쓸 수 없다)
    const ids = ["e2e\\_%"]
    for (const sql of [
      'DELETE FROM tb_admin_refresh_token WHERE admin_id IN (SELECT admin_id FROM tb_admin WHERE login_id LIKE $1)',
      'DELETE FROM tb_admin_role WHERE admin_id IN (SELECT admin_id FROM tb_admin WHERE login_id LIKE $1)',
      'DELETE FROM tb_admin_login_hist WHERE login_id LIKE $1',
      'DELETE FROM spring_session WHERE principal_name LIKE $1',
      'DELETE FROM tb_admin WHERE login_id LIKE $1',
    ]) {
      await client.query(sql, ids)
      await client.query(sql, ['e2eadm%'])
    }
    // E2E가 만든 코드관리 데이터 정리 (반복 실행 안전)
    await client.query("DELETE FROM tb_code WHERE group_cd LIKE 'E2E\\_%'")
    await client.query("DELETE FROM tb_code_group WHERE group_cd LIKE 'E2E\\_%'")
    // E2E가 화면에서 등록한 회원 정리 (상태 이력 → 회원)
    await client.query("DELETE FROM tb_user_status_hist WHERE user_id IN (SELECT user_id FROM tb_user WHERE login_id LIKE 'e2eusr%')")
    await client.query("DELETE FROM tb_user WHERE login_id LIKE 'e2eusr%'")
    // E2E가 만든 기업 정리 (사업자등록번호 800000000x, 소속 회원 없음)
    await client.query("DELETE FROM tb_company WHERE biz_reg_no LIKE '80000000%'")
    // E2E가 만든 역할 정리
    await client.query("DELETE FROM tb_role_permission WHERE role_id IN (SELECT role_id FROM tb_role WHERE role_cd LIKE 'E2E\\_%')")
    await client.query("DELETE FROM tb_admin_role WHERE role_id IN (SELECT role_id FROM tb_role WHERE role_cd LIKE 'E2E\\_%')")
    await client.query("DELETE FROM tb_role WHERE role_cd LIKE 'E2E\\_%'")
    // E2E가 만든 메뉴관리 데이터 정리 (권한·역할 매핑 → 깊은 메뉴부터)
    await client.query(`DELETE FROM tb_role_permission WHERE perm_id IN (
        SELECT p.perm_id FROM tb_permission p JOIN tb_menu m ON m.menu_id = p.menu_id WHERE m.menu_cd LIKE 'E2E\\_%')`)
    await client.query("DELETE FROM tb_permission WHERE menu_id IN (SELECT menu_id FROM tb_menu WHERE menu_cd LIKE 'E2E\\_%')")
    for (const depth of [3, 2, 1]) {
      await client.query("DELETE FROM tb_menu WHERE menu_cd LIKE 'E2E\\_%' AND depth = $1", [depth])
    }
    for (const front of FRONTS) {
      // 임시 비밀번호 계정 (비밀번호 변경 시나리오), 내 정보 수정 계정
      await createAdmin(client, `e2e_temp_${front}`, password, true)
      await createAdmin(client, `e2e_me_${front}`, password, false)
      await createOrderMenus(client, front)
      // 권한관리 시나리오용 역할 (권한 없음)
      await client.query("INSERT INTO tb_role (role_cd, role_nm) VALUES ($1::text, $2::text)",
        [`E2E_${front.toUpperCase()}_PRM`, `E2E 권한 ${front}`])
    }
    await client.query('COMMIT')
  } catch (e) {
    await client.query('ROLLBACK')
    throw e
  } finally {
    await client.end()
  }
}

async function createAdmin(client: pg.Client, loginId: string, password: string, pwdTemp: boolean) {
  await client.query(
    `INSERT INTO tb_admin (login_id, password, admin_nm, email, status_cd, pwd_temp_yn, pwd_changed_dt)
     VALUES ($1::text, crypt($2, gen_salt('bf', 10)), 'E2E관리자', $1::text || '@example.com', 'ACTIVE', $3, now())`,
    [loginId, password, pwdTemp ? 'Y' : 'N'],
  )
  await client.query(
    `INSERT INTO tb_admin_role (admin_id, role_id)
     SELECT a.admin_id, r.role_id FROM tb_admin a, tb_role r WHERE a.login_id = $1 AND r.role_cd = 'VIEWER'`,
    [loginId],
  )
}

/** 순서 변경 시나리오용 메뉴: 최상위 폴더 1개와 화면 메뉴 2개 ('E2E 가' → 'E2E 나' 순서) */
async function createOrderMenus(client: pg.Client, front: string) {
  const code = `E2E_${front.toUpperCase()}_ORD`
  const { rows } = await client.query(
    `INSERT INTO tb_menu (menu_cd, menu_nm, menu_type_cd, depth, sort_ord)
     VALUES ($1::text, $2::text, 'FOLDER', 1, 100) RETURNING menu_id`,
    [code, `E2E 순서 ${front}`],
  )
  for (const [i, nm] of ['가', '나'].entries()) {
    await client.query(
      `INSERT INTO tb_menu (parent_menu_id, menu_cd, menu_nm, menu_type_cd, menu_url, depth, sort_ord)
       VALUES ($1, $2::text, $3::text, 'PAGE', $4::text, 2, $5)`,
      [rows[0].menu_id, `${code}_${i + 1}`, `E2E ${nm} ${front}`, `/e2e-${front}-${i + 1}`, i + 1],
    )
  }
}
