/**
 * Admin模块 - 事件管理相关API
 * 用于管理员管理平台全部事件定义的创建、发布和下线操作
 */
import { useTrueFetch } from '@/utils/constants';
import { API_CONFIG, buildApiUrl, fetchApi } from '@/configs/web.config';
import { mockEvents } from './mock';

const delay = (ms) => new Promise((resolve) => setTimeout(resolve, ms));

/**
 * 获取事件列表（管理员视角）
 * @param {Object} params - 查询参数，包含 categoryId（分类ID）、status（状态）、keyword（关键词）
 * @returns {Promise<Object>} 包含 code、messageZh、data、page 的响应对象
 */
export const fetchEventList = async (params = {}) => {
  if (useTrueFetch) {
    return fetchApi(API_CONFIG.EVENTS.LIST, { method: 'GET', params });
  }
  await delay(300);
  let data = mockEvents;
  if (params.categoryId) {
    data = data.filter(item => item.categoryId === params.categoryId);
  }
  if (params.status !== undefined) {
    data = data.filter(item => item.status === params.status);
  }
  if (params.keyword) {
    data = data.filter(item =>
      item.nameCn.includes(params.keyword) ||
      item.nameEn.includes(params.keyword)
    );
  }
  return {
    code: '200',
    messageZh: '查询成功',
    data: data,
    page: { curPage: 1, pageSize: 20, total: data.length }
  };
};

/**
 * 获取事件详情
 * @param {string} id - 事件ID
 * @returns {Promise<Object>} 包含 code、messageZh、data 的响应对象
 */
export const fetchEventDetail = async (id) => {
  if (useTrueFetch) {
    return fetchApi(buildApiUrl(API_CONFIG.EVENTS.DETAIL, { id }));
  }
  await delay(300);
  const event = mockEvents.find(item => item.id === id);
  return {
    code: '200',
    messageZh: '查询成功',
    data: event
  };
};

/**
 * 创建新事件（管理员操作）
 * @param {Object} data - 事件配置数据
 * @returns {Promise<Object>} 包含 code、messageZh、data 的响应对象
 */
export const createEvent = async (data) => {
  if (useTrueFetch) {
    return fetchApi(API_CONFIG.EVENTS.CREATE, { method: 'POST', body: JSON.stringify(data) });
  }
  await delay(300);
  return {
    code: '200',
    messageZh: '注册成功',
    data: { id: String(Date.now()), ...data, status: 0 }
  };
};

/**
 * 更新事件信息（管理员操作）
 * @param {string} id - 事件ID
 * @param {Object} data - 更新后的事件配置数据
 * @returns {Promise<Object>} 包含 code、messageZh、data 的响应对象
 */
export const updateEvent = async (id, data) => {
  if (useTrueFetch) {
    return fetchApi(buildApiUrl(API_CONFIG.EVENTS.UPDATE, { id }), { method: 'PUT', body: JSON.stringify(data) });
  }
  await delay(300);
  return {
    code: '200',
    messageZh: '更新成功',
    data: { id, ...data }
  };
};

/**
 * 删除事件（管理员操作）
 * @param {string} id - 事件ID
 * @returns {Promise<Object>} 包含 code、messageZh、data 的响应对象
 */
export const deleteEvent = async (id) => {
  if (useTrueFetch) {
    return fetchApi(buildApiUrl(API_CONFIG.EVENTS.DELETE, { id }), { method: 'DELETE' });
  }
  await delay(300);
  return {
    code: '200',
    messageZh: '删除成功',
    data: { id }
  };
};

/**
 * 撤回事件审核（重新变为草稿状态）
 * @param {string} id - 事件ID
 * @returns {Promise<Object>} 包含 code、messageZh、data 的响应对象
 */
export const withdrawEvent = async (id) => {
  if (useTrueFetch) {
    return fetchApi(buildApiUrl(API_CONFIG.EVENTS.WITHDRAW, { id }), { method: 'POST' });
  }
  await delay(300);
  return {
    code: '200',
    messageZh: '撤回成功',
    data: { id, status: 0 }
  };
};