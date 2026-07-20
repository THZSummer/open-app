import { defineConfig } from 'vite';
import react from '@vitejs/plugin-react';
import path from 'path';

// https://vitejs.dev/config/
export default defineConfig({
  base: '/market-web/',
  plugins: [
    react({
      // 允许 .js 文件使用 JSX 语法
      include: /\.(js|jsx|ts|tsx)$/,
      babel: {
        babelrc: false,
        configFile: false,
        plugins: [
          ['@babel/plugin-transform-react-jsx', {
            runtime: 'automatic'
          }]
        ]
      }
    })
  ],
  optimizeDeps: {
    esbuildOptions: {
      loader: {
        '.js': 'jsx'
      }
    }
  },
  resolve: {
    alias: {
      '@': path.resolve(__dirname, './src'),
    },
  },
  css: {
    modules: {
      localsConvention: 'camelCase',
      exportLocalsConvention: 'camelCaseOnly',
      namedExport: false,
      generateScopedName: '[local]_[hash:base64:6]',
    },
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
      '/market-web/service': {
        target: 'http://localhost:18083/market-server',
        changeOrigin: true,
        rewrite: (path) => path.replace(/^\/market-web/, ''),
      },
      '/ability-files': {
        target: 'http://localhost:18083',
        changeOrigin: true,
        rewrite: (path) => path.replace(/^\/ability-files/, '/market-server/ability-files'),
      },
    },
  },
  build: {
    outDir: 'dist',
    sourcemap: true,
  },
});
