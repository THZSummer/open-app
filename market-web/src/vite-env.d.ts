/// <reference types="vite/client" />

interface ImportMetaEnv {
  readonly VITE_API_BASE_URL: string;
}

interface ImportMeta {
  readonly env: ImportMetaEnv;
}

// antd locale type declaration
declare module 'antd/es/locale/zh_CN' {
  import type { Locale } from 'antd/es/locale';
  const locale: Locale;
  export default locale;
}
