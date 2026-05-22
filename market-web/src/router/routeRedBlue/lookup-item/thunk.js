/**
 * LookUp 项管理 API 接口
 * 提供 LookUp 项的增删改查以及导入导出功能
 */
import { API_CONFIG, buildApiUrl, fetchApi } from '@/configs/web.config';

/**
 * 获取分类下的 LookUp 项列表
 * @param {string} classifyId - 分类ID
 * @param {Object} params - 查询参数，包含分页和筛选条件
 * @returns {Promise<Object>} LookUp 项列表数据
 */
export const getItemList = async (classifyId, params = {}) => {
  try {
    const result = await fetchApi(buildApiUrl(API_CONFIG.ITEM.LIST, { classifyId }), { method: 'GET', params });
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
    const result = await fetchApi(buildApiUrl(API_CONFIG.ITEM.DETAIL, { itemId }));
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
    const result = await fetchApi(buildApiUrl(API_CONFIG.ITEM.CREATE, { classifyId }), { method: 'POST', body: data });
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
    const result = await fetchApi(buildApiUrl(API_CONFIG.ITEM.UPDATE, { itemId }), { method: 'PUT', body: data });
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
    const result = await fetchApi(buildApiUrl(API_CONFIG.ITEM.DELETE, { itemId }), { method: 'DELETE' });
    return result || {};
  } catch (err) {
    return {};
  }
};

/**
 * 下载导入模板
 * @returns {Promise<Blob>} 模板文件 Blob 数据
 */
export const downloadImportTemplate = async () => {
  try {
    const result = await fetchApi(API_CONFIG.IMPORT_EXPORT.TEMPLATE, { method: 'GET', responseType: 'blob' });
    return result;
  } catch (err) {
    return null;
  }
};

/**
 * 批量导入 LookUp 项
 * @param {string} classifyId - 分类ID
 * @param {File} file - Excel 文件
 * @returns {Promise<Object>} 导入结果
 */
export const importItems = async (classifyId, file) => {
  try {
    const formData = new FormData();
    formData.append('file', file);
    const result = await fetchApi(`${API_CONFIG.IMPORT_EXPORT.IMPORT}?classifyId=${classifyId}`, {
      method: 'POST',
      body: formData,
      headers: { 'Content-Type': 'multipart/form-data' }
    });
    return result || {};
  } catch (err) {
    return {};
  }
};

/**
 * 导出 LookUp 项
 * @param {string} classifyId - 分类ID
 * @param {Object} params - 导出筛选参数
 * @returns {Promise<Blob>} 导出文件 Blob 数据
 */
export const exportItems = async (classifyId, params = {}) => {
  try {
    const result = await fetchApi(API_CONFIG.IMPORT_EXPORT.EXPORT, {
      method: 'GET',
      params: { classifyId, ...params },
      responseType: 'blob'
    });
    return result;
  } catch (err) {
    return null;
  }
};

/**
 * 异步导入 LookUp 项（创建任务，立即返回）
 * @param {string} classifyId - 分类ID
 * @param {File} file - Excel 文件
 * @returns {Promise<Object>} 任务ID
 */
export const importItemsAsync = async (classifyId, file) => {
  try {
    const formData = new FormData();
    formData.append('file', file);
    const result = await fetchApi(`${API_CONFIG.IMPORT_EXPORT.IMPORT_ASYNC}?classifyId=${classifyId}`, {
      method: 'POST',
      body: formData,
      headers: { 'Content-Type': 'multipart/form-data' }
    });
    return result || {};
  } catch (err) {
    return {};
  }
};

/**
 * 异步导出 LookUp 项（创建任务，立即返回）
 * @param {Object} params - 导出参数
 * @returns {Promise<Object>} 任务ID
 */
export const exportItemsAsync = async (params = {}) => {
  try {
    const result = await fetchApi(API_CONFIG.IMPORT_EXPORT.EXPORT_ASYNC, { method: 'POST', body: params });
    return result || {};
  } catch (err) {
    return {};
  }
};

/**
 * 下载任务结果文件
 * @param {string} taskId - 任务ID
 * @returns {Promise<Blob>} 结果文件 Blob 数据
 */
export const downloadTaskResult = async (taskId) => {
  try {
    const result = await fetchApi(buildApiUrl(API_CONFIG.IMPORT_EXPORT.TASK_RESULT, { taskId }), {
      method: 'GET',
      responseType: 'blob'
    });
    return result;
  } catch (err) {
    return null;
  }
};
