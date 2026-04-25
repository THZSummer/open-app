/**
 * API 管理相关 API
 * 用于应用订阅 API 权限
 */
import { useTrueFetch } from '@/utils/constants';
import { API_CONFIG, buildApiUrl, fetchApi } from '@/configs/web.config';
import { mockApis, identityPermissionApis } from './mock';

const delay = (ms) => new Promise((resolve) => setTimeout(resolve, ms));

/**
 * 获取分类列表（用于模块列表）
 * @param {string} identityType - 身份类型（如 BUSINESS_IDENTITY, PERSONAL_IDENTITY）
 * @returns {Promise<Array>} 分类列表数组
 */
const API_TYPE_MAP = {
  app_type_a: 'SOA',
  app_type_b: 'APIG',
  personal_aksk: 'AKSK'
};

const getMockKey = (identityType, apiType) => {
  const apiTypeSuffix = API_TYPE_MAP[apiType] || apiType;
  return `${identityType}_${apiTypeSuffix}`;
};

export const fetchCategories = async (apiType) => {
  if (!useTrueFetch) {
    await delay(300);
    const mockKey = getMockKey(identityType, apiType);
    const data = identityPermissionApis[mockKey]?.modules || [];
    const categories = [{
      id: '1',
      nameCn: 'API分类',
      children: data.map((item, index) => ({
        id: item.key,
        nameCn: item.name,
        name: item.name
      }))
    }];
    return categories;
  }
  try {
    const result = await fetchApi(API_CONFIG.CATEGORIES.LIST, { params: { categoryAlias: apiType } });
    return result || {};
  } catch (err) {
    return {};
  }
};

/**
 * 根据筛选条件获取API列表（用于API选择器）
 * @param {Object} params - 筛选参数，包含 identityType、keyword、needReview、categoryId、curPage、pageSize
 * @returns {Promise<Array>} 筛选后的API列表
 */
export const fetchApis = async ({ keyword, needReview, identityType, apiType, categoryId, curPage, pageSize, appId }) => {
  if (!useTrueFetch) {
    await delay(300);
    const mockKey = getMockKey(identityType, apiType);
    let apis = identityPermissionApis[mockKey]?.apis || [];
    
    if (categoryId && categoryId !== 'all') {
      const category = identityPermissionApis[mockKey]?.modules?.find(m => m.key === categoryId);
      if (category) {
        apis = apis.filter(api => api.category === category.name);
      }
    }
    
    if (keyword) {
      const lowerKeyword = keyword.toLowerCase();
      apis = apis.filter(api => 
        (api.nameCn || api.name || '').toLowerCase().includes(lowerKeyword) || 
        (api.scope || '').toLowerCase().includes(lowerKeyword)
      );
    }
    if (needReview !== undefined && needReview !== 'all') {
      const needReviewBool = needReview === 'true';
      apis = apis.filter(api => api.needReview === needReviewBool);
    }
    
    const total = apis.length;
    const startIndex = ((curPage || 1) - 1) * (pageSize || 10);
    const paginatedApis = apis.slice(startIndex, startIndex + (pageSize || 10));
    
    return {
      data: paginatedApis,
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
    const result = await fetchApi(buildApiUrl(API_CONFIG.CATEGORIES.APIS, { id: categoryId }), { params: queryParams });
    return result || {};
  } catch (err) {
    return {};
  }
};

/**
 * 获取应用已订阅的API列表（带分页）
 * @param {string} appId - 应用ID
 * @param {Object} params - 查询参数，包含 status、keyword、curPage、pageSize 等
 * @returns {Promise<Object>} 包含 code、messageZh、data、page 的响应对象
 */
export const fetchAppApis = async (appId, params = {}) => {
  if (!useTrueFetch) {
    await delay(300);
    let data = mockApis;
    data = data.filter(item => item.status !== 3);
    if (params.status !== undefined) {
      data = data.filter(item => item.status === params.status);
    }
    if (params.keyword) {
      data = data.filter(item =>
        item.permission?.nameCn?.includes(params.keyword) ||
        item.permission?.scope?.includes(params.keyword)
      );
    }
    
    const curPage = params.curPage || 1;
    const pageSize = params.pageSize || 10;
    const total = data.length;
    const startIndex = (curPage - 1) * pageSize;
    const paginatedData = data.slice(startIndex, startIndex + pageSize);
    
    return {
      code: '200',
      messageZh: '查询成功',
      data: paginatedData,
      page: { curPage, pageSize, total }
    };
  }
  try {
    const result = await fetchApi(buildApiUrl(API_CONFIG.APP_APIS.LIST, { appId }), { params });
    return result || {};
  } catch (err) {
    return {};
  }
};

/**
 * 订阅API权限
 * @param {string} appId - 应用ID
 * @param {Object} params - 订阅参数，包含 permissionIds 等
 * @returns {Promise<Object>} 订阅结果
 */
export const subscribeApis = async (appId, params) => {
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
    const result = await fetchApi(buildApiUrl(API_CONFIG.APP_APIS.SUBSCRIBE, { appId }), { method: 'POST', body: JSON.stringify(params) });
    return result || {};
  } catch (err) {
    return {};
  }
};

/**
 * 撤回API申请
 * @param {string} appId - 应用ID
 * @param {string} subscriptionId - 订阅记录ID
 * @returns {Promise<Object>} 撤回结果
 */
export const withdrawApiApplication = async (appId, subscriptionId) => {
  if (!useTrueFetch) {
    await delay(300);
    return {
      code: '200',
      messageZh: '申请已撤回',
      data: {
        id: subscriptionId,
        status: 2
      }
    };
  }
  try {
    const result = await fetchApi(buildApiUrl(API_CONFIG.APP_APIS.WITHDRAW, { appId, id: subscriptionId }), { method: 'POST' });
    return result || {};
  } catch (err) {
    return {};
  }
};

/**
 * 催办API审批
 * @param {string} id - 订阅记录ID
 * @returns {Promise<Object>} 催办结果
 */
export const remindApproval = async (id) => {
  if (!useTrueFetch) {
    await delay(300);
    console.log(`催办 API id: ${id}`);
    return { code: '200', messageZh: '催办成功' };
  }
  try {
    const result = await fetchApi(`/approval/remind`, { method: 'POST', body: JSON.stringify({ id }) });
    return result || {};
  } catch (err) {
    return {};
  }
};
