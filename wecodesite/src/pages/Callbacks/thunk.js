/**
 * ========================================
 * 回调订阅管理 - API层
 * ========================================
 * 说明：回调订阅相关接口
 */

import { API_CONFIG, buildApiUrl, fetchApi } from '../../configs/web.config';

// 分类别名常量
export const CALLBACK_CATEGORY_ALIAS = 'callback';

/**
 * 获取回调分类
 */
export const fetchCallbackCategories = async () => {
  try {
    const result = await fetchApi(API_CONFIG.CATEGORIES.LIST, {
      params: { categoryAlias: CALLBACK_CATEGORY_ALIAS }
    });
    return result || {};
  } catch (err) {
    return {};
  }
};

/**
 * 获取可订阅的回调列表
 */
export const fetchCallbacks = async ({
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
      buildApiUrl(API_CONFIG.CATEGORIES.CALLBACKS, { id: categoryId }),
      { params: query }
    );
    return result || {};
  } catch (err) {
    return {};
  }
};

/**
 * 获取所有回调（管理后台用）
 */
export const fetchAllCallbacks = async (params = {}) => {
  try {
    const result = await fetchApi(API_CONFIG.CALLBACKS.LIST, { params });
    return result || {};
  } catch (err) {
    return {};
  }
};

/**
 * 获取应用回调订阅列表
 */
export const fetchAppCallbacks = async (appId, params = {}) => {
  try {
    const result = await fetchApi(
      buildApiUrl(API_CONFIG.APP_CALLBACKS.LIST, { appId }),
      { params }
    );
    return result || {};
  } catch (err) {
    return {};
  }
};

/**
 * 订阅回调
 */
export const subscribeCallbacks = async (appId, params) => {
  try {
    const result = await fetchApi(
      buildApiUrl(API_CONFIG.APP_CALLBACKS.SUBSCRIBE, { appId }),
      { method: 'POST', body: JSON.stringify(params) }
    );
    return result || {};
  } catch (err) {
    return {};
  }
};

/**
 * 配置回调订阅
 */
export const configCallbackSubscription = async (appId, callbackId, params) => {
  try {
    const result = await fetchApi(
      buildApiUrl(API_CONFIG.APP_CALLBACKS.CONFIG, { appId, id: callbackId }),
      { method: 'PUT', body: JSON.stringify(params) }
    );
    return result || {};
  } catch (err) {
    return {};
  }
};

/**
 * 催办回调审批
 */
export const remindApproval = async (id) => {
  try {
    const result = await fetchApi(`/callbacks/${id}/remind`, { method: 'POST' });
    return result || {};
  } catch (err) {
    return {};
  }
};

/**
 * 删除回调（管理后台）
 */
export const deleteCallback = async (id) => {
  try {
    const result = await fetchApi(
      buildApiUrl(API_CONFIG.CALLBACKS.DELETE, { id }),
      { method: 'DELETE' }
    );
    return result || {};
  } catch (err) {
    return {};
  }
};

/**
 * 删除应用回调订阅
 */
export const deleteAppCallbackSubscription = async (appId, subscriptionId) => {
  try {
    const result = await fetchApi(
      buildApiUrl(API_CONFIG.APP_CALLBACKS.DELETE, { appId, id: subscriptionId }),
      { method: 'DELETE' }
    );
    return result || {};
  } catch (err) {
    return {};
  }
};

/**
 * 撤回回调订阅
 */
export const withdrawApproval = async (appId, id) => {
  try {
    const result = await fetchApi(
      buildApiUrl(API_CONFIG.APP_CALLBACKS.WITHDRAW, { appId, id }),
      { method: 'POST' }
    );
    return result || {};
  } catch (err) {
    return {};
  }
};
