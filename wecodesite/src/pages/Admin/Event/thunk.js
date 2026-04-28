/**
 * Admin模块 - 事件管理相关API
 * 用于管理员管理平台全部事件定义的创建、发布和下线操作
 */
import { API_CONFIG, buildApiUrl, fetchApi } from '../../../configs/web.config';

/**
 * 获取事件列表（管理员视角）
 * @param {Object} params - 查询参数，包含 categoryId（分类ID）、status（状态）、keyword（关键词）
 * @returns {Promise<Object>} 包含 code、messageZh、data、page 的响应对象
 */
export const fetchEventList = async (params = {}) => {
  try {
    const result = await fetchApi(API_CONFIG.EVENTS.LIST, { method: 'GET', params });
    return result || {};
  } catch (err) {
    return {};
  }
};

/**
 * 获取事件详情
 * @param {string} id - 事件ID
 * @returns {Promise<Object>} 包含 code、messageZh、data 的响应对象
 */
export const fetchEventDetail = async (id) => {
  try {
    const result = await fetchApi(buildApiUrl(API_CONFIG.EVENTS.DETAIL, { id }));
    return result || {};
  } catch (err) {
    return {};
  }
};

/**
 * 创建新事件（管理员操作）
 * @param {Object} data - 事件配置数据
 * @returns {Promise<Object>} 包含 code、messageZh、data 的响应对象
 */
export const createEvent = async (data) => {
  try {
    const result = await fetchApi(API_CONFIG.EVENTS.CREATE, { method: 'POST', body: JSON.stringify(data) });
    return result || {};
  } catch (err) {
    return {};
  }
};

/**
 * 更新事件信息（管理员操作）
 * @param {string} id - 事件ID
 * @param {Object} data - 更新后的事件配置数据
 * @returns {Promise<Object>} 包含 code、messageZh、data 的响应对象
 */
export const updateEvent = async (id, data) => {
  try {
    const result = await fetchApi(buildApiUrl(API_CONFIG.EVENTS.UPDATE, { id }), { method: 'PUT', body: JSON.stringify(data) });
    return result || {};
  } catch (err) {
    return {};
  }
};

/**
 * 删除事件（管理员操作）
 * @param {string} id - 事件ID
 * @returns {Promise<Object>} 包含 code、messageZh、data 的响应对象
 */
export const deleteEvent = async (id) => {
  try {
    const result = await fetchApi(buildApiUrl(API_CONFIG.EVENTS.DELETE, { id }), { method: 'DELETE' });
    return result || {};
  } catch (err) {
    return {};
  }
};

/**
 * 撤回事件审核（重新变为草稿状态）
 * @param {string} id - 事件ID
 * @returns {Promise<Object>} 包含 code、messageZh、data 的响应对象
 */
export const withdrawEvent = async (id) => {
  try {
    const result = await fetchApi(buildApiUrl(API_CONFIG.EVENTS.WITHDRAW, { id }), { method: 'POST' });
    return result || {};
  } catch (err) {
    return {};
  }
};
