/**
 * ========================================
 * 连接器编辑页 - API调用函数
 * ========================================
 *
 * 提供连接器编辑的CRUD操作API调用
 */

import { API_CONFIG, buildApiUrl, fetchApi } from '../../../configs/web.config';
import {
  mockFetchConnectorDetail,
  mockCreateConnector,
  mockUpdateConnector,
} from './mock';

/**
 * 获取连接器详情
 *
 * @param {string} id - 连接器ID
 * @returns {Promise<Object>}
 */
export const fetchConnectorDetail = async (id) => {
  try {
    const result = await mockFetchConnectorDetail(id);
    return result || {};
  } catch (err) {
    console.error(`获取连接器详情失败: ${id}`, err);
    return { code: '500', message: '网络错误，请稍后重试' };
  }
};

/**
 * 创建连接器
 *
 * @param {Object} data - 连接器配置数据
 * @returns {Promise<Object>}
 */
export const createConnector = async (data) => {
  try {
    const result = await mockCreateConnector(data);
    return result || {};
  } catch (err) {
    console.error('创建连接器失败', err);
    return { code: '500', message: '网络错误，请稍后重试' };
  }
};

/**
 * 更新连接器
 *
 * @param {string} id - 连接器ID
 * @param {Object} data - 更新后的连接器配置数据
 * @returns {Promise<Object>}
 */
export const updateConnector = async (id, data) => {
  try {
    const result = await mockUpdateConnector(id, data);
    return result || {};
  } catch (err) {
    console.error(`更新连接器失败: ${id}`, err);
    return { code: '500', message: '网络错误，请稍后重试' };
  }
};
