import { create } from 'zustand';
import { persist } from 'zustand/middleware';

/**
 * 用户信息
 */
export interface UserInfo {
  userId: string;
  userName: string;
  avatar?: string;
  roles?: string[];
}

/**
 * 应用信息
 */
export interface AppInfo {
  appId: string;
  appName: string;
  appType?: string;
}

/**
 * 全局状态
 */
export interface GlobalState {
  // 用户信息
  userInfo: UserInfo | null;
  setUserInfo: (userInfo: UserInfo | null) => void;

  // 应用信息
  appInfo: AppInfo | null;
  setAppInfo: (appInfo: AppInfo | null) => void;

  // Token
  token: string | null;
  setToken: (token: string | null) => void;

  // 侧边栏折叠状态
  collapsed: boolean;
  setCollapsed: (collapsed: boolean) => void;

  // 主题
  theme: 'light' | 'dark';
  setTheme: (theme: 'light' | 'dark') => void;

  // 登出
  logout: () => void;
}

/**
 * 全局状态管理
 */
const useGlobalStore = create<GlobalState>()(
  persist(
    (set) => ({
      // 用户信息
      userInfo: null,
      setUserInfo: (userInfo) => set({ userInfo }),

      // 应用信息
      appInfo: null,
      setAppInfo: (appInfo) => set({ appInfo }),

      // Token
      token: null,
      setToken: (token) => set({ token }),

      // 侧边栏折叠
      collapsed: false,
      setCollapsed: (collapsed) => set({ collapsed }),

      // 主题
      theme: 'light',
      setTheme: (theme) => set({ theme }),

      // 登出
      logout: () => {
        set({
          userInfo: null,
          appInfo: null,
          token: null,
        });
        localStorage.removeItem('token');
        localStorage.removeItem('appId');
      },
    }),
    {
      name: 'global-storage',
      partialize: (state) => ({
        userInfo: state.userInfo,
        appInfo: state.appInfo,
        token: state.token,
        theme: state.theme,
      }),
    }
  )
);

export default useGlobalStore;
