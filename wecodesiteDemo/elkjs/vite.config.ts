import { defineConfig } from 'vite';
import react from '@vitejs/plugin-react';

// Vite 配置文件
export default defineConfig({
  plugins: [react()],
  server: {
    port: 5174,
    open: true
  }
});
