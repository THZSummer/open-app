/**
 * qiankun 微前端子应用注册配置
 *
 * 后续新增子应用只需在此数组中添加一条配置即可，无需修改 App.jsx / Layout / Sidebar
 *
 * 字段说明：
 * - name: 子应用名称，需与子应用 entry 中导出的 library 名称一致
 * - entry: 子应用开发服务器地址（含协议和端口）
 * - container: 子应用挂载的 DOM 容器选择器
 * - activeRule: 子应用激活的路由规则，统一以 /qiankun/ 为前缀
 *   主应用使用 HashRouter，qiankun 默认匹配 location.pathname（始终为 /），
 *   因此用函数从 location.hash 中匹配
 */

/**
 * 生成基于 hash 的 activeRule 匹配函数
 * @param {string} rule - 路由规则，如 '/qiankun/sub-b'
 * @returns {function} 返回匹配函数，供 qiankun 判断子应用是否激活
 */
function hashRule(rule) {
  return (location) => location.hash.startsWith('#' + rule);
}

const microApps = [
  {
    // 子应用 B（React + Vite）
    name: 'sub-app-b',
    entry: '//localhost.uat.com:5174',
    container: '#sub-app-viewport',
    activeRule: hashRule('/qiankun/sub-b'),
    // 供 Sidebar 菜单使用的高亮 key（字符串形式）
    menuKey: 'qiankun/sub-b',
  },
  {
    // 子应用 C（React + Webpack）
    name: 'sub-app-c',
    entry: '//localhost.uat.com:8082',
    container: '#sub-app-viewport',
    activeRule: hashRule('/qiankun/sub-c'),
    menuKey: 'qiankun/sub-c',
  },
  {
    // 子应用 D（Vue3 + Webpack）
    name: 'sub-app-d',
    entry: '//localhost.uat.com:8083',
    container: '#sub-app-viewport',
    activeRule: hashRule('/qiankun/sub-d'),
    menuKey: 'qiankun/sub-d',
  },
  {
    // 子应用 E（Vue3 + Vite）
    name: 'sub-app-e',
    entry: '//localhost.uat.com:5175',
    container: '#sub-app-viewport',
    activeRule: hashRule('/qiankun/sub-e'),
    menuKey: 'qiankun/sub-e',
  },
];

export default microApps;
