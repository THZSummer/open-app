/**
 * 分类管理 API 接口
 * 提供分类的增删改查等接口调用
 */
import { API_CONFIG, buildApiUrl, fetchApi } from '@/configs/web.config';

/**
 * 获取分类列表
 * @param {Object} params - 查询参数，包含分页和筛选条件
 * @returns {Promise<Object>} 分类列表数据
 */
export const getClassifyList = async (params = {}) => {
  try {
    const result = await fetchApi(API_CONFIG.CLASSIFY.LIST, { method: 'GET', params });
    return result || {};
  } catch (err) {
    return {};
  }
};

/**
 * 获取分类详情
 * @param {string} classifyId - 分类ID
 * @returns {Promise<Object>} 分类详情数据
 */
export const getClassifyDetail = async (classifyId) => {
  try {
    const result = await fetchApi(buildApiUrl(API_CONFIG.CLASSIFY.DETAIL, { classifyId }));
    return result || {};
  } catch (err) {
    return {};
  }
};

/**
 * 新增分类
 * @param {Object} data - 分类表单数据
 * @returns {Promise<Object>} 新增结果
 */
export const createClassify = async (data) => {
  try {
    const result = await fetchApi(API_CONFIG.CLASSIFY.CREATE, { method: 'POST', body: data });
    return result || {};
  } catch (err) {
    return {};
  }
};

/**
 * 编辑分类
 * @param {string} classifyId - 分类ID
 * @param {Object} data - 分类表单数据
 * @returns {Promise<Object>} 编辑结果
 */
export const updateClassify = async (classifyId, data) => {
  try {
    const result = await fetchApi(buildApiUrl(API_CONFIG.CLASSIFY.UPDATE, { classifyId }), { method: 'PUT', body: data });
    return result || {};
  } catch (err) {
    return {};
  }
};

/**
 * 删除分类
 * @param {string} classifyId - 分类ID
 * @returns {Promise<Object>} 删除结果
 */
export const deleteClassify = async (classifyId) => {
  try {
    const result = await fetchApi(buildApiUrl(API_CONFIG.CLASSIFY.DELETE, { classifyId }), { method: 'DELETE' });
    return result || {};
  } catch (err) {
    return {};
  }
};
