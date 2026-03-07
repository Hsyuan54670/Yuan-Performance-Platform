import { create } from "zustand";

interface AppState {
  collapsed: boolean;
  activeTaskId: number;
  setCollapsed: (value: boolean) => void;
  setActiveTaskId: (taskId: number) => void;
}

export const useAppStore = create<AppState>((set) => ({
  collapsed: false,
  activeTaskId: 3001,
  setCollapsed: (value) => set({ collapsed: value }),
  setActiveTaskId: (taskId) => set({ activeTaskId: taskId })
}));
