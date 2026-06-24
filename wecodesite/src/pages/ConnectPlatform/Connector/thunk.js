/**
 * ========================================
 * 连接器管理 - API调用函数
 * ========================================
 *
 * 提供连接器列表的API调用
 */

import { API_CONFIG, buildApiUrl, fetchApi } from '../../../configs/web.config';

/**
 * 获取连接器列表
 *
 * @param {Object} params - 查询参数
 * @param {string} [params.keyword] - 搜索关键词
 * @param {number} [params.curPage] - 当前页码
 * @param {number} [params.pageSize] - 每页条数
 * @param {number} [params.connectorType] - 连接器类型筛选
 *
 * @returns {Promise<Object>} 连接器列表
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
 * 删除连接器
 *
 * @param {string} connectorId - 连接器ID
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

/**
 * 失效连接器
 *
 * @param {string} connectorId - 连接器ID
 * @returns {Promise<Object>} 失效结果
 */
export const disableConnector = async (connectorId) => {
  try {
    const result = await fetchApi(
      buildApiUrl(API_CONFIG.CONNECTORS.UPDATE, { connectorId }),
      {
        method: 'PUT',
        body: JSON.stringify({ status: 0 })
      }
    );
    return result || {};
  } catch (err) {
    return {};
  }
};

/**
 * 创建连接器
 *
 * @param {Object} data - 连接器配置数据
 * @param {string} data.nameCn - 连接器中文名称
 * @param {string} data.nameEn - 连接器英文名称
 * @param {string} data.descriptionCn - 连接器中文描述
 * @param {string} data.descriptionEn - 连接器英文描述
 * @returns {Promise<Object>} 创建结果
 */
export const createConnector = async (data) => {
  try {
    const result = await fetchApi(API_CONFIG.CONNECTORS.CREATE, {
      method: 'POST',
      body: JSON.stringify(data)
    });
    return result || {};
  } catch (err) {
    return {};
  }
};

/**
 * 更新连接器
 *
 * @param {string} connectorId - 连接器ID
 * @param {Object} data - 更新数据
 * @param {string} data.nameCn - 连接器中文名称
 * @param {string} data.nameEn - 连接器英文名称
 * @param {string} data.descriptionCn - 连接器中文描述
 * @param {string} data.descriptionEn - 连接器英文描述
 * @returns {Promise<Object>} 更新结果
 */
export const updateConnector = async (connectorId, data) => {
  try {
    const result = await fetchApi(
      buildApiUrl(API_CONFIG.CONNECTORS.UPDATE, { connectorId }),
      {
        method: 'PUT',
        body: JSON.stringify(data)
      }
    );
    return result || {};
  } catch (err) {
    return {};
  }
};
