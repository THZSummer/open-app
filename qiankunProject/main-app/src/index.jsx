import { registerMicroApps, start } from 'qiankun';
import { createRoot } from 'react-dom/client';
import React from 'react';
import App from './App';
import microApps from './microApps';

/**
 * 注册所有子应用
 * qiankun 会根据 activeRule 自动加载对应子应用并渲染到 container
 */
registerMicroApps(microApps);

/**
 * 启动 qiankun
 * prefetch  开启子应用预加载，提升切换速度
 * sandbox   开启样式隔离，避免主子应用样式互相污染
 */
start({
  prefetch: true,
  sandbox: { experimentalStyleIsolation: true }
});

// 渲染主应用根组件
const root = createRoot(document.getElementById('root'));
root.render(<App />);
