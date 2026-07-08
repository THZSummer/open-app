// 必须在最前面引入，用于动态设置 publicPath
import './public-path';
import { createApp } from 'vue';
import App from './App.vue';

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
  app = createApp(App);
  app.mount(mountNode);
}

// 独立运行（非 qiankun 环境）时直接渲染
if (!window.__POWERED_BY_QIANKUN__) {
  render({});
}

/**
 * bootstrap：子应用首次激活时调用，只执行一次
 */
export async function bootstrap() {
  console.log('子应用 D bootstrap');
}

/**
 * mount：每次进入子应用时调用
 * @param {Object} props qiankun 注入的参数对象
 */
export async function mount(props) {
  render(props);
}

/**
 * unmount：每次离开子应用时调用
 * @param {Object} props qiankun 注入的参数对象
 */
export async function unmount(props) {
  if (app) {
    app.unmount();
    app = null;
  }
}
