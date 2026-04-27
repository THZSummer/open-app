import { useTrueFetch } from '../../utils/constants';
import { API_CONFIG, buildApiUrl, fetchApi } from '../../configs/web.config';
import { mockEvents, mockAllEvents } from './mock';

const delay = (ms) => new Promise((resolve) => setTimeout(resolve, ms));

export const EVENT_CATEGORY_ALIAS = 'event';

const transformCategoriesToModules = (categories) => {
  if (!Array.isArray(categories) || categories.length === 0) return [];

  const result = [];
  const firstCategoryId = categories[0]?.id;
  if (firstCategoryId) {
    result.push({
      key: 'all',
      value: firstCategoryId,
      name: '全部分类'
    });
  }

  categories.forEach(cat => {
    if (cat.children && Array.isArray(cat.children)) {
      cat.children.forEach(child => {
        if (child.id) {
          result.push({
            key: child.id,
            value: child.id,
            name: child.nameCn || child.name
          });
        }
      });
    }
  });

  return result;
};

export const fetchEventCategories = async () => {
  if (!useTrueFetch) {
    await delay(300);
    const modules = mockAllEvents.map((event, index) => ({
      key: String(index + 1),
      name: event.eventType || '事件分类'
    }));
    const categories = [{
      id: '1',
      nameCn: '事件分类',
      children: modules
    }];
    return categories;
  }
  try {
    const result = await fetchApi(API_CONFIG.CATEGORIES.LIST, { params: { categoryAlias: EVENT_CATEGORY_ALIAS } });
    return result || {};
  } catch (err) {
    return {};
  }
};

export const fetchEvents = async ({ keyword, needReview, categoryId, curPage, pageSize, appId }) => {
  if (!useTrueFetch) {
    await delay(300);
    let events = mockAllEvents.map((event, index) => ({
      id: String(201 + index),
      nameCn: event.name,
      name: event.name,
      topic: event.event,
      scope: `event:${event.event}`,
      needApproval: event.needReview ? 1 : 0,
      needReview: event.needReview,
      isSubscribed: 0,
      docUrl: event.docUrl
    }));
    
    const total = events.length;
    const startIndex = ((curPage || 1) - 1) * (pageSize || 10);
    const paginatedEvents = events.slice(startIndex, startIndex + (pageSize || 10));
    
    return {
      data: paginatedEvents,
      total: total,
      page: { curPage: curPage || 1, pageSize: pageSize || 10, total }
    };
  }
  
  const queryParams = {};
  if (keyword) queryParams.keyword = keyword;
  if (needReview !== undefined && needReview !== 'all') {
    queryParams.needApproval = needReview === 'true' ? 1 : 0;
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
  if (!useTrueFetch) {
    await delay(300);
    let data = mockAllEvents;
    const curPage = params.curPage || 1;
    const pageSize = params.pageSize || 20;
    const start = (curPage - 1) * pageSize;
    const end = start + pageSize;
    return {
      code: '200',
      messageZh: '查询成功',
      data: data.slice(start, end),
      page: { curPage, pageSize, total: data.length }
    };
  }
  try {
    const result = await fetchApi(API_CONFIG.EVENTS.LIST, { params });
    return result || {};
  } catch (err) {
    return {};
  }
};

export const fetchAppEvents = async (appId, params = {}) => {
  if (!useTrueFetch) {
    await delay(300);
    let data = mockEvents;
    data = data.filter(item => item.status !== 3);
    if (params.status !== undefined) {
      data = data.filter(item => item.status === params.status);
    }
    if (params.keyword) {
      data = data.filter(item =>
        item.permission?.nameCn?.includes(params.keyword)
      );
    }
    return {
      code: '200',
      messageZh: '查询成功',
      data: data,
      page: { curPage: 1, pageSize: 20, total: data.length }
    };
  }
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
  if (!useTrueFetch) {
    await delay(300);
    const { permissionIds } = params;
    return {
      code: '200',
      messageZh: `申请已提交，共${permissionIds?.length || 0}条，等待审批`,
      data: {
        successCount: permissionIds?.length || 0,
        failedCount: 0,
        records: permissionIds?.map(permissionId => ({
          id: String(Date.now() + Math.random()),
          appId,
          permissionId,
          channelType: 0,
          status: 0
        })) || []
      }
    };
  }
  try {
    const result = await fetchApi(buildApiUrl(API_CONFIG.APP_EVENTS.SUBSCRIBE, { appId }), { method: 'POST', body: JSON.stringify(subscribeParams) });
    return result || {};
  } catch (err) {
    return {};
  }
};

export const configEventSubscription = async (appId, eventId, params) => {
  if (!useTrueFetch) {
    await delay(300);
    return {
      code: '200',
      messageZh: '订阅配置已保存',
      data: { id: eventId, ...params }
    };
  }
  try {
    const result = await fetchApi(buildApiUrl(API_CONFIG.APP_EVENTS.CONFIG, { appId, id: eventId }), { method: 'PUT', body: JSON.stringify(params) });
    return result || {};
  } catch (err) {
    return {};
  }
};

export const remindApproval = async (id) => {
  if (!useTrueFetch) {
    await delay(300);
    console.log(`催办事件 id: ${id}`);
    return { code: '200', messageZh: '催办成功' };
  }
  try {
    const result = await fetchApi(`/events/${id}/remind`, { method: 'POST' });
    return result || {};
  } catch (err) {
    return {};
  }
};

export const deleteEvent = async (id) => {
  if (!useTrueFetch) {
    await delay(300);
    console.log(`删除事件 id: ${id}`);
    return { code: '200', messageZh: '删除成功' };
  }
  try {
    const result = await fetchApi(buildApiUrl(API_CONFIG.EVENTS.DELETE, { id }), { method: 'DELETE' });
    return result || {};
  } catch (err) {
    return {};
  }
};

export const withdrawApproval = async (id) => {
  if (!useTrueFetch) {
    await delay(300);
    console.log(`撤回审核事件 id: ${id}`);
    return { code: '200', messageZh: '已撤回' };
  }
  try {
    const result = await fetchApi(buildApiUrl(API_CONFIG.APP_EVENTS.WITHDRAW, { appId: '10', id }), { method: 'POST' });
    return result || {};
  } catch (err) {
    return {};
  }
};
