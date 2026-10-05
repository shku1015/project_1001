import '@testing-library/jest-dom/vitest'
import { cleanup } from '@testing-library/react'
import { afterEach } from 'vitest'

// 테스트마다 그린 화면을 지운다 (vitest globals를 쓰지 않으므로 직접 등록)
afterEach(() => cleanup())
