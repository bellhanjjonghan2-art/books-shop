import { create } from 'zustand'
import { persist } from 'zustand/middleware'
import { immer } from 'zustand/middleware/immer'

export const useHealthStore = create()(
  persist(
    immer((set) => ({
      lastStatus: null,
      setLastStatus: (status) =>
        set((state) => {
          state.lastStatus = status
        }),
    })),
    { name: 'health-store' },
  ),
)
