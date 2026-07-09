import React from 'react';
import { createRoot } from 'react-dom/client';
import { BrowserRouter } from 'react-router-dom';
import { renderWithQiankun, qiankunWindow } from 'vite-plugin-qiankun/dist/helper';
import App from './App';

// 子应用根实例引用
let root = null;

/**
 * 渲染子应用到挂载节点
 * @param {Object} props qiankun 注入的参数对象（可含 container 等）
 */
function render(props) {
  // 解构出挂载容器
  const { container } = props || {};
  // 被主应用加载时用 container 内的 #root，独立运行时用 document 的 #root
  const mountNode = container ? container.querySelector('#root') : document.getElementById('root');
  // 根据运行环境设置路由 basename，qiankun 环境下用 activeRule 作为 basename
  const basename = qiankunWindow.__POWERED_BY_QIANKUN__ ? '/sub-b' : '/';
  root = createRoot(mountNode);
  root.render(
    <BrowserRouter basename={basename}>
      <App />
    </BrowserRouter>
  );
}

// 通过 vite-plugin-qiankun 导出生命周期，并兼容独立运行
renderWithQiankun({
  /**
   * bootstrap：子应用首次激活时调用一次
   */
  bootstrap() {
    console.log('子应用 B bootstrap');
  },
  /**
   * mount：每次进入子应用时调用
   * @param {Object} props qiankun 注入的参数对象
   */
  mount(props) {
    render(props);
  },
  /**
   * unmount：每次离开子应用时调用
   * @param {Object} props qiankun 注入的参数对象
   */
  unmount(props) {
    if (root) {
      root.unmount();
      root = null;
    }
  },
  update() {}
});

// 独立运行（非 qiankun 环境）时直接渲染
if (!qiankunWindow.__POWERED_BY_QIANKUN__) {
  render({});
}
