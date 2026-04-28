/**
 * Admin模块 - 分类管理相关API
 * 用于管理员管理平台API/事件/回调的分类结构
 */
import { API_CONFIG, buildApiUrl, fetchApi } from '../../../configs/web.config';

/**
 * 获取分类树结构
 * @param {Object} params - 查询参数，包含 categoryAlias 等
 * @returns {Promise<Object>} 包含 code、messageZh、data 的响应对象
 */
export const fetchCategoryTree = async (params = {}) => {
  try {
    const result = await fetchApi(API_CONFIG.CATEGORIES.LIST, { params });
    return result || {};
  } catch (err) {
    return {};
  }
};

/**
 * 获取分类详情
 * @param {string} id - 分类ID
 * @returns {Promise<Object>} 包含 code、messageZh、data 的响应对象
 */
export const fetchCategoryDetail = async (id) => {
  try {
    const result = await fetchApi(buildApiUrl(API_CONFIG.CATEGORIES.DETAIL, { id }));
    return result || {};
  } catch (err) {
    return {};
  }
};

/**
 * 获取分类负责人列表
 * @param {string} categoryId - 分类ID
 * @returns {Promise<Object>} 包含 code、messageZh、data 的响应对象
 */
export const fetchCategoryOwners = async (categoryId) => {
  try {
    const result = await fetchApi(buildApiUrl(API_CONFIG.CATEGORIES.OWNERS, { id: categoryId }));
    return result || {};
  } catch (err) {
    return {};
  }
};

/**
 * 创建新分类
 * @param {Object} data - 分类数据（nameCn、nameEn、parentId等）
 * @returns {Promise<Object>} 包含 code、messageZh、data 的响应对象
 */
export const createCategory = async (data) => {
  try {
    const result = await fetchApi(API_CONFIG.CATEGORIES.CREATE, { method: 'POST', body: JSON.stringify(data) });
    return result || {};
  } catch (err) {
    return {};
  }
};

/**
 * 更新分类信息
 * @param {string} id - 分类ID
 * @param {Object} data - 更新后的分类数据
 * @returns {Promise<Object>} 包含 code、messageZh、data 的响应对象
 */
export const updateCategory = async (id, data) => {
  try {
    const result = await fetchApi(buildApiUrl(API_CONFIG.CATEGORIES.UPDATE, { id }), { method: 'PUT', body: JSON.stringify(data) });
    return result || {};
  } catch (err) {
    return {};
  }
};

/**
 * 删除分类
 * @param {string} id - 分类ID
 * @returns {Promise<Object>} 包含 code、messageZh、data 的响应对象
 */
export const deleteCategory = async (id) => {
  try {
    const result = await fetchApi(buildApiUrl(API_CONFIG.CATEGORIES.DELETE, { id }), { method: 'DELETE' });
    return result || {};
  } catch (err) {
    return {};
  }
};

/**
 * 添加分类负责人
 * @param {string} categoryId - 分类ID
 * @param {Object} owner - 负责人信息（userId、userName等）
 * @returns {Promise<Object>} 包含 code、messageZh、data 的响应对象
 */
export const addCategoryOwner = async (categoryId, owner) => {
  try {
    const result = await fetchApi(buildApiUrl(API_CONFIG.CATEGORIES.OWNERS, { id: categoryId }), { method: 'POST', body: JSON.stringify(owner) });
    return result || {};
  } catch (err) {
    return {};
  }
};

/**
 * 移除分类负责人
 * @param {string} categoryId - 分类ID
 * @param {string} userId - 用户ID
 * @returns {Promise<Object>} 包含 code、messageZh、data 的响应对象
 */
export const removeCategoryOwner = async (categoryId, userId) => {
  try {
    const result = await fetchApi(`/categories/${categoryId}/owners/${userId}`, { method: 'DELETE' });
    return result || {};
  } catch (err) {
    return {};
  }
};
