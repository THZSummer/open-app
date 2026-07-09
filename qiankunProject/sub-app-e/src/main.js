import { createApp } from 'vue';
import { renderWithQiankun, qiankunWindow } from 'vite-plugin-qiankun/dist/helper';
import App from './App.vue';
import createAppRouter from './router/index';

// 子应用根实例引用
let app = null;

/**
 * 渲染子应用到挂载节点
 * @param {Object} props qiankun 注入的参数对象（可含 container 等）
 */
function render(props) {
  // 解构出挂载容器
  const { container } = props || {};
  // 被主应用加载时用 container 内的 #app，独立运行时用 document 的 #app
  const mountNode = container ? container.querySelector('#app') : document.getElementById('app');
  // 根据运行环境设置路由 basename，qiankun 环境下用 activeRule 作为 basename
  const basename = qiankunWindow.__POWERED_BY_QIANKUN__ ? '/sub-e' : '/';
  // 创建路由实例
  const router = createAppRouter(basename);
  app = createApp(App);
  app.use(router);
  app.mount(mountNode);
}

// 通过 vite-plugin-qiankun 导出生命周期，并兼容独立运行
renderWithQiankun({
  /**
   * bootstrap：子应用首次激活时调用一次
   */
  bootstrap() {
    console.log('子应用 E bootstrap');
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
    if (app) {
      app.unmount();
      app = null;
    }
  },
  update() {}
});

// 独立运行（非 qiankun 环境）时直接渲染
if (!qiankunWindow.__POWERED_BY_QIANKUN__) {
  render({});
}
