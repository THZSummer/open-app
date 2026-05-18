/**
 * ========================================
 * 连接器编辑页模块 - 常量配置
 * ========================================
 *
 * 定义连接器编辑页面的配置信息、状态映射等
 */

/**
 * 页面配置信息
 * 定义连接器编辑页面的标题等基本信息
 */
export const editorPageInfo = {
  createTitle: '新建连接器',
  editTitle: '编辑连接器',
};

/**
 * 连接器状态映射
 * 用于表单中的状态显示
 */
export const CONNECTOR_STATUS_MAP = {
  0: { text: '禁用', color: 'default' },
  1: { text: '启用', color: 'success' },
};

/**
 * 触发类型映射
 */
export const TRIGGER_TYPE_MAP = {
  webhook: { text: 'Webhook', color: 'blue' },
  api: { text: 'API轮询', color: 'green' },
  schedule: { text: '定时触发', color: 'purple' },
};

/**
 * 执行动作HTTP方法映射
 */
export const HTTP_METHOD_MAP = {
  GET: { text: 'GET', color: 'green' },
  POST: { text: 'POST', color: 'blue' },
  PUT: { text: 'PUT', color: 'orange' },
  DELETE: { text: 'DELETE', color: 'red' },
};
