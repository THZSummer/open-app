import { defineConfig } from 'vite';
import qiankun from 'vite-plugin-qiankun';

// Vite 子应用接入 qiankun 配置
// 说明：不使用 @vitejs/plugin-react，改用 esbuild 内置的 JSX 转换（automatic runtime），
//       从根源避免 react-refresh preamble 与 qiankun 沙箱的 window 不一致问题
export default defineConfig({
  plugins: [
    // 注入子应用名称，qiankun 据此识别并接管子应用生命周期
    qiankun('sub-app-b', { useDevMode: true })
  ],
  // 使用 esbuild 的 automatic JSX runtime 转换 JSX（React 18+），
  // 不注入 react-refresh，无 preamble、无 HMR 检测代码
  esbuild: {
    jsx: 'automatic'
  },
  server: {
    // 使用自定义域名，便于跨子应用 cookie / 域名隔离
    host: 'localhost.uat.com',
    port: 5174,
    // 允许主应用跨域拉取子应用资源
    cors: true,
    origin: 'http://localhost.uat.com:5174'
  }
});
