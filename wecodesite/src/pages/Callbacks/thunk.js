import { useTrueFetch } from '../../utils/constants';
import { API_CONFIG, buildApiUrl, fetchApi } from '../../configs/web.config';
import { mockCallbacks, mockAllCallbacks } from './mock';

const delay = (ms) => new Promise((resolve) => setTimeout(resolve, ms));

export const CALLBACK_CATEGORY_ALIAS = 'callback';

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

export const fetchCallbackCategories = async () => {
  if (!useTrueFetch) {
    await delay(300);
    const modules = mockAllCallbacks.map((callback, index) => ({
      key: String(index + 1),
      name: callback.callbackType || '回调分类'
    }));
    const categories = [{
      id: '1',
      nameCn: '回调分类',
      children: modules
    }];
    return categories;
  }
  try {
    const result = await fetchApi(API_CONFIG.CATEGORIES.LIST, { params: { categoryAlias: CALLBACK_CATEGORY_ALIAS } });
    return result || {};
  } catch (err) {
    return {};
  }
};

export const fetchCallbacks = async ({ keyword, needReview, categoryId, curPage, pageSize, appId }) => {
  if (!useTrueFetch) {
    await delay(300);
    let callbacks = mockAllCallbacks.map((callback, index) => ({
      id: String(301 + index),
      nameCn: callback.name,
      name: callback.name,
      scope: `callback:${callback.callback}`,
      needApproval: callback.needReview ? 1 : 0,
      needReview: callback.needReview,
      isSubscribed: 0,
      docUrl: callback.docUrl
    }));
    
    const total = callbacks.length;
    const startIndex = ((curPage || 1) - 1) * (pageSize || 10);
    const paginatedCallbacks = callbacks.slice(startIndex, startIndex + (pageSize || 10));
    
    return {
      data: paginatedCallbacks,
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
    const result = await fetchApi(buildApiUrl(API_CONFIG.CATEGORIES.CALLBACKS, { id: categoryId }), { params: queryParams });
    return result || {};
  } catch (err) {
    return {};
  }
};

export const fetchAllCallbacks = async (params = {}) => {
  if (!useTrueFetch) {
    await delay(300);
    let data = mockAllCallbacks;
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
    const result = await fetchApi(API_CONFIG.CALLBACKS.LIST, { params });
    return result || {};
  } catch (err) {
    return {};
  }
};

export const fetchAppCallbacks = async (appId, params = {}) => {
  if (!useTrueFetch) {
    await delay(300);
    let data = mockCallbacks;
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
    const result = await fetchApi(buildApiUrl(API_CONFIG.APP_CALLBACKS.LIST, { appId }), { params });
    return result || {};
  } catch (err) {
    return {};
  }
};

export const subscribeCallbacks = async (appId, params) => {
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
          status: 0
        })) || []
      }
    };
  }
  try {
    const result = await fetchApi(buildApiUrl(API_CONFIG.APP_CALLBACKS.SUBSCRIBE, { appId }), { method: 'POST', body: JSON.stringify(params) });
    return result || {};
  } catch (err) {
    return {};
  }
};

export const configCallbackSubscription = async (appId, callbackId, params) => {
  if (!useTrueFetch) {
    await delay(300);
    return {
      code: '200',
      messageZh: '回调配置已保存',
      data: { id: callbackId, ...params }
    };
  }
  try {
    const result = await fetchApi(buildApiUrl(API_CONFIG.APP_CALLBACKS.CONFIG, { appId, id: callbackId }), { method: 'PUT', body: JSON.stringify(params) });
    return result || {};
  } catch (err) {
    return {};
  }
};

export const remindApproval = async (id) => {
  if (!useTrueFetch) {
    await delay(300);
    console.log(`催办回调 id: ${id}`);
    return { code: '200', messageZh: '催办成功' };
  }
  try {
    const result = await fetchApi(`/callbacks/${id}/remind`, { method: 'POST' });
    return result || {};
  } catch (err) {
    return {};
  }
};

export const deleteCallback = async (id) => {
  if (!useTrueFetch) {
    await delay(300);
    console.log(`删除回调 id: ${id}`);
    return { code: '200', messageZh: '删除成功' };
  }
  try {
    const result = await fetchApi(buildApiUrl(API_CONFIG.CALLBACKS.DELETE, { id }), { method: 'DELETE' });
    return result || {};
  } catch (err) {
    return {};
  }
};

export const withdrawApproval = async (id) => {
  if (!useTrueFetch) {
    await delay(300);
    console.log(`撤回审核回调 id: ${id}`);
    return { code: '200', messageZh: '已撤回' };
  }
  try {
    const result = await fetchApi(buildApiUrl(API_CONFIG.APP_CALLBACKS.WITHDRAW, { appId: '10', id }), { method: 'POST' });
    return result || {};
  } catch (err) {
    return {};
  }
};
