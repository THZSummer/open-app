import { defineConfig } from 'vite';
import vue from '@vitejs/plugin-vue';
import qiankun from 'vite-plugin-qiankun';

// Vite + Vue 子应用接入 qiankun 配置
export default defineConfig({
  plugins: [
    // 处理 .vue 单文件组件
    vue(),
    // 注入子应用名称，qiankun 据此识别并接管子应用生命周期
    qiankun('sub-app-e', { useDevMode: true })
  ],
  server: {
    // 使用自定义域名，便于跨子应用 cookie / 域名隔离
    host: 'localhost.uat.com',
    port: 5175,
    // 允许主应用跨域拉取子应用资源
    cors: true,
    // 固定资源 origin，避免被主应用加载时资源路径 404
    origin: 'http://localhost.uat.com:5175'
  }
});
