import { create } from 'zustand'
import { persist } from 'zustand/middleware'

interface AuthState {
  token: string | null
  userId: string | null
  tenantId: string | null
  role: string | null
  isAuthenticated: boolean
  setAuth: (token: string, userId: string, tenantId: string, role: string) => void
  clearAuth: () => void
}

export const useAuthStore = create<AuthState>()(
  persist(
    (set) => ({
      token: null,
      userId: null,
      tenantId: null,
      role: null,
      isAuthenticated: false,
      setAuth: (token, userId, tenantId, role) =>
        set({ token, userId, tenantId, role, isAuthenticated: true }),
      clearAuth: () =>
        set({ token: null, userId: null, tenantId: null, role: null, isAuthenticated: false }),
    }),
    { name: 'devdocsai-auth' }
  )
)
