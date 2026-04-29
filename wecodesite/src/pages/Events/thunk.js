import { API_CONFIG, buildApiUrl, fetchApi } from '../../configs/web.config';

export const EVENT_CATEGORY_ALIAS = 'event';

export const fetchEventCategories = async () => {
  try {
    const result = await fetchApi(API_CONFIG.CATEGORIES.LIST, { params: { categoryAlias: EVENT_CATEGORY_ALIAS } });
    return result || {};
  } catch (err) {
    return {};
  }
};

export const fetchEvents = async ({ keyword, needReview, categoryId, curPage, pageSize, appId }) => {
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
    const result = await fetchApi(buildApiUrl(API_CONFIG.CATEGORIES.EVENTS, { id: categoryId }), { params: queryParams });
    return result || {};
  } catch (err) {
    return {};
  }
};

export const fetchAllEvents = async (params = {}) => {
  try {
    const result = await fetchApi(API_CONFIG.EVENTS.LIST, { params });
    return result || {};
  } catch (err) {
    return {};
  }
};

export const fetchAppEvents = async (appId, params = {}) => {
  try {
    const result = await fetchApi(buildApiUrl(API_CONFIG.APP_EVENTS.LIST, { appId }), { params });
    return result || {};
  } catch (err) {
    return {};
  }
};

export const subscribeEvents = async (appId, params) => {
  const subscribeParams = {
    ...params,
    channelType: 0
  };
  try {
    const result = await fetchApi(buildApiUrl(API_CONFIG.APP_EVENTS.SUBSCRIBE, { appId }), { method: 'POST', body: JSON.stringify(subscribeParams) });
    return result || {};
  } catch (err) {
    return {};
  }
};

export const configEventSubscription = async (appId, eventId, params) => {
  try {
    const result = await fetchApi(buildApiUrl(API_CONFIG.APP_EVENTS.CONFIG, { appId, id: eventId }), { method: 'PUT', body: JSON.stringify(params) });
    return result || {};
  } catch (err) {
    return {};
  }
};

export const remindApproval = async (id) => {
  try {
    const result = await fetchApi(`/events/${id}/remind`, { method: 'POST' });
    return result || {};
  } catch (err) {
    return {};
  }
};

export const deleteEvent = async (id) => {
  try {
    const result = await fetchApi(buildApiUrl(API_CONFIG.EVENTS.DELETE, { id }), { method: 'DELETE' });
    return result || {};
  } catch (err) {
    return {};
  }
};

export const withdrawApproval = async (appId, id) => {
  try {
    const result = await fetchApi(buildApiUrl(API_CONFIG.APP_EVENTS.WITHDRAW, { appId, id }), { method: 'POST' });
    return result || {};
  } catch (err) {
    return {};
  }
};
