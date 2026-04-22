/**
 * API 管理相关 API
 * 用于应用订阅 API 权限
 */
import { useTrueFetch } from '@/utils/constants';
import { API_CONFIG, buildApiUrl, fetchApi } from '@/configs/web.config';
import { mockApis, identityPermissionApis } from './mock';

const delay = (ms) => new Promise((resolve) => setTimeout(resolve, ms));

/**
 * 获取应用已订阅的API列表
 * @param {Object} params - 查询参数，包含 appId 等
 * @returns {Promise<Array>} API列表数组
 */
export const fetchApiList = async (params = {}) => {
  if (!useTrueFetch) {
    await delay(300);
    let data = mockApis;
    data = data.filter(item => item.status !== 3);
    return data;
  }
  const result = await fetchApi(buildApiUrl(API_CONFIG.APP_APIS.LIST, { appId: params.appId || '10' }), { params });
  return result?.data || [];
};

/**
 * 获取分类树（用于模块列表）
 * @param {string} categoryAlias - 分类别名（可选）
 * @returns {Promise<Array>} 分类树数组
 */
export const fetchApiModules = async (categoryAlias) => {
  if (!useTrueFetch) {
    await delay(300);
    const identityKey = `BUSINESS_IDENTITY_${categoryAlias}`;
    return identityPermissionApis[identityKey]?.modules || [];
  }
  const result = await fetchApi(API_CONFIG.CATEGORIES.LIST, { params: { categoryAlias } });
  return result?.data || [];
};

/**
 * 获取身份类型对应的分类模块
 * @param {string} identityType - 身份类型
 * @returns {Promise<Array>} 分类列表
 */
export const fetchIdentityModules = async (identityType) => {
  if (!useTrueFetch) {
    await delay(300);
    return identityPermissionApis[identityType]?.modules || [];
  }
  const categoryAlias = identityType.split('_')[0];
  const result = await fetchApi(API_CONFIG.CATEGORIES.LIST, { params: { categoryAlias } });
  return result?.data || [];
};

/**
 * 获取身份类型对应的全部API列表
 * @param {string} identityType - 身份类型
 * @returns {Promise<Array>} API列表
 */
export const fetchIdentityApis = async (identityType) => {
  if (!useTrueFetch) {
    await delay(300);
    return identityPermissionApis[identityType]?.apis || [];
  }
  const result = await fetchApi(buildApiUrl(API_CONFIG.CATEGORIES.APIS, { id: 'root' }), { 
    params: { includeChildren: true } 
  });
  return result?.data || [];
};

/**
 * 根据筛选条件获取API列表（用于API选择器）
 * @param {Object} params - 筛选参数，包含 auth、name、scope、needReview、identityType、appType
 * @returns {Promise<Array>} 筛选后的API列表
 */
export const fetchFilteredApis = async ({ auth, name, scope, needReview, identityType, appType }) => {
  if (!useTrueFetch) {
    await delay(300);
    let apis;
    if (identityType) {
      apis = identityPermissionApis[identityType]?.apis || [];
    } else {
      const type = appType === 'personal' ? 'PERSONAL_IDENTITY' : 'BUSINESS_IDENTITY';
      const key = `${type}_${auth}`;
      apis = identityPermissionApis[key]?.apis || [];
    }
    if (name) {
      apis = apis.filter(api => (api.nameCn || api.name).includes(name));
    }
    if (scope) {
      apis = apis.filter(api => api.scope.includes(scope));
    }
    if (needReview !== undefined && needReview !== 'all') {
      const needReviewBool = needReview === 'true';
      apis = apis.filter(api => api.needReview === needReviewBool);
    }
    return apis;
  }
  
  const queryParams = {};
  if (name) queryParams.keyword = name;
  if (needReview !== undefined && needReview !== 'all') {
    queryParams.needApproval = needReview === 'true' ? 1 : 0;
  }
  queryParams.includeChildren = true;
  
  const result = await fetchApi(buildApiUrl(API_CONFIG.CATEGORIES.APIS, { id: '11' }), { params: queryParams });
  let apis = result?.data || [];
  
  if (scope) {
    apis = apis.filter(api => api.scope.includes(scope));
  }
  
  return apis;
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
    return {
      code: '200',
      messageZh: '查询成功',
      data: data,
      page: { curPage: 1, pageSize: 20, total: data.length }
    };
  }
  return fetchApi(buildApiUrl(API_CONFIG.APP_APIS.LIST, { appId }), { params });
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
  return fetchApi(buildApiUrl(API_CONFIG.APP_APIS.SUBSCRIBE, { appId }), { method: 'POST', body: JSON.stringify(params) });
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
  return fetchApi(buildApiUrl(API_CONFIG.APP_APIS.WITHDRAW, { appId, id: subscriptionId }), { method: 'POST' });
};

/**
 * 催办API审批
 * @param {string} id - 订阅记录ID
 * @returns {Promise<Object>} 催办结果
 */
export const remindApproval = async (id) => {
  await delay(300);
  console.log(`催办 API id: ${id}`);
  return { success: true };
};

/**
 * 删除已订阅的API权限
 * @param {string} id - 订阅记录ID
 * @returns {Promise<Object>} 删除结果
 */
export const deleteApi = async (id) => {
  if (!useTrueFetch) {
    await delay(300);
    console.log(`删除 API id: ${id}`);
    return { success: true };
  }
  return fetchApi(buildApiUrl(API_CONFIG.APIS.DELETE, { id }), { method: 'DELETE' });
};

/**
 * 撤回API审核（已订阅列表中）
 * @param {string} id - 订阅记录ID
 * @returns {Promise<Object>} 撤回结果
 */
export const withdrawApproval = async (id) => {
  if (!useTrueFetch) {
    await delay(300);
    console.log(`撤回审核 API id: ${id}`);
    return { success: true };
  }
  return fetchApi(buildApiUrl(API_CONFIG.APP_APIS.WITHDRAW, { appId: '10', id }), { method: 'POST' });
};
