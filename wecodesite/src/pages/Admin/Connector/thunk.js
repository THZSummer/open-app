/**
 * ========================================
 * 连接器管理 - API调用函数
 * ========================================
 *
 * 提供连接器列表的API调用
 */

import { API_CONFIG, buildApiUrl, fetchApi } from '../../../configs/web.config';
import {
  mockFetchConnectorList,
  mockDeleteConnector,
} from './mock';

/**
 * 获取连接器列表
 *
 * @param {Object} params - 查询参数
 * @param {string} [params.keyword] - 搜索关键词
 * @param {number} [params.curPage=1] - 当前页码
 * @param {number} [params.pageSize=10] - 每页条数
 * @param {number} [params.status] - 状态筛选（0-禁用，1-启用）
 *
 * @returns {Promise<Object>}
 */
export const fetchConnectorList = async (params = {}) => {
  try {
    const result = await mockFetchConnectorList(params);
    return result || {};
  } catch (err) {
    console.error('获取连接器列表失败', err);
    return { code: '500', message: '网络错误，请稍后重试' };
  }
};

/**
 * 删除连接器
 *
 * @param {string} id - 连接器ID
 * @returns {Promise<Object>}
 */
export const deleteConnector = async (id) => {
  try {
    const result = await mockDeleteConnector(id);
    return result || {};
  } catch (err) {
    console.error(`删除连接器失败: ${id}`, err);
    return { code: '500', message: '网络错误，请稍后重试' };
  }
};
