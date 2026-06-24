/**
 * ========================================
 * 连接器管理 - API调用函数
 * ========================================
 *
 * 对齐 plan-api.md v7.0：
 *   - #2 查询连接器列表
 *   - #1 创建连接器
 *   - #4 更新连接器
 *   - #5 失效连接器 / #6 恢复连接器
 *   - #7 删除连接器
 *
 * 字段命名遵循 camelCase，状态枚举见 §1.8.1
 */

import { API_CONFIG, buildApiUrl, fetchApi } from '../../../configs/web.config';

/**
 * 查询连接器列表（#2）
 *
 * @param {Object} params - 查询参数对象
 * 包含以下字段：
 * - curPage: 当前页码（默认 1）
 * - pageSize: 每页数量（默认 20）
 * - keyword: 关键词模糊搜索（按中文名称）
 * - connectorType: 协议类型（1=HTTP）
 * - status: 连接器状态（1=有效不可用 / 2=有效可用 / 3=已失效）
 *
 * @returns {Promise<Object>} 列表响应
 */
export const fetchConnectorList = async (params = {}) => {
  try {
    const result = await fetchApi(API_CONFIG.CONNECTORS.LIST, { params });
    return result || {};
  } catch (err) {
    return {};
  }
};

/**
 * 创建连接器（#1）
 *
 * @param {Object} data - 连接器数据
 * 包含以下字段：
 * - nameCn: 中文名称
 * - nameEn: 英文名称
 * - descriptionCn: 中文描述
 * - descriptionEn: 英文描述
 * - connectorType: 协议类型（1=HTTP）
 *
 * @returns {Promise<Object>} 创建结果
 */
export const createConnector = async (data) => {
  try {
    const result = await fetchApi(API_CONFIG.CONNECTORS.CREATE, {
      method: 'POST',
      body: JSON.stringify(data),
    });
    return result || {};
  } catch (err) {
    return {};
  }
};

/**
 * 更新连接器（#4）
 *
 * @param {string} connectorId - 连接器 ID
 * @param {Object} data - 更新数据（nameCn / nameEn / descriptionCn / descriptionEn 可选）
 * @returns {Promise<Object>} 更新结果
 */
export const updateConnector = async (connectorId, data) => {
  try {
    const result = await fetchApi(
      buildApiUrl(API_CONFIG.CONNECTORS.UPDATE, { connectorId }),
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
 * 失效连接器（#5）
 *
 * @param {string} connectorId - 连接器 ID
 * @returns {Promise<Object>} 失效结果
 */
export const disableConnector = async (connectorId) => {
  try {
    const result = await fetchApi(
      buildApiUrl(API_CONFIG.CONNECTORS.INVALIDATE, { connectorId }),
      { method: 'PUT' }
    );
    return result || {};
  } catch (err) {
    return {};
  }
};

/**
 * 恢复连接器（#6）
 *
 * @param {string} connectorId - 连接器 ID
 * @returns {Promise<Object>} 恢复结果
 */
export const restoreConnector = async (connectorId) => {
  try {
    const result = await fetchApi(
      buildApiUrl(API_CONFIG.CONNECTORS.RECOVER, { connectorId }),
      { method: 'PUT' }
    );
    return result || {};
  } catch (err) {
    return {};
  }
};

/**
 * 删除连接器（#7）
 *
 * @param {string} connectorId - 连接器 ID
 * @returns {Promise<Object>} 删除结果
 */
export const deleteConnector = async (connectorId) => {
  try {
    const result = await fetchApi(
      buildApiUrl(API_CONFIG.CONNECTORS.DELETE, { connectorId }),
      { method: 'DELETE' }
    );
    return result || {};
  } catch (err) {
    return {};
  }
};
