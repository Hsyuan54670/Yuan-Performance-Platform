import { create } from "zustand";
import { createJSONStorage, persist } from "zustand/middleware";

interface AppState {
  collapsed: boolean;
  activeTaskId: number;
  setCollapsed: (value: boolean) => void;
  setActiveTaskId: (taskId: number) => void;
}

export const useAppStore = create<AppState>()(
  persist(
    (set) => ({
      collapsed: false,
      activeTaskId: 0,
      setCollapsed: (value) => set({ collapsed: value }),
      setActiveTaskId: (taskId) => set({ activeTaskId: taskId })
    }),
    {
      name: "yuan-app-store",
      storage: createJSONStorage(() => localStorage),
      partialize: (state) => ({ collapsed: state.collapsed, activeTaskId: state.activeTaskId })
    }
  )
);
