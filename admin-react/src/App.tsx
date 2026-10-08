import { Route, Routes } from 'react-router'
import { RequireAuth } from './auth/RequireAuth'
import { HomePage } from './pages/HomePage'
import { LoginPage } from './pages/LoginPage'
import { CodePage } from './pages/CodePage'
import { MePage } from './pages/MePage'
import { MenuPage } from './pages/MenuPage'
import { NotFoundPage } from './pages/NotFoundPage'
import { PasswordPage } from './pages/PasswordPage'
import { PermissionPage } from './pages/PermissionPage'
import { RoleDetailPage } from './pages/RoleDetailPage'
import { RoleFormPage } from './pages/RoleFormPage'
import { RoleListPage } from './pages/RoleListPage'

// ① React 화면 경로 (docs/05-ia-screens.md 5절). 실제 주소는 /react 접두어가 붙는다 (main.tsx basename)
export function App() {
  return (
    <Routes>
      <Route path="/login" element={<LoginPage />} />
      <Route path="/" element={<RequireAuth><HomePage /></RequireAuth>} />
      <Route path="/me" element={<RequireAuth><MePage /></RequireAuth>} />
      <Route path="/menus" element={<RequireAuth><MenuPage /></RequireAuth>} />
      <Route path="/permissions" element={<RequireAuth><PermissionPage /></RequireAuth>} />
      <Route path="/roles" element={<RequireAuth><RoleListPage /></RequireAuth>} />
      <Route path="/roles/new" element={<RequireAuth><RoleFormPage /></RequireAuth>} />
      <Route path="/roles/:roleId" element={<RequireAuth><RoleDetailPage /></RequireAuth>} />
      <Route path="/roles/:roleId/edit" element={<RequireAuth><RoleFormPage /></RequireAuth>} />
      <Route path="/codes" element={<RequireAuth><CodePage /></RequireAuth>} />
      <Route path="/password" element={<RequireAuth allowTempPassword><PasswordPage /></RequireAuth>} />
      <Route path="*" element={<RequireAuth><NotFoundPage /></RequireAuth>} />
    </Routes>
  )
}
