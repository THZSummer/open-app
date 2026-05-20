/**
 * ========================================
 * 连接流管理 - API调用函数
 * ========================================
 *
 * 提供连接流的所有CRUD操作API调用
 */

import { API_CONFIG, buildApiUrl, fetchApi } from '../../../configs/web.config';
import {
  mockFetchFlowDetail,
  mockCreateFlow,
  mockUpdateFlow,
  mockPublishFlow,
  mockUnpublishFlow
} from './mock';

/**
 * 获取连接流详情
 *
 * @param {string} id - 连接流ID
 * @returns {Promise<Object>}
 */
export const fetchFlowDetail = async (id) => {
  try {
    // TODO: 临时使用Mock数据，后续需替换为真实API调用
    // const result = await fetchApi(buildApiUrl(API_CONFIG.FLOWS.DETAIL, { id }));
    const result = await mockFetchFlowDetail(id);
    return result || {};
  } catch (err) {
    console.error(`获取连接流详情失败: ${id}`, err);
    return { code: '500', message: '网络错误，请稍后重试' };
  }
};

/**
 * 创建连接流
 *
 * @param {Object} data - 连接流配置数据
 * @param {string} data.name - 连接流名称（必填）
 * @param {string} [data.description] - 连接流描述
 * @param {string} [data.type='automation'] - 流程类型
 * @param {number} [data.status=0] - 状态（0-草稿，1-已发布）
 * @param {Array} [data.nodes] - 节点列表
 * @param {Array} [data.edges] - 连线列表
 * @returns {Promise<Object>}
 */
export const createFlow = async (data) => {
  try {
    // TODO: 临时使用Mock数据，后续需替换为真实API调用
    // const result = await fetchApi(API_CONFIG.FLOWS.CREATE, {
    //   method: 'POST',
    //   body: JSON.stringify(data)
    // });
    const result = await mockCreateFlow(data);
    return result || {};
  } catch (err) {
    console.error('创建连接流失败', err);
    return { code: '500', message: '网络错误，请稍后重试' };
  }
};

/**
 * 更新连接流
 *
 * @param {string} id - 连接流ID
 * @param {Object} data - 更新后的连接流配置数据
 * @returns {Promise<Object>}
 */
export const updateFlow = async (id, data) => {
  try {
    // TODO: 临时使用Mock数据，后续需替换为真实API调用
    // const result = await fetchApi(buildApiUrl(API_CONFIG.FLOWS.UPDATE, { id }), {
    //   method: 'PUT',
    //   body: JSON.stringify(data)
    // });
    const result = await mockUpdateFlow(id, data);
    return result || {};
  } catch (err) {
    console.error(`更新连接流失败: ${id}`, err);
    return { code: '500', message: '网络错误，请稍后重试' };
  }
};

/**
 * 发布连接流
 *
 * @param {string} id - 连接流ID
 * @returns {Promise<Object>}
 */
export const publishFlow = async (id) => {
  try {
    // TODO: 临时使用Mock数据，后续需替换为真实API调用
    // const result = await fetchApi(buildApiUrl(API_CONFIG.FLOWS.PUBLISH, { id }), {
    //   method: 'POST'
    // });
    const result = await mockPublishFlow(id);
    return result || {};
  } catch (err) {
    console.error(`发布连接流失败: ${id}`, err);
    return { code: '500', message: '网络错误，请稍后重试' };
  }
};

/**
 * 取消发布连接流
 *
 * @param {string} id - 连接流ID
 * @returns {Promise<Object>}
 */
export const unpublishFlow = async (id) => {
  try {
    // TODO: 临时使用Mock数据，后续需替换为真实API调用
    // const result = await fetchApi(buildApiUrl(API_CONFIG.FLOWS.UNPUBLISH, { id }), {
    //   method: 'POST'
    // });
    const result = await mockUnpublishFlow(id);
    return result || {};
  } catch (err) {
    console.error(`取消发布连接流失败: ${id}`, err);
    return { code: '500', message: '网络错误，请稍后重试' };
  }
};
