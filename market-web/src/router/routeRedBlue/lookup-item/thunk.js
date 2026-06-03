/**
 * LookUp 项管理 API 接口
 * 提供 LookUp 项的增删改查功能
 */
import API_CONFIG from '../../../configs/web.config';
import { buildApiUrl, fetchApi } from '../../../utils/webFetch';

/**
 * 获取分类下的 LookUp 项列表
 * @param {string} classifyId - 分类ID
 * @param {Object} params - 查询参数，包含分页和筛选条件
 * @returns {Promise<Object>} LookUp 项列表数据
 */
export const getItemList = async (classifyId, params = {}) => {
  try {
    const result = await fetchApi(buildApiUrl(API_CONFIG.ITEM_LIST, { classifyId }), { method: 'GET', params });
    return result || {};
  } catch (err) {
    return {};
  }
};

/**
 * 获取 LookUp 项详情
 * @param {string} itemId - 项ID
 * @returns {Promise<Object>} LookUp 项详情数据
 */
export const getItemDetail = async (itemId) => {
  try {
    const result = await fetchApi(buildApiUrl(API_CONFIG.ITEM_DETAIL, { itemId }));
    return result || {};
  } catch (err) {
    return {};
  }
};

/**
 * 新增 LookUp 项
 * @param {string} classifyId - 分类ID
 * @param {Object} data - 项表单数据
 * @returns {Promise<Object>} 新增结果
 */
export const createItem = async (classifyId, data) => {
  try {
    const result = await fetchApi(buildApiUrl(API_CONFIG.ITEM_CREATE, { classifyId }), {
      method: 'POST',
      body: JSON.stringify(data)
    });
    return result || {};
  } catch (err) {
    return {};
  }
};

/**
 * 编辑 LookUp 项
 * @param {string} itemId - 项ID
 * @param {Object} data - 项表单数据
 * @returns {Promise<Object>} 编辑结果
 */
export const updateItem = async (itemId, data) => {
  try {
    const result = await fetchApi(buildApiUrl(API_CONFIG.ITEM_UPDATE, { itemId }), {
      method: 'PUT',
      body: JSON.stringify(data)
    });
    return result || {};
  } catch (err) {
    return {};
  }
};

/**
 * 删除 LookUp 项
 * @param {string} itemId - 项ID
 * @returns {Promise<Object>} 删除结果
 */
export const deleteItem = async (itemId) => {
  try {
    const result = await fetchApi(buildApiUrl(API_CONFIG.ITEM_DELETE, { itemId }), { method: 'DELETE' });
    return result || {};
  } catch (err) {
    return {};
  }
};