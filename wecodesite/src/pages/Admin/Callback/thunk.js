/**
 * ========================================
 * 回调管理 - API接口层
 * ========================================
 *
 * 【功能说明】
 * 提供回调的增删改查操作接口封装
 */

import { API_CONFIG, buildApiUrl, fetchApi } from '../../../configs/web.config';

/**
 * 获取回调列表数据
 *
 * @description 分页查询平台所有回调接口
 * @param {Object} filter - 筛选条件
 * @param {string} [filter.categoryId] - 分类ID
 * @param {number} [filter.status] - 状态
 * @param {string} [filter.keyword] - 关键词
 * @returns {Promise<Object>} { code, data, page }
 */
export const fetchCallbackList = async (filter = {}) => {
  try {
    const result = await fetchApi(API_CONFIG.CALLBACKS.LIST, { method: 'GET', params: filter });
    return result || {};
  } catch (err) {
    return {};
  }
};

/**
 * 获取回调详情
 *
 * @description 根据ID查询单个回调的完整信息
 * @param {string} callbackId - 回调ID
 * @returns {Promise<Object>} { code, data }
 */
export const fetchCallbackDetail = async (callbackId) => {
  try {
    const result = await fetchApi(buildApiUrl(API_CONFIG.CALLBACKS.DETAIL, { id: callbackId }));
    return result || {};
  } catch (err) {
    return {};
  }
};

/**
 * 新建回调接口
 *
 * @description 创建新的回调配置
 * @param {Object} config - 回调配置数据
 * @returns {Promise<Object>} { code, data }
 */
export const createCallback = async (config) => {
  try {
    const result = await fetchApi(API_CONFIG.CALLBACKS.CREATE, {
      method: 'POST',
      body: JSON.stringify(config)
    });
    return result || {};
  } catch (err) {
    return {};
  }
};

/**
 * 更新回调配置
 *
 * @description 修改指定回调的信息
 * @param {string} callbackId - 回调ID
 * @param {Object} config - 更新后的配置
 * @returns {Promise<Object>} { code, data }
 */
export const updateCallback = async (callbackId, config) => {
  try {
    const result = await fetchApi(buildApiUrl(API_CONFIG.CALLBACKS.UPDATE, { id: callbackId }), {
      method: 'PUT',
      body: JSON.stringify(config)
    });
    return result || {};
  } catch (err) {
    return {};
  }
};

/**
 * 删除回调
 *
 * @description 永久删除指定的回调配置
 * @param {string} callbackId - 回调ID
 * @returns {Promise<Object>} { code, data }
 */
export const deleteCallback = async (callbackId) => {
  try {
    const result = await fetchApi(buildApiUrl(API_CONFIG.CALLBACKS.DELETE, { id: callbackId }), {
      method: 'DELETE'
    });
    return result || {};
  } catch (err) {
    return {};
  }
};

/**
 * 撤回回调审核
 *
 * @description 将已提交审核的回调退回草稿状态
 * @param {string} callbackId - 回调ID
 * @returns {Promise<Object>} { code, data }
 */
export const withdrawCallback = async (callbackId) => {
  try {
    const result = await fetchApi(buildApiUrl(API_CONFIG.CALLBACKS.WITHDRAW, { id: callbackId }), {
      method: 'POST'
    });
    return result || {};
  } catch (err) {
    return {};
  }
};
