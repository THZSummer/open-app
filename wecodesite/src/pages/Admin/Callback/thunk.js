/**
 * Admin模块 - 回调管理相关API
 * 用于管理员管理平台全部回调接口的定义、发布和下线操作
 */
import { API_CONFIG, buildApiUrl, fetchApi } from '../../../configs/web.config';

/**
 * 获取回调列表（管理员视角）
 * @param {Object} params - 查询参数，包含 categoryId（分类ID）、status（状态）、keyword（关键词）
 * @returns {Promise<Object>} 包含 code、messageZh、data、page 的响应对象
 */
export const fetchCallbackList = async (params = {}) => {
  try {
    const result = await fetchApi(API_CONFIG.CALLBACKS.LIST, { method: 'GET', params });
    return result || {};
  } catch (err) {
    return {};
  }
};

/**
 * 获取回调详情
 * @param {string} id - 回调ID
 * @returns {Promise<Object>} 包含 code、messageZh、data 的响应对象
 */
export const fetchCallbackDetail = async (id) => {
  try {
    const result = await fetchApi(buildApiUrl(API_CONFIG.CALLBACKS.DETAIL, { id }));
    return result || {};
  } catch (err) {
    return {};
  }
};

/**
 * 创建新回调（管理员操作）
 * @param {Object} data - 回调配置数据
 * @returns {Promise<Object>} 包含 code、messageZh、data 的响应对象
 */
export const createCallback = async (data) => {
  try {
    const result = await fetchApi(API_CONFIG.CALLBACKS.CREATE, { method: 'POST', body: JSON.stringify(data) });
    return result || {};
  } catch (err) {
    return {};
  }
};

/**
 * 更新回调信息（管理员操作）
 * @param {string} id - 回调ID
 * @param {Object} data - 更新后的回调配置数据
 * @returns {Promise<Object>} 包含 code、messageZh、data 的响应对象
 */
export const updateCallback = async (id, data) => {
  try {
    const result = await fetchApi(buildApiUrl(API_CONFIG.CALLBACKS.UPDATE, { id }), { method: 'PUT', body: JSON.stringify(data) });
    return result || {};
  } catch (err) {
    return {};
  }
};

/**
 * 删除回调（管理员操作）
 * @param {string} id - 回调ID
 * @returns {Promise<Object>} 包含 code、messageZh、data 的响应对象
 */
export const deleteCallback = async (id) => {
  try {
    const result = await fetchApi(buildApiUrl(API_CONFIG.CALLBACKS.DELETE, { id }), { method: 'DELETE' });
    return result || {};
  } catch (err) {
    return {};
  }
};

/**
 * 撤回回调审核（重新变为草稿状态）
 * @param {string} id - 回调ID
 * @returns {Promise<Object>} 包含 code、messageZh、data 的响应对象
 */
export const withdrawCallback = async (id) => {
  try {
    const result = await fetchApi(buildApiUrl(API_CONFIG.CALLBACKS.WITHDRAW, { id }), { method: 'POST' });
    return result || {};
  } catch (err) {
    return {};
  }
};
