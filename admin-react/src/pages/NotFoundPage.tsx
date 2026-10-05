import { Link } from 'react-router'
import { Layout } from '../components/Layout'

/** SCR-ERR-404 (① React). 아직 만들지 않은 메뉴 화면도 여기로 온다 */
export function NotFoundPage() {
  return (
    <Layout title="페이지 없음">
      <div className="empty">
        <p className="empty-title">요청한 페이지를 찾을 수 없습니다</p>
        <div className="empty-action"><Link to="/" className="btn btn-primary">홈으로</Link></div>
      </div>
    </Layout>
  )
}
