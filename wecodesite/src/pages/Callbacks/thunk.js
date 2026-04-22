import { useTrueFetch } from '@/utils/constants';
import { API_CONFIG, buildApiUrl, fetchApi } from '@/configs/web.config';
import { mockCallbacks, mockAllCallbacks } from './mock';

const delay = (ms) => new Promise((resolve) => setTimeout(resolve, ms));

/**
 * 获取应用已订阅的回调列表
 * @param {Object} params - 查询参数，包含 appId 等
 * @returns {Promise<Array>} 回调列表数组
 */
export const fetchCallbackList = async (params = {}) => {
  if (!useTrueFetch) {
    await delay(300);
    let data = mockCallbacks;
    data = data.filter(item => item.status !== 3);
    return data;
  }
  const result = await fetchApi(buildApiUrl(API_CONFIG.APP_CALLBACKS.LIST, { appId: params.appId || '10' }), { params });
  return result?.data || [];
};

/**
 * 获取全部可用回调列表（用于回调选择器，不包含待审核状态）
 * @returns {Promise<Array>} 回调列表
 */
export const fetchAllCallbacks = async () => {
  if (!useTrueFetch) {
    await delay(300);
    return mockAllCallbacks;
  }
  const result = await fetchApi(API_CONFIG.CALLBACKS.LIST, { params: { needApproval: 0, includeChildren: true } });
  return result?.data || [];
};

/**
 * 获取应用已订阅的回调列表（带分页和筛选）
 * @param {string} appId - 应用ID
 * @param {Object} params - 查询参数，包含 status、keyword、curPage、pageSize 等
 * @returns {Promise<Object>} 包含 code、messageZh、data、page 的响应对象
 */
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
  return fetchApi(buildApiUrl(API_CONFIG.APP_CALLBACKS.LIST, { appId }), { params });
};

/**
 * 订阅回调
 * @param {string} appId - 应用ID
 * @param {Object} params - 订阅参数，包含 permissionIds 等
 * @returns {Promise<Object>} 订阅结果
 */
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
  return fetchApi(buildApiUrl(API_CONFIG.APP_CALLBACKS.SUBSCRIBE, { appId }), { method: 'POST', body: JSON.stringify(params) });
};

/**
 * 配置回调订阅参数
 * @param {string} appId - 应用ID
 * @param {string} callbackId - 回调ID
 * @param {Object} params - 配置参数
 * @returns {Promise<Object>} 配置结果
 */
export const configCallbackSubscription = async (appId, callbackId, params) => {
  if (!useTrueFetch) {
    await delay(300);
    return {
      code: '200',
      messageZh: '回调配置已保存',
      data: { id: callbackId, ...params }
    };
  }
  return fetchApi(buildApiUrl(API_CONFIG.APP_CALLBACKS.CONFIG, { appId, id: callbackId }), { method: 'PUT', body: JSON.stringify(params) });
};

/**
 * 撤回回调申请
 * @param {string} appId - 应用ID
 * @param {string} subscriptionId - 订阅记录ID
 * @returns {Promise<Object>} 撤回结果
 */
export const withdrawCallbackApplication = async (appId, subscriptionId) => {
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
  return fetchApi(buildApiUrl(API_CONFIG.APP_CALLBACKS.WITHDRAW, { appId, id: subscriptionId }), { method: 'POST' });
};

/**
 * 催办回调审批
 * @param {string} id - 订阅记录ID
 * @returns {Promise<Object>} 催办结果
 */
export const remindApproval = async (id) => {
  if (!useTrueFetch) {
    await delay(300);
    console.log(`催办回调 id: ${id}`);
    return { success: true };
  }
  return fetchApi(`/callbacks/${id}/remind`, { method: 'POST' });
};

/**
 * 删除已订阅的回调
 * @param {string} id - 订阅记录ID
 * @returns {Promise<Object>} 删除结果
 */
export const deleteCallback = async (id) => {
  if (!useTrueFetch) {
    await delay(300);
    console.log(`删除回调 id: ${id}`);
    return { success: true };
  }
  return fetchApi(buildApiUrl(API_CONFIG.CALLBACKS.DELETE, { id }), { method: 'DELETE' });
};

/**
 * 撤回回调审核（已订阅列表中）
 * @param {string} id - 订阅记录ID
 * @returns {Promise<Object>} 撤回结果
 */
export const withdrawApproval = async (id) => {
  if (!useTrueFetch) {
    await delay(300);
    console.log(`撤回审核回调 id: ${id}`);
    return { success: true };
  }
  return fetchApi(buildApiUrl(API_CONFIG.APP_CALLBACKS.WITHDRAW, { appId: '10', id }), { method: 'POST' });
};