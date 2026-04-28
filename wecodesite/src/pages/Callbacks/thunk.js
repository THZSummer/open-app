import { API_CONFIG, buildApiUrl, fetchApi } from '../../configs/web.config';

export const CALLBACK_CATEGORY_ALIAS = 'callback';

export const fetchCallbackCategories = async () => {
  try {
    const result = await fetchApi(API_CONFIG.CATEGORIES.LIST, { params: { categoryAlias: CALLBACK_CATEGORY_ALIAS } });
    return result || {};
  } catch (err) {
    return {};
  }
};

export const fetchCallbacks = async ({ keyword, needReview, categoryId, curPage, pageSize, appId }) => {
  const queryParams = {};
  if (keyword) queryParams.keyword = keyword;
  if (needReview !== undefined && needReview !== 'all') {
    queryParams.need_approval = needReview === 'true' ? 1 : 0;
  }
  if (curPage) queryParams.curPage = curPage;
  if (pageSize) queryParams.pageSize = pageSize;
  if (appId) queryParams.appId = appId;
  queryParams.includeChildren = true;
  
  try {
    const result = await fetchApi(buildApiUrl(API_CONFIG.CATEGORIES.CALLBACKS, { id: categoryId }), { params: queryParams });
    return result || {};
  } catch (err) {
    return {};
  }
};

export const fetchAllCallbacks = async (params = {}) => {
  try {
    const result = await fetchApi(API_CONFIG.CALLBACKS.LIST, { params });
    return result || {};
  } catch (err) {
    return {};
  }
};

export const fetchAppCallbacks = async (appId, params = {}) => {
  try {
    const result = await fetchApi(buildApiUrl(API_CONFIG.APP_CALLBACKS.LIST, { appId }), { params });
    return result || {};
  } catch (err) {
    return {};
  }
};

export const subscribeCallbacks = async (appId, params) => {
  try {
    const result = await fetchApi(buildApiUrl(API_CONFIG.APP_CALLBACKS.SUBSCRIBE, { appId }), { method: 'POST', body: JSON.stringify(params) });
    return result || {};
  } catch (err) {
    return {};
  }
};

export const configCallbackSubscription = async (appId, callbackId, params) => {
  try {
    const result = await fetchApi(buildApiUrl(API_CONFIG.APP_CALLBACKS.CONFIG, { appId, id: callbackId }), { method: 'PUT', body: JSON.stringify(params) });
    return result || {};
  } catch (err) {
    return {};
  }
};

export const remindApproval = async (id) => {
  try {
    const result = await fetchApi(`/callbacks/${id}/remind`, { method: 'POST' });
    return result || {};
  } catch (err) {
    return {};
  }
};

export const deleteCallback = async (id) => {
  try {
    const result = await fetchApi(buildApiUrl(API_CONFIG.CALLBACKS.DELETE, { id }), { method: 'DELETE' });
    return result || {};
  } catch (err) {
    return {};
  }
};

export const withdrawApproval = async (appId, id) => {
  try {
    const result = await fetchApi(buildApiUrl(API_CONFIG.APP_CALLBACKS.WITHDRAW, { appId, id }), { method: 'POST' });
    return result || {};
  } catch (err) {
    return {};
  }
};
