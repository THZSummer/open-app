/**
 * 子应用配置列表
 * 新增子应用只需在此追加一条配置，主应用业务代码无需修改
 * 字段说明：
 *   name        子应用唯一标识，需与子应用导出的 name 一致
 *   entry       子应用访问地址（子应用独立启动后的本地地址）
 *   container   子应用挂载的 DOM 容器选择器
 *   activeRule  激活该子应用的路由规则
 */
const microApps = [
  {
    name: 'sub-app-b',
    entry: '//localhost:5173',
    container: '#sub-app-viewport',
    activeRule: '/sub-b'
  },
  {
    name: 'sub-app-c',
    entry: '//localhost:8081',
    container: '#sub-app-viewport',
    activeRule: '/sub-c'
  },
  {
    name: 'sub-app-d',
    entry: '//localhost:8082',
    container: '#sub-app-viewport',
    activeRule: '/sub-d'
  },
  {
    name: 'sub-app-e',
    entry: '//localhost:5174',
    container: '#sub-app-viewport',
    activeRule: '/sub-e'
  }
];

export default microApps;
