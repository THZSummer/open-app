/**
 * Web API 配置文件
 * 统一管理所有接口配置
 */

/**
 * API 配置对象
 * 定义所有模块的 API 端点
 */
export const API_CONFIG = {
  BASE_URL: '/market-web/api/v1',

  /**
   * 分类管理 API 配置
   */
  CLASSIFY: {
    LIST: '/lookup/classify/list',
    DETAIL: '/lookup/classify/{classifyId}',
    CREATE: '/lookup/classify',
    UPDATE: '/lookup/classify/{classifyId}',
    DELETE: '/lookup/classify/{classifyId}',
  },

  /**
   * LookUp 项管理 API 配置
   */
  ITEM: {
    LIST: '/lookup/classify/{classifyId}/items',
    DETAIL: '/lookup/items/{itemId}',
    CREATE: '/lookup/classify/{classifyId}/items',
    UPDATE: '/lookup/items/{itemId}',
    DELETE: '/lookup/items/{itemId}',
  },

  /**
   * 导入导出 API 配置
   */
  IMPORT_EXPORT: {
    TEMPLATE: '/lookup/import/template',
    IMPORT: '/lookup/import',
    IMPORT_ASYNC: '/lookup/import/async',
    EXPORT: '/lookup/export',
    EXPORT_ASYNC: '/lookup/export/async',
    TASK_RESULT: '/lookup/tasks/{taskId}/download',
  },

  /**
   * 任务管理 API 配置
   */
  TASK: {
    LIST: '/lookup/tasks',
    DETAIL: '/lookup/tasks/{taskId}',
    CREATE: '/lookup/tasks',
    UPDATE_STATUS: '/lookup/tasks/{taskId}/status',
  },
};

/**
 * 动态替换 URL 中的占位符
 * @param {string} template - URL 模板，包含 {param} 格式的占位符
 * @param {Object} params - 替换参数对象
 * @returns {string} 替换后的 URL
 */
export const buildApiUrl = (template, params = {}) => {
  let url = template;
  Object.keys(params).forEach((key) => {
    url = url.replace(`{${key}}`, params[key]);
  });
  return url;
};

/**
 * 统一 API 请求封装
 * @param {string} url - API 端点（相对路径）
 * @param {Object} options - 请求配置，包含 method、params、body、headers 等
 * @returns {Promise<Object>} API 响应数据
 */
export const fetchApi = async (url, options = {}) => {
  const { params, ...fetchOptions } = options;
  let fullUrl = `${API_CONFIG.BASE_URL}${url}`;
  
  // 处理查询参数
  if (params) {
    const queryString = new URLSearchParams(params).toString();
    fullUrl = queryString ? `${fullUrl}?${queryString}` : fullUrl;
  }
  
  // 从 localStorage 获取 Token
  const token = localStorage.getItem('token');
  const appId = localStorage.getItem('appId');

  const response = await fetch(fullUrl, {
    ...fetchOptions,
    credentials: 'include', // 确保请求携带 Cookie
    headers: {
      'Content-Type': 'application/json',
      ...(token && { Authorization: `Bearer ${token}` }),
      ...(appId && { 'X-App-Id': appId }),
      ...fetchOptions.headers,
    },
  });
  
  return response.json();
};
