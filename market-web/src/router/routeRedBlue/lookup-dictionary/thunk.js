/**
 * 数据字典 API 接口
 * 提供字典的增删改查接口调用
 */
import API_CONFIG from '../../../configs/web.config';
import { buildApiUrl, fetchApi } from '../../../utils/webFetch';

/**
 * 获取字典列表
 * @param {Object} params - 查询参数，包含分页和筛选条件
 * @returns {Promise<Object>} 字典列表数据
 */
export const getDictionaryList = async (params = {}) => {
  try {
    const result = await fetchApi(API_CONFIG.DICTIONARY_LIST, { method: 'GET', params });
    return result || {};
  } catch (err) {
    return {};
  }
};

/**
 * 获取字典详情
 * @param {string} id - 字典ID
 * @returns {Promise<Object>} 字典详情数据
 */
export const getDictionaryDetail = async (id) => {
  try {
    const result = await fetchApi(buildApiUrl(API_CONFIG.DICTIONARY_DETAIL, { id }));
    return result || {};
  } catch (err) {
    return {};
  }
};

/**
 * 新增字典
 * @param {Object} data - 字典表单数据
 * @returns {Promise<Object>} 新增结果
 */
export const createDictionary = async (data) => {
  try {
    const result = await fetchApi(API_CONFIG.DICTIONARY_CREATE, {
      method: 'POST',
      body: JSON.stringify(data)
    });
    return result || {};
  } catch (err) {
    return {};
  }
};

/**
 * 编辑字典
 * @param {string} id - 字典ID
 * @param {Object} data - 字典表单数据
 * @returns {Promise<Object>} 编辑结果
 */
export const updateDictionary = async (id, data) => {
  try {
    const result = await fetchApi(buildApiUrl(API_CONFIG.DICTIONARY_UPDATE, { id }), {
      method: 'PUT',
      body: JSON.stringify(data)
    });
    return result || {};
  } catch (err) {
    return {};
  }
};

/**
 * 删除字典
 * @param {string} id - 字典ID
 * @returns {Promise<Object>} 删除结果
 */
export const deleteDictionary = async (id) => {
  try {
    const result = await fetchApi(buildApiUrl(API_CONFIG.DICTIONARY_DELETE, { id }), { method: 'DELETE' });
    return result || {};
  } catch (err) {
    return {};
  }
};
