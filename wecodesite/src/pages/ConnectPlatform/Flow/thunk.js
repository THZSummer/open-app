/**
 * ========================================
 * 连接流列表 - API调用函数
 * ========================================
 *
 * 提供连接流列表相关的API调用
 */

import { API_CONFIG, buildApiUrl, fetchApi } from '../../../configs/web.config';
import {
  mockFetchFlowList,
  mockDeleteFlow,
} from './mock';

/**
 * 获取连接流列表
 *
 * @param {Object} params - 查询参数
 * @param {string} [params.keyword] - 搜索关键词
 * @param {string} [params.type] - 流程类型筛选
 * @param {number} [params.curPage=1] - 当前页码
 * @param {number} [params.pageSize=10] - 每页条数
 * @returns {Promise<Object>}
 */
export const fetchFlowList = async (params = {}) => {
  try {
    // TODO: 临时使用Mock数据，后续需替换为真实API调用
    // const result = await fetchApi(API_CONFIG.FLOWS.LIST, {
    //   method: 'GET',
    //   params
    // });
    const result = await mockFetchFlowList(params);
    return result || {};
  } catch (err) {
    console.error('获取连接流列表失败', err);
    return { code: '500', message: '网络错误，请稍后重试' };
  }
};

/**
 * 删除连接流
 *
 * @param {string} id - 连接流ID
 * @returns {Promise<Object>}
 */
export const deleteFlow = async (id) => {
  try {
    // TODO: 临时使用Mock数据，后续需替换为真实API调用
    // const result = await fetchApi(buildApiUrl(API_CONFIG.FLOWS.DELETE, { id }), {
    //   method: 'DELETE'
    // });
    const result = await mockDeleteFlow(id);
    return result || {};
  } catch (err) {
    console.error(`删除连接流失败: ${id}`, err);
    return { code: '500', message: '网络错误，请稍后重试' };
  }
};
