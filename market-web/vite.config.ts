import { defineConfig } from 'vite';
import react from '@vitejs/plugin-react';
import path from 'path';

// https://vitejs.dev/config/
export default defineConfig({
  base: '/market-web/',
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
          '@primary-color': '#2b5ff5',
          '@primary-1': '#eef2ff',
          '@success-color': '#00b578',
          '@error-color': '#f54a45',
          '@warning-color': '#ff9f00',
          '@text-color': '#1d2129',
          '@text-color-secondary': '#4e5969',
          '@text-color-help': '#86909c',
          '@border-color-base': '#e5e6eb',
          '@background-color': '#f2f3f5',
          '@border-radius-base': '6px',
        },
      },
    },
  },
  server: {
    port: 13000,
    host: true,
    open: false,
    proxy: {
      '/market-web/api/v1': {
        target: 'http://localhost:18080/market-server/api/v1',
        changeOrigin: true,
        rewrite: (path) => path.replace(/^\/market-web\/api\/v1/, ''),
      },
    },
  },
  build: {
    outDir: 'dist',
    sourcemap: true,
  },
});
