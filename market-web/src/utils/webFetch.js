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
 * @param {string} url - API 完整路径
 * @param {Object} options - 请求配置，包含 method、params、body、headers 等
 * @returns {Promise<Object>} API 响应数据
 */
export const fetchApi = async (url, options = {}) => {
  const { params, ...fetchOptions } = options;
  if (params) {
    const queryString = new URLSearchParams(params).toString();
    url = queryString ? `${url}?${queryString}` : url;
  }
  const response = await fetch(url, {
    ...fetchOptions,
    credentials: 'include',  // 确保请求携带 Cookie
    headers: {
      'Content-Type': 'application/json',
      ...fetchOptions.headers,
    },
  });
  return response.json();
};
