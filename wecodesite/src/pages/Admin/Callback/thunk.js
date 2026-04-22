/**
 * Admin模块 - 回调管理相关API
 * 用于管理员管理平台全部回调接口的定义、发布和下线操作
 */
import { useTrueFetch } from '@/utils/constants';
import { API_CONFIG, buildApiUrl, fetchApi } from '@/configs/web.config';
import { mockCallbacks } from './mock';

const delay = (ms) => new Promise((resolve) => setTimeout(resolve, ms));

/**
 * 获取回调列表（管理员视角）
 * @param {Object} params - 查询参数，包含 categoryId（分类ID）、status（状态）、keyword（关键词）
 * @returns {Promise<Object>} 包含 code、messageZh、data、page 的响应对象
 */
export const fetchCallbackList = async (params = {}) => {
  if (useTrueFetch) {
    return fetchApi(API_CONFIG.CALLBACKS.LIST, { method: 'GET', params });
  }
  await delay(300);
  let data = mockCallbacks;
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
 * 获取回调详情
 * @param {string} id - 回调ID
 * @returns {Promise<Object>} 包含 code、messageZh、data 的响应对象
 */
export const fetchCallbackDetail = async (id) => {
  if (useTrueFetch) {
    return fetchApi(buildApiUrl(API_CONFIG.CALLBACKS.DETAIL, { id }));
  }
  await delay(300);
  const callback = mockCallbacks.find(item => item.id === id);
  return {
    code: '200',
    messageZh: '查询成功',
    data: callback
  };
};

/**
 * 创建新回调（管理员操作）
 * @param {Object} data - 回调配置数据
 * @returns {Promise<Object>} 包含 code、messageZh、data 的响应对象
 */
export const createCallback = async (data) => {
  if (useTrueFetch) {
    return fetchApi(API_CONFIG.CALLBACKS.CREATE, { method: 'POST', body: JSON.stringify(data) });
  }
  await delay(300);
  return {
    code: '200',
    messageZh: '注册成功',
    data: { id: String(Date.now()), ...data, status: 0 }
  };
};

/**
 * 更新回调信息（管理员操作）
 * @param {string} id - 回调ID
 * @param {Object} data - 更新后的回调配置数据
 * @returns {Promise<Object>} 包含 code、messageZh、data 的响应对象
 */
export const updateCallback = async (id, data) => {
  if (useTrueFetch) {
    return fetchApi(buildApiUrl(API_CONFIG.CALLBACKS.UPDATE, { id }), { method: 'PUT', body: JSON.stringify(data) });
  }
  await delay(300);
  return {
    code: '200',
    messageZh: '更新成功',
    data: { id, ...data }
  };
};

/**
 * 删除回调（管理员操作）
 * @param {string} id - 回调ID
 * @returns {Promise<Object>} 包含 code、messageZh、data 的响应对象
 */
export const deleteCallback = async (id) => {
  if (useTrueFetch) {
    return fetchApi(buildApiUrl(API_CONFIG.CALLBACKS.DELETE, { id }), { method: 'DELETE' });
  }
  await delay(300);
  return {
    code: '200',
    messageZh: '删除成功',
    data: { id }
  };
};

/**
 * 撤回回调审核（重新变为草稿状态）
 * @param {string} id - 回调ID
 * @returns {Promise<Object>} 包含 code、messageZh、data 的响应对象
 */
export const withdrawCallback = async (id) => {
  if (useTrueFetch) {
    return fetchApi(buildApiUrl(API_CONFIG.CALLBACKS.WITHDRAW, { id }), { method: 'POST' });
  }
  await delay(300);
  return {
    code: '200',
    messageZh: '撤回成功',
    data: { id, status: 0 }
  };
};