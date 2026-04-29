import { defineConfig } from 'vite';
import react from '@vitejs/plugin-react';
import path from 'path';

export default defineConfig({
  plugins: [react()],
  resolve: {
    alias: {
      '@': path.resolve(__dirname, './src'),
    },
  },
  css: {
    less: {
      javascriptEnabled: true,
    },
  },
  server: {
    port: 5173,
    host: true,
    open: false,
    proxy: {
      '/service/open/v2': {
        target: 'http://localhost:18080/open-server',
        changeOrigin: true,
        rewrite: (path) => path,
      },
    },
  },
});