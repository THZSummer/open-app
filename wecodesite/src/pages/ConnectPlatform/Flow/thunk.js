/**
 * ========================================
 * 连接流列表 - API调用函数
 * ========================================
 *
 * 提供连接流列表相关的API调用
 */

import { API_CONFIG, buildApiUrl, fetchApi } from '../../../configs/web.config';

/**
 * 获取连接流列表
 *
 * @param {Object} params - 查询参数
 * @param {string} [params.keyword] - 搜索关键词
 * @param {number} [params.curPage] - 当前页码
 * @param {number} [params.pageSize] - 每页条数
 * @param {number} [params.lifecycleStatus] - 生命周期状态（0=未部署，1=运行中，2=已停止）
 * @returns {Promise<Object>} 连接流列表
 */
export const fetchFlowList = async (params = {}) => {
  try {
    const result = await fetchApi(API_CONFIG.FLOWS.LIST, { params });
    return result || {};
  } catch (err) {
    return {};
  }
};

/**
 * 删除连接流
 *
 * @param {string} flowId - 连接流ID
 * @returns {Promise<Object>} 删除结果
 */
export const deleteFlow = async (flowId) => {
  try {
    const result = await fetchApi(
      buildApiUrl(API_CONFIG.FLOWS.DELETE, { flowId }),
      { method: 'DELETE' }
    );
    return result || {};
  } catch (err) {
    return {};
  }
};

/**
 * 创建连接流
 *
 * @param {Object} data - 连接流数据
 * @param {string} data.nameCn - 连接流中文名称
 * @param {string} data.nameEn - 连接流英文名称
 * @param {string} data.descriptionCn - 连接流中文描述
 * @param {string} data.descriptionEn - 连接流英文描述
 * @returns {Promise<Object>} 创建结果
 */
export const createFlow = async (data) => {
  try {
    const result = await fetchApi(API_CONFIG.FLOWS.CREATE, {
      method: 'POST',
      body: JSON.stringify(data),
    });
    return result || {};
  } catch (err) {
    return {};
  }
};

/**
 * 更新连接流
 *
 * @param {Object} params - 更新参数
 * @param {string} params.flowId - 连接流ID
 * @param {Object} params.data - 连接流数据
 * @param {string} params.data.nameCn - 连接流中文名称
 * @param {string} params.data.nameEn - 连接流英文名称
 * @param {string} params.data.descriptionCn - 连接流中文描述
 * @param {string} params.data.descriptionEn - 连接流英文描述
 * @returns {Promise<Object>} 更新结果
 */
export const updateFlow = async ({ flowId, data }) => {
  try {
    const result = await fetchApi(
      buildApiUrl(API_CONFIG.FLOWS.UPDATE, { flowId }),
      {
        method: 'PUT',
        body: JSON.stringify(data),
      }
    );
    return result || {};
  } catch (err) {
    return {};
  }
};

/**
 * 中止（停止）连接流
 *
 * @param {string} flowId - 连接流ID
 * @returns {Promise<Object>} 停止结果
 */
export const stopFlow = async (flowId) => {
  try {
    const result = await fetchApi(
      buildApiUrl(API_CONFIG.FLOWS.STOP, { flowId }),
      { method: 'POST' }
    );
    return result || {};
  } catch (err) {
    return {};
  }
};

/**
 * 启动连接流
 *
 * @param {string} flowId - 连接流ID
 * @returns {Promise<Object>} 启动结果
 */
export const startFlow = async (flowId) => {
  try {
    const result = await fetchApi(
      buildApiUrl(API_CONFIG.FLOWS.START, { flowId }),
      { method: 'POST' }
    );
    return result || {};
  } catch (err) {
    return {};
  }
};
