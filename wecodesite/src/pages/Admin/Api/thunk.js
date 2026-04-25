/**
 * Admin模块 - API管理相关API
 * 用于管理员管理平台全部API接口的定义、发布和下线操作
 */
import { useTrueFetch } from '@/utils/constants';
import { API_CONFIG, buildApiUrl, fetchApi } from '@/configs/web.config';
import { mockApis, mockCategories } from './mock';

const delay = (ms) => new Promise((resolve) => setTimeout(resolve, ms));

/**
 * 获取API列表（管理员视角）
 * @param {Object} params - 查询参数，包含 categoryId（分类ID）、status（状态）、keyword（关键词）
 * @returns {Promise<Object>} 包含 code、messageZh、data、page 的响应对象
 */
export const fetchApiList = async (params = {}) => {
  if (useTrueFetch) {
    try {
      const result = await fetchApi(API_CONFIG.APIS.LIST, { method: 'GET', params });
      return result || {};
    } catch (err) {
      return {};
    }
  }
  await delay(300);
  let data = mockApis;
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
 * 获取API详情
 * @param {string} id - API ID
 * @returns {Promise<Object>} 包含 code、messageZh、data 的响应对象
 */
export const fetchApiDetail = async (id) => {
  if (useTrueFetch) {
    try {
      const result = await fetchApi(buildApiUrl(API_CONFIG.APIS.DETAIL, { id }));
      return result || {};
    } catch (err) {
      return {};
    }
  }
  await delay(300);
  const api = mockApis.find(item => item.id === id);
  return {
    code: '200',
    messageZh: '查询成功',
    data: api
  };
};

/**
 * 注册新API（管理员操作）
 * @param {Object} data - API配置数据
 * @returns {Promise<Object>} 包含 code、messageZh、data 的响应对象
 */
export const createApi = async (data) => {
  if (useTrueFetch) {
    try {
      return await fetchApi(API_CONFIG.APIS.CREATE, { method: 'POST', body: JSON.stringify(data) });
    } catch (err) {
      return {};
    }
  }
  await delay(300);
  return {
    code: '200',
    messageZh: '注册成功',
    data: { id: String(Date.now()), ...data, status: 0 }
  };
};

/**
 * 更新API信息（管理员操作）
 * @param {string} id - API ID
 * @param {Object} data - 更新后的API配置数据
 * @returns {Promise<Object>} 包含 code、messageZh、data 的响应对象
 */
export const updateApi = async (id, data) => {
  if (useTrueFetch) {
    try {
      const result = await fetchApi(buildApiUrl(API_CONFIG.APIS.UPDATE, { id }), { method: 'PUT', body: JSON.stringify(data) });
      return result || {};
    } catch (err) {
      return {};
    }
  }
  await delay(300);
  return {
    code: '200',
    messageZh: '更新成功',
    data: { id, ...data }
  };
};

/**
 * 删除API（管理员操作）
 * @param {string} id - API ID
 * @returns {Promise<Object>} 包含 code、messageZh、data 的响应对象
 */
export const deleteApi = async (id) => {
  if (useTrueFetch) {
    try {
      const result = await fetchApi(buildApiUrl(API_CONFIG.APIS.DELETE, { id }), { method: 'DELETE' });
      return result || {};
    } catch (err) {
      return {};
    }
  }
  await delay(300);
  return {
    code: '200',
    messageZh: '删除成功',
    data: { id }
  };
};

/**
 * 撤回API审核（重新变为草稿状态）
 * @param {string} id - API ID
 * @returns {Promise<Object>} 包含 code、messageZh、data 的响应对象
 */
export const withdrawApi = async (id) => {
  if (useTrueFetch) {
    try {
      const result = await fetchApi(buildApiUrl(API_CONFIG.APIS.WITHDRAW, { id }), { method: 'POST' });
      return result || {};
    } catch (err) {
      return {};
    }
  }
  await delay(300);
  return {
    code: '200',
    messageZh: '撤回成功',
    data: { id, status: 0 }
  };
};