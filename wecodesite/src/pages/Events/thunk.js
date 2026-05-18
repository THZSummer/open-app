/**
 * ========================================
 * 事件订阅管理 - 数据接口
 * ========================================
 * 功能：事件订阅相关接口定义
 */

import { API_CONFIG, buildApiUrl, fetchApi } from '../../configs/web.config';

// 分类别名
export const EVENT_CATEGORY_ALIAS = 'event';

/**
 * 获取事件分类
 *
 * @returns {Promise<Object>} 分类列表
 */
export const fetchEventCategories = async () => {
  try {
    const result = await fetchApi(API_CONFIG.CATEGORIES.LIST, {
      params: { categoryAlias: EVENT_CATEGORY_ALIAS }
    });
    return result || {};
  } catch (err) {
    return {};
  }
};

/**
 * 获取可选事件列表
 *
 * @param {Object} filter - 筛选条件
 * @returns {Promise<Object>} 事件列表
 */
export const fetchEvents = async ({
  keyword,
  needReview,
  categoryId,
  curPage,
  pageSize,
  appId
}) => {
  const query = {};
  if (keyword) query.keyword = keyword;
  if (needReview !== undefined && needReview !== 'all') {
    query.needApproval = needReview === 'true' ? 1 : 0;
  }
  if (curPage) query.curPage = curPage;
  if (pageSize) query.pageSize = pageSize;
  if (appId) query.appId = appId;
  query.includeChildren = true;

  try {
    const result = await fetchApi(
      buildApiUrl(API_CONFIG.CATEGORIES.EVENTS, { id: categoryId }),
      { params: query }
    );
    return result || {};
  } catch (err) {
    return {};
  }
};

/**
 * 获取所有事件（管理后台）
 *
 * @param {Object} params - 查询参数
 * @returns {Promise<Object>} 事件列表
 */
export const fetchAllEvents = async (params = {}) => {
  try {
    const result = await fetchApi(API_CONFIG.EVENTS.LIST, { params });
    return result || {};
  } catch (err) {
    return {};
  }
};

/**
 * 获取应用事件订阅列表
 *
 * @param {string} appId - 应用ID
 * @param {Object} params - 查询参数
 * @returns {Promise<Object>} 订阅列表
 */
export const fetchAppEvents = async (appId, params = {}) => {
  try {
    const result = await fetchApi(
      buildApiUrl(API_CONFIG.APP_EVENTS.LIST, { appId }),
      { params }
    );
    return result || {};
  } catch (err) {
    return {};
  }
};

/**
 * 订阅事件
 *
 * @param {string} appId - 应用ID
 * @param {Object} params - 订阅参数
 * @returns {Promise<Object>} 订阅结果
 */
export const subscribeEvents = async (appId, params) => {
  const subscribeParams = {
    ...params,
    channelType: 0
  };
  try {
    const result = await fetchApi(
      buildApiUrl(API_CONFIG.APP_EVENTS.SUBSCRIBE, { appId }),
      { method: 'POST', body: JSON.stringify(subscribeParams) }
    );
    return result || {};
  } catch (err) {
    return {};
  }
};

/**
 * 配置事件订阅
 *
 * @param {string} appId - 应用ID
 * @param {string} eventId - 事件ID
 * @param {Object} params - 配置参数
 * @returns {Promise<Object>} 配置结果
 */
export const configEventSubscription = async (appId, eventId, params) => {
  try {
    const result = await fetchApi(
      buildApiUrl(API_CONFIG.APP_EVENTS.CONFIG, { appId, id: eventId }),
      { method: 'PUT', body: JSON.stringify(params) }
    );
    return result || {};
  } catch (err) {
    return {};
  }
};

/**
 * 催办审批
 *
 * @param {string} id - 记录ID
 * @returns {Promise<Object>} 催办结果
 */
export const remindApproval = async (id) => {
  try {
    const result = await fetchApi(`/events/${id}/remind`, { method: 'POST' });
    return result || {};
  } catch (err) {
    return {};
  }
};

/**
 * 删除事件（管理后台）
 *
 * @param {string} id - 事件ID
 * @returns {Promise<Object>} 删除结果
 */
export const deleteEvent = async (id) => {
  try {
    const result = await fetchApi(
      buildApiUrl(API_CONFIG.EVENTS.DELETE, { id }),
      { method: 'DELETE' }
    );
    return result || {};
  } catch (err) {
    return {};
  }
};

/**
 * 删除应用事件订阅
 *
 * @param {string} appId - 应用ID
 * @param {string} subscriptionId - 订阅记录ID
 * @returns {Promise<Object>} 删除结果
 */
export const deleteAppEventSubscription = async (appId, subscriptionId) => {
  try {
    const result = await fetchApi(
      buildApiUrl(API_CONFIG.APP_EVENTS.DELETE, { appId, id: subscriptionId }),
      { method: 'DELETE' }
    );
    return result || {};
  } catch (err) {
    return {};
  }
};

/**
 * 撤回事件订阅
 *
 * @param {string} appId - 应用ID
 * @param {string} id - 订阅记录ID
 * @returns {Promise<Object>} 撤回结果
 */
export const withdrawApproval = async (appId, id) => {
  try {
    const result = await fetchApi(
      buildApiUrl(API_CONFIG.APP_EVENTS.WITHDRAW, { appId, id }),
      { method: 'POST' }
    );
    return result || {};
  } catch (err) {
    return {};
  }
};
