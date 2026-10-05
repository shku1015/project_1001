import { render, screen } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { MemoryRouter, Route, Routes } from 'react-router'
import { describe, expect, it, vi } from 'vitest'
import { ApiError } from '../api/client'
import { AuthContext, type AuthContextValue } from '../auth/AuthContext'
import { LoginPage } from './LoginPage'

function renderLogin(login: AuthContextValue['login']) {
  const value: AuthContextValue = { state: { status: 'anonymous' }, login, logout: vi.fn(), reloadMe: vi.fn() }
  render(
    <AuthContext.Provider value={value}>
      <MemoryRouter initialEntries={['/login']}>
        <Routes>
          <Route path="/login" element={<LoginPage />} />
          <Route path="/" element={<p>홈 화면</p>} />
          <Route path="/password" element={<p>비밀번호 변경 화면</p>} />
        </Routes>
      </MemoryRouter>
    </AuthContext.Provider>,
  )
}

describe('LoginPage', () => {
  it('로그인에 실패하면 서버 메시지를 보여 준다', async () => {
    renderLogin(vi.fn().mockRejectedValue(new ApiError(401, 'LOGIN_FAILED', '아이디 또는 비밀번호가 올바르지 않습니다.')))
    await userEvent.type(screen.getByLabelText('아이디'), 't_system')
    await userEvent.type(screen.getByLabelText('비밀번호'), 'wrong')
    await userEvent.click(screen.getByRole('button', { name: '로그인' }))
    expect(await screen.findByRole('alert')).toHaveTextContent('아이디 또는 비밀번호가 올바르지 않습니다.')
  })

  it('임시 비밀번호면 비밀번호 변경 화면으로 간다', async () => {
    const login = vi.fn().mockResolvedValue({ accessToken: 't', expiresIn: 1800, pwdChangeRequired: true })
    renderLogin(login)
    await userEvent.type(screen.getByLabelText('아이디'), 'admin')
    await userEvent.type(screen.getByLabelText('비밀번호'), 'temp')
    await userEvent.click(screen.getByRole('button', { name: '로그인' }))
    expect(await screen.findByText('비밀번호 변경 화면')).toBeInTheDocument()
    expect(login).toHaveBeenCalledWith('admin', 'temp')
  })
})
