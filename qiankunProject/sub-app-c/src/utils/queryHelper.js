/**
 * 获取当前 URL 上的所有 query 参数
 * @returns {Object} query 参数对象
 */
function getCurrentQueryParams() {
  const params = new URLSearchParams(window.location.search);
  const result = {};
  params.forEach((value, key) => {
    result[key] = value;
  });
  return result;
}

/**
 * 合并主应用传入的 query 参数和子页面跳转所需的参数
 * @param {Object} params 子页面自身需要携带的参数对象
 * @returns {Object} 合并后的参数对象
 */
function mergeQueryParams(params) {
  // 获取主应用传入的参数
  const mainParams = getCurrentQueryParams();
  // 合并参数，子页面参数优先级更高
  return { ...mainParams, ...params };
}

/**
 * 将参数对象转换为 query string
 * @param {Object} params 参数对象
 * @returns {string} query string，如 ?key1=value1&key2=value2
 */
function toQueryString(params) {
  const searchParams = new URLSearchParams(params);
  const str = searchParams.toString();
  return str ? `?${str}` : '';
}

export { getCurrentQueryParams, mergeQueryParams, toQueryString };
