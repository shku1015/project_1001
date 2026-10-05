import { QueryClient, QueryClientProvider } from '@tanstack/react-query'
import { StrictMode } from 'react'
import { createRoot } from 'react-dom/client'
import { BrowserRouter } from 'react-router'
import '@tabler/core/dist/css/tabler.min.css'
import 'bootstrap'  // 드롭다운·모달 등 상호작용 (Tabler JS는 ESM에서 window.bootstrap을 노출하지 않는다)
import './index.css'
import { App } from './App'
import { AuthProvider } from './auth/AuthProvider'

const queryClient = new QueryClient({ defaultOptions: { queries: { retry: false } } })

createRoot(document.getElementById('root')!).render(
  <StrictMode>
    <QueryClientProvider client={queryClient}>
      <BrowserRouter basename="/react">
        <AuthProvider>
          <App />
        </AuthProvider>
      </BrowserRouter>
    </QueryClientProvider>
  </StrictMode>,
)
