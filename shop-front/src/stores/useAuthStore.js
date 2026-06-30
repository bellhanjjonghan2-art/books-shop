import { create } from 'zustand'
import { persist } from 'zustand/middleware'
import { immer } from 'zustand/middleware/immer'

export const useAuthStore = create()(
  persist(
    immer((set) => ({
      accessToken: null,
      user: null,
      isAuthenticated: false,

      setAuth: ({ accessToken, user }) =>
        set((state) => {
          state.accessToken = accessToken
          state.user = user
          state.isAuthenticated = true
        }),

      logout: () =>
        set((state) => {
          state.accessToken = null
          state.user = null
          state.isAuthenticated = false
        }),
    })),
    { name: 'auth-storage' },
  ),
)
