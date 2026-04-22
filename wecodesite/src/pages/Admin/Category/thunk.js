/**
 * Admin模块 - 分类管理相关API
 * 用于管理员管理平台API/事件/回调的分类结构
 */
import { useTrueFetch } from '@/utils/constants';
import { API_CONFIG, buildApiUrl, fetchApi } from '@/configs/web.config';
import { mockCategories, mockOwners } from './mock';

const delay = (ms) => new Promise((resolve) => setTimeout(resolve, ms));

/**
 * 获取分类树结构
 * @param {Object} params - 查询参数，包含 categoryAlias 等
 * @returns {Promise<Object>} 包含 code、messageZh、data 的响应对象
 */
export const fetchCategoryTree = async (params = {}) => {
  if (useTrueFetch) {
    return fetchApi(API_CONFIG.CATEGORIES.LIST, { params });
  }
  await delay(300);
  return {
    code: '200',
    messageZh: '查询成功',
    data: mockCategories
  };
};

/**
 * 获取分类详情
 * @param {string} id - 分类ID
 * @returns {Promise<Object>} 包含 code、messageZh、data 的响应对象
 */
export const fetchCategoryDetail = async (id) => {
  if (useTrueFetch) {
    return fetchApi(buildApiUrl(API_CONFIG.CATEGORIES.DETAIL, { id }));
  }
  await delay(300);
  const category = mockCategories.find(item => item.id === id);
  return {
    code: '200',
    messageZh: '查询成功',
    data: category
  };
};

/**
 * 获取分类负责人列表
 * @param {string} categoryId - 分类ID
 * @returns {Promise<Object>} 包含 code、messageZh、data 的响应对象
 */
export const fetchCategoryOwners = async (categoryId) => {
  if (useTrueFetch) {
    return fetchApi(buildApiUrl(API_CONFIG.CATEGORIES.OWNERS, { id: categoryId }));
  }
  await delay(300);
  return {
    code: '200',
    messageZh: '查询成功',
    data: mockOwners
  };
};

/**
 * 创建新分类
 * @param {Object} data - 分类数据（nameCn、nameEn、parentId等）
 * @returns {Promise<Object>} 包含 code、messageZh、data 的响应对象
 */
export const createCategory = async (data) => {
  if (useTrueFetch) {
    return fetchApi(API_CONFIG.CATEGORIES.CREATE, { method: 'POST', body: JSON.stringify(data) });
  }
  await delay(300);
  return {
    code: '200',
    messageZh: '创建成功',
    data: { id: String(Date.now()), ...data }
  };
};

/**
 * 更新分类信息
 * @param {string} id - 分类ID
 * @param {Object} data - 更新后的分类数据
 * @returns {Promise<Object>} 包含 code、messageZh、data 的响应对象
 */
export const updateCategory = async (id, data) => {
  if (useTrueFetch) {
    return fetchApi(buildApiUrl(API_CONFIG.CATEGORIES.UPDATE, { id }), { method: 'PUT', body: JSON.stringify(data) });
  }
  await delay(300);
  return {
    code: '200',
    messageZh: '更新成功',
    data: { id, ...data }
  };
};

/**
 * 删除分类
 * @param {string} id - 分类ID
 * @returns {Promise<Object>} 包含 code、messageZh、data 的响应对象
 */
export const deleteCategory = async (id) => {
  if (useTrueFetch) {
    return fetchApi(buildApiUrl(API_CONFIG.CATEGORIES.DELETE, { id }), { method: 'DELETE' });
  }
  await delay(300);
  return {
    code: '200',
    messageZh: '删除成功',
    data: { id }
  };
};

/**
 * 添加分类负责人
 * @param {string} categoryId - 分类ID
 * @param {Object} owner - 负责人信息（userId、userName等）
 * @returns {Promise<Object>} 包含 code、messageZh、data 的响应对象
 */
export const addCategoryOwner = async (categoryId, owner) => {
  if (useTrueFetch) {
    return fetchApi(buildApiUrl(API_CONFIG.CATEGORIES.OWNERS, { id: categoryId }), { method: 'POST', body: JSON.stringify(owner) });
  }
  await delay(300);
  return {
    code: '200',
    messageZh: '添加成功',
    data: { id: String(Date.now()), categoryId, ...owner }
  };
};

/**
 * 移除分类负责人
 * @param {string} categoryId - 分类ID
 * @param {string} userId - 用户ID
 * @returns {Promise<Object>} 包含 code、messageZh、data 的响应对象
 */
export const removeCategoryOwner = async (categoryId, userId) => {
  if (useTrueFetch) {
    return fetchApi(`/categories/${categoryId}/owners/${userId}`, { method: 'DELETE' });
  }
  await delay(300);
  return {
    code: '200',
    messageZh: '移除成功',
    data: { categoryId, userId }
  };
};