/**
 * API 管理相关 API
 * 用于应用订阅 API 权限
 */
import { API_CONFIG, buildApiUrl, fetchApi } from '../../configs/web.config';

/**
 * 获取分类列表（用于模块列表）
 * @param {string} identityType - 身份类型（如 BUSINESS_IDENTITY, PERSONAL_IDENTITY）
 * @param {string} apiType - API类型（如 api_business_app_soa, api_personal_user_aksk）
 * @returns {Promise<Array>} 分类列表数组
 */
export const fetchCategories = async (apiType) => {
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
export const fetchApis = async ({ keyword, needReview, apiType, categoryId, curPage, pageSize, appId }) => {
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
  try {
    const result = await fetchApi(`/approval/remind`, { method: 'POST', body: JSON.stringify({ id }) });
    return result || {};
  } catch (err) {
    return {};
  }
};

/**
 * 删除已撤回的API订阅
 * @param {string} appId - 应用ID
 * @param {string} subscriptionId - 订阅记录ID
 * @returns {Promise<Object>} 删除结果
 */
export const deleteApiSubscription = async (appId, subscriptionId) => {
  try {
    const result = await fetchApi(buildApiUrl(API_CONFIG.APP_APIS.DELETE, { appId, id: subscriptionId }), { method: 'DELETE' });
    return result || {};
  } catch (err) {
    return {};
  }
};
