/// <reference types="vitest/config" />
import react from '@vitejs/plugin-react'
import { defineConfig } from 'vite'

// 배포 시 서버의 /react 경로에서 제공한다 (docs/08-architecture.md 1.1).
export default defineConfig({
  base: '/react/',
  plugins: [react()],
  server: {
    // 개발 중에는 Vite 개발 서버에서 API를 서버(8080)로 넘긴다.
    proxy: {
      '/api': 'http://localhost:8080',
    },
  },
  test: {
    environment: 'jsdom',
    setupFiles: ['./src/test/setup.ts'],
  },
})
