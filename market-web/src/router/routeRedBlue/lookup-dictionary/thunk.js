/**
 * 数据字典 API 接口
 * 提供字典的增删改查、导入导出等接口调用
 */
import { API_CONFIG, buildApiUrl, fetchApi } from '../../../configs/web.config';

/**
 * 获取字典列表
 * @param {Object} params - 查询参数，包含分页和筛选条件
 * @returns {Promise<Object>} 字典列表数据
 */
export const getDictionaryList = async (params = {}) => {
  try {
    const result = await fetchApi(API_CONFIG.DICTIONARY.LIST, { method: 'GET', params });
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
    const result = await fetchApi(buildApiUrl(API_CONFIG.DICTIONARY.DETAIL, { id }));
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
    const result = await fetchApi(API_CONFIG.DICTIONARY.CREATE, { method: 'POST', body: data });
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
    const result = await fetchApi(buildApiUrl(API_CONFIG.DICTIONARY.UPDATE, { id }), { method: 'PUT', body: data });
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
    const result = await fetchApi(buildApiUrl(API_CONFIG.DICTIONARY.DELETE, { id }), { method: 'DELETE' });
    return result || {};
  } catch (err) {
    return {};
  }
};

/**
 * 下载导入模板
 * @returns {Promise<Blob>} 模板文件 Blob
 */
export const downloadImportTemplate = async () => {
  try {
    const response = await fetch(`${API_CONFIG.BASE_URL}${API_CONFIG.DICTIONARY.IMPORT_TEMPLATE}`, {
      method: 'GET',
      credentials: 'include'
    });
    return response.blob();
  } catch (err) {
    throw err;
  }
};

/**
 * 提交导入任务
 * @param {File} file - 导入文件
 * @returns {Promise<Object>} 任务ID
 */
export const submitImportTask = async (file) => {
  try {
    const formData = new FormData();
    formData.append('file', file);

    const token = localStorage.getItem('token');
    const appId = localStorage.getItem('appId');

    const response = await fetch(`${API_CONFIG.BASE_URL}${API_CONFIG.DICTIONARY.IMPORT_ASYNC}`, {
      method: 'POST',
      credentials: 'include',
      body: formData,
      headers: {
        ...(token && { Authorization: `Bearer ${token}` }),
        ...(appId && { 'X-App-Id': appId })
      }
    });

    return response.json();
  } catch (err) {
    return {};
  }
};

/**
 * 提交导出任务
 * @param {Object} params - 导出参数，包含 selectedIds 和 filters
 * @returns {Promise<Object>} 任务ID
 */
export const submitExportTask = async (params = {}) => {
  try {
    const result = await fetchApi(API_CONFIG.DICTIONARY.EXPORT_ASYNC, {
      method: 'POST',
      body: { params }
    });
    return result || {};
  } catch (err) {
    return {};
  }
};

/**
 * 获取任务列表
 * @param {Object} params - 查询参数
 * @returns {Promise<Object>} 任务列表数据
 */
export const getTaskList = async (params = {}) => {
  try {
    const result = await fetchApi(API_CONFIG.TASK.LIST, { method: 'GET', params });
    return result || {};
  } catch (err) {
    return {};
  }
};

/**
 * 获取任务进度
 * @param {string} taskId - 任务ID
 * @returns {Promise<Object>} 任务进度信息
 */
export const getTaskProgress = async (taskId) => {
  try {
    const result = await fetchApi(buildApiUrl(API_CONFIG.TASK.PROGRESS, { taskId }));
    return result || {};
  } catch (err) {
    return {};
  }
};

/**
 * 下载任务结果
 * @param {string} taskId - 任务ID
 * @returns {Promise<Blob>} 结果文件 Blob
 */
export const downloadTaskResult = async (taskId) => {
  try {
    const response = await fetch(`${API_CONFIG.BASE_URL}${buildApiUrl(API_CONFIG.TASK.DOWNLOAD, { taskId })}`, {
      method: 'GET',
      credentials: 'include',
      responseType: 'blob'
    });
    return response.blob();
  } catch (err) {
    throw err;
  }
};
