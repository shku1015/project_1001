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
    const ids = ["e2e\\_%"]
    for (const sql of [
      'DELETE FROM tb_admin_refresh_token WHERE admin_id IN (SELECT admin_id FROM tb_admin WHERE login_id LIKE $1)',
      'DELETE FROM tb_admin_role WHERE admin_id IN (SELECT admin_id FROM tb_admin WHERE login_id LIKE $1)',
      'DELETE FROM tb_admin_login_hist WHERE login_id LIKE $1',
      'DELETE FROM spring_session WHERE principal_name LIKE $1',
      'DELETE FROM tb_admin WHERE login_id LIKE $1',
    ]) {
      await client.query(sql, ids)
    }
    for (const front of FRONTS) {
      // 임시 비밀번호 계정 (비밀번호 변경 시나리오), 내 정보 수정 계정
      await createAdmin(client, `e2e_temp_${front}`, password, true)
      await createAdmin(client, `e2e_me_${front}`, password, false)
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
