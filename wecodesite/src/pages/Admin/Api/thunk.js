/**
 * ========================================
 * API管理 - 接口定义文件
 * ========================================
 *
 * 模块说明：平台全部API接口的增删改查操作
 */

import { API_CONFIG, buildApiUrl, fetchApi } from '../../../configs/web.config';

/**
 * 查询API列表
 *
 * @param {Object} queryParams - 查询参数 { categoryId?, status?, keyword? }
 * @returns {Promise<Object>} 响应结果 { code, data, page }
 */
export const fetchApiList = async (queryParams = {}) => {
  try {
    const result = await fetchApi(API_CONFIG.APIS.LIST, { method: 'GET', params: queryParams });
    return result || {};
  } catch (err) {
    return {};
  }
};

/**
 * 查询API详情
 *
 * @param {string} apiId - API标识
 * @returns {Promise<Object>} 响应结果 { code, data }
 */
export const fetchApiDetail = async (apiId) => {
  try {
    const result = await fetchApi(buildApiUrl(API_CONFIG.APIS.DETAIL, { id: apiId }));
    return result || {};
  } catch (err) {
    return {};
  }
};

/**
 * 新增API
 *
 * @param {Object} apiData - API配置信息
 * @returns {Promise<Object>} 响应结果 { code, data }
 */
export const createApi = async (apiData) => {
  try {
    const result = await fetchApi(API_CONFIG.APIS.CREATE, {
      method: 'POST',
      body: JSON.stringify(apiData)
    });
    return result || {};
  } catch (err) {
    return {};
  }
};

/**
 * 修改API信息
 *
 * @param {string} apiId - API标识
 * @param {Object} apiData - 更新后的API配置信息
 * @returns {Promise<Object>} 响应结果 { code, data }
 */
export const updateApi = async (apiId, apiData) => {
  try {
    const result = await fetchApi(buildApiUrl(API_CONFIG.APIS.UPDATE, { id: apiId }), {
      method: 'PUT',
      body: JSON.stringify(apiData)
    });
    return result || {};
  } catch (err) {
    return {};
  }
};

/**
 * 删除API
 *
 * @param {string} apiId - API标识
 * @returns {Promise<Object>} 响应结果 { code, data }
 */
export const deleteApi = async (apiId) => {
  try {
    const result = await fetchApi(buildApiUrl(API_CONFIG.APIS.DELETE, { id: apiId }), {
      method: 'DELETE'
    });
    return result || {};
  } catch (err) {
    return {};
  }
};

/**
 * 撤回API（退回草稿状态）
 *
 * @param {string} apiId - API标识
 * @returns {Promise<Object>} 响应结果 { code, data }
 */
export const withdrawApi = async (apiId) => {
  try {
    const result = await fetchApi(buildApiUrl(API_CONFIG.APIS.WITHDRAW, { id: apiId }), {
      method: 'POST'
    });
    return result || {};
  } catch (err) {
    return {};
  }
};
