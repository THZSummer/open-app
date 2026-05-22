/**
 * 任务管理 API 接口
 * 提供任务的查询、创建和状态更新等功能
 */
import { API_CONFIG, buildApiUrl, fetchApi } from '@/configs/web.config';

/**
 * 获取任务列表
 * @param {Object} params - 查询参数，包含分页和筛选条件
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
 * 获取任务详情
 * @param {string} taskId - 任务ID
 * @returns {Promise<Object>} 任务详情数据
 */
export const getTaskDetail = async (taskId) => {
  try {
    const result = await fetchApi(buildApiUrl(API_CONFIG.TASK.DETAIL, { taskId }));
    return result || {};
  } catch (err) {
    return {};
  }
};

/**
 * 创建任务
 * @param {Object} data - 任务创建参数
 * @returns {Promise<Object>} 任务创建结果
 */
export const createTask = async (data) => {
  try {
    const result = await fetchApi(API_CONFIG.TASK.CREATE, { method: 'POST', body: data });
    return result || {};
  } catch (err) {
    return {};
  }
};

/**
 * 更新任务状态
 * @param {string} taskId - 任务ID
 * @param {Object} data - 任务状态更新参数
 * @returns {Promise<Object>} 状态更新结果
 */
export const updateTaskStatus = async (taskId, data) => {
  try {
    const result = await fetchApi(buildApiUrl(API_CONFIG.TASK.UPDATE_STATUS, { taskId }), { method: 'PUT', body: data });
    return result || {};
  } catch (err) {
    return {};
  }
};
