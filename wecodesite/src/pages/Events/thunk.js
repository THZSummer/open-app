import { useTrueFetch } from '@/utils/constants';
import { API_CONFIG, buildApiUrl, fetchApi } from '@/configs/web.config';
import { mockEvents, mockSubscriptionConfig, mockAllEvents } from './mock';

const delay = (ms) => new Promise((resolve) => setTimeout(resolve, ms));

/**
 * 获取应用已订阅的事件列表
 * @param {Object} params - 查询参数，包含 appId 等
 * @returns {Promise<Array>} 事件列表数组
 */
export const fetchEventList = async (params = {}) => {
  if (!useTrueFetch) {
    await delay(300);
    let data = mockEvents;
    data = data.filter(item => item.status !== 3);
    return data;
  }
  const result = await fetchApi(buildApiUrl(API_CONFIG.APP_EVENTS.LIST, { appId: params.appId || '10' }), { params });
  return result?.data || [];
};

/**
 * 获取事件订阅配置信息
 * @returns {Promise<Object>} 订阅配置对象
 */
export const fetchSubscriptionConfig = async () => {
  if (!useTrueFetch) {
    await delay(300);
    return mockSubscriptionConfig;
  }
  return fetchApi('/events/subscription/config');
};

/**
 * 获取全部可用事件列表（用于事件选择器，不包含待审核状态）
 * @returns {Promise<Array>} 事件列表
 */
export const fetchAllEvents = async () => {
  if (!useTrueFetch) {
    await delay(300);
    return mockAllEvents;
  }
  const result = await fetchApi(API_CONFIG.EVENTS.LIST, { params: { needApproval: 0, includeChildren: true } });
  return result?.data || [];
};

/**
 * 获取应用已订阅的事件列表（带分页和筛选）
 * @param {string} appId - 应用ID
 * @param {Object} params - 查询参数，包含 status、keyword、curPage、pageSize 等
 * @returns {Promise<Object>} 包含 code、messageZh、data、page 的响应对象
 */
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
  return fetchApi(buildApiUrl(API_CONFIG.APP_EVENTS.LIST, { appId }), { params });
};

/**
 * 订阅事件
 * @param {string} appId - 应用ID
 * @param {Object} params - 订阅参数，包含 permissionIds 等
 * @returns {Promise<Object>} 订阅结果
 */
export const subscribeEvents = async (appId, params) => {
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
  return fetchApi(buildApiUrl(API_CONFIG.APP_EVENTS.SUBSCRIBE, { appId }), { method: 'POST', body: JSON.stringify(params) });
};

/**
 * 配置事件订阅参数
 * @param {string} appId - 应用ID
 * @param {string} eventId - 事件ID
 * @param {Object} params - 配置参数
 * @returns {Promise<Object>} 配置结果
 */
export const configEventSubscription = async (appId, eventId, params) => {
  if (!useTrueFetch) {
    await delay(300);
    return {
      code: '200',
      messageZh: '订阅配置已保存',
      data: { id: eventId, ...params }
    };
  }
  return fetchApi(buildApiUrl(API_CONFIG.APP_EVENTS.CONFIG, { appId, id: eventId }), { method: 'PUT', body: JSON.stringify(params) });
};

/**
 * 撤回事件申请
 * @param {string} appId - 应用ID
 * @param {string} subscriptionId - 订阅记录ID
 * @returns {Promise<Object>} 撤回结果
 */
export const withdrawEventApplication = async (appId, subscriptionId) => {
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
  return fetchApi(buildApiUrl(API_CONFIG.APP_EVENTS.WITHDRAW, { appId, id: subscriptionId }), { method: 'POST' });
};

/**
 * 催办事件审批
 * @param {string} id - 订阅记录ID
 * @returns {Promise<Object>} 催办结果
 */
export const remindApproval = async (id) => {
  if (!useTrueFetch) {
    await delay(300);
    console.log(`催办事件 id: ${id}`);
    return { success: true };
  }
  return fetchApi(`/events/${id}/remind`, { method: 'POST' });
};

/**
 * 删除已订阅的事件
 * @param {string} id - 订阅记录ID
 * @returns {Promise<Object>} 删除结果
 */
export const deleteEvent = async (id) => {
  if (!useTrueFetch) {
    await delay(300);
    console.log(`删除事件 id: ${id}`);
    return { success: true };
  }
  return fetchApi(buildApiUrl(API_CONFIG.EVENTS.DELETE, { id }), { method: 'DELETE' });
};

/**
 * 撤回事件审核（已订阅列表中）
 * @param {string} id - 订阅记录ID
 * @returns {Promise<Object>} 撤回结果
 */
export const withdrawApproval = async (id) => {
  if (!useTrueFetch) {
    await delay(300);
    console.log(`撤回审核事件 id: ${id}`);
    return { success: true };
  }
  return fetchApi(buildApiUrl(API_CONFIG.APP_EVENTS.WITHDRAW, { appId: '10', id }), { method: 'POST' });
};