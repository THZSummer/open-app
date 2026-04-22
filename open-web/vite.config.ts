import { defineConfig } from 'vite';
import react from '@vitejs/plugin-react';
import path from 'path';

// https://vitejs.dev/config/
export default defineConfig({
  base: '/open-web/',
  plugins: [react()],
  resolve: {
    alias: {
      '@': path.resolve(__dirname, './src'),
    },
  },
  css: {
    preprocessorOptions: {
      less: {
        javascriptEnabled: true,
        modifyVars: {
          '@primary-color': '#1890ff',
          '@link-color': '#1890ff',
          '@border-radius-base': '4px',
        },
      },
    },
  },
  server: {
    port: 13000,
    host: true,
    open: false,
    proxy: {
      '/api/v1': {
        target: 'http://localhost:18080/open-server',
        changeOrigin: true,
        rewrite: (path) => path,
      },
    },
  },
  build: {
    outDir: 'dist',
    sourcemap: true,
  },
});
