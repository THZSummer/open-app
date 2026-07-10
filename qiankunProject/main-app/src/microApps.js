/**
 * 子应用配置列表
 * 新增子应用只需在此追加一条配置，主应用业务代码无需修改
 *
 * 字段说明：
 *   name        子应用唯一标识，需与子应用导出的 name 一致
 *   entry       子应用访问地址（子应用独立启动后的本地地址）
 *   container   子应用挂载的 DOM 容器选择器
 *   activeRule  激活该子应用的路由规则（函数形式，从 location.hash 中匹配）
 *
 * 主应用使用 HashRouter，qiankun 默认匹配 location.pathname（始终为 /），
 * 因此 activeRule 需改为函数，从 location.hash 中匹配
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
    activeRule: hashRule('/qiankun/sub-b')
  },
  {
    // 子应用 C（React + Webpack）
    name: 'sub-app-c',
    entry: '//localhost.uat.com:8082',
    container: '#sub-app-viewport',
    activeRule: hashRule('/qiankun/sub-c')
  },
  {
    // 子应用 D（Vue3 + Webpack）
    name: 'sub-app-d',
    entry: '//localhost.uat.com:8083',
    container: '#sub-app-viewport',
    activeRule: hashRule('/qiankun/sub-d')
  },
  {
    // 子应用 E（Vue3 + Vite）
    name: 'sub-app-e',
    entry: '//localhost.uat.com:5175',
    container: '#sub-app-viewport',
    activeRule: hashRule('/qiankun/sub-e')
  }
];

export default microApps;
