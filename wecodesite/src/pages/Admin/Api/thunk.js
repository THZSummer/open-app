/**
 * Admin模块 - API管理相关API
 * 用于管理员管理平台全部API接口的定义、发布和下线操作
 */
import { API_CONFIG, buildApiUrl, fetchApi } from '../../../configs/web.config';

/**
 * 获取API列表（管理员视角）
 * @param {Object} params - 查询参数，包含 categoryId（分类ID）、status（状态）、keyword（关键词）
 * @returns {Promise<Object>} 包含 code、messageZh、data、page 的响应对象
 */
export const fetchApiList = async (params = {}) => {
  try {
    const result = await fetchApi(API_CONFIG.APIS.LIST, { method: 'GET', params });
    return result || {};
  } catch (err) {
    return {};
  }
};

/**
 * 获取API详情
 * @param {string} id - API ID
 * @returns {Promise<Object>} 包含 code、messageZh、data 的响应对象
 */
export const fetchApiDetail = async (id) => {
  try {
    const result = await fetchApi(buildApiUrl(API_CONFIG.APIS.DETAIL, { id }));
    return result || {};
  } catch (err) {
    return {};
  }
};

/**
 * 注册新API（管理员操作）
 * @param {Object} data - API配置数据
 * @returns {Promise<Object>} 包含 code、messageZh、data 的响应对象
 */
export const createApi = async (data) => {
  try {
    const result = await fetchApi(API_CONFIG.APIS.CREATE, { method: 'POST', body: JSON.stringify(data) });
    return result || {};
  } catch (err) {
    return {};
  }
};

/**
 * 更新API信息（管理员操作）
 * @param {string} id - API ID
 * @param {Object} data - 更新后的API配置数据
 * @returns {Promise<Object>} 包含 code、messageZh、data 的响应对象
 */
export const updateApi = async (id, data) => {
  try {
    const result = await fetchApi(buildApiUrl(API_CONFIG.APIS.UPDATE, { id }), { method: 'PUT', body: JSON.stringify(data) });
    return result || {};
  } catch (err) {
    return {};
  }
};

/**
 * 删除API（管理员操作）
 * @param {string} id - API ID
 * @returns {Promise<Object>} 包含 code、messageZh、data 的响应对象
 */
export const deleteApi = async (id) => {
  try {
    const result = await fetchApi(buildApiUrl(API_CONFIG.APIS.DELETE, { id }), { method: 'DELETE' });
    return result || {};
  } catch (err) {
    return {};
  }
};

/**
 * 撤回API审核（重新变为草稿状态）
 * @param {string} id - API ID
 * @returns {Promise<Object>} 包含 code、messageZh、data 的响应对象
 */
export const withdrawApi = async (id) => {
  try {
    const result = await fetchApi(buildApiUrl(API_CONFIG.APIS.WITHDRAW, { id }), { method: 'POST' });
    return result || {};
  } catch (err) {
    return {};
  }
};
