/**
 * ========================================
 * 连接流管理 - API调用函数
 * ========================================
 *
 * 提供连接流的详情获取、配置保存接口调用
 */

import { API_CONFIG, buildApiUrl, fetchApi } from '../../../configs/web.config';

/**
 * 获取连接流详情
 *
 * @param {string} flowId - 连接流ID
 * @returns {Promise<Object>} 连接流详情
 */
export const fetchFlowDetail = async (flowId) => {
  try {
    const result = await fetchApi(
      buildApiUrl(API_CONFIG.FLOWS.CONFIG, { flowId })
    );
    return result || {};
  } catch (err) {
    return {};
  }
};

/**
 * 保存连接流编排配置
 *
 * @param {string} flowId - 连接流ID
 * @param {Object} config - 编排配置数据
 * @param {string} config.nameCn - 连接流中文名称
 * @param {string} config.nameEn - 连接流英文名称
 * @param {string} [config.descriptionCn] - 连接流中文描述
 * @param {string} [config.descriptionEn] - 连接流英文描述
 * @param {Object} config.orchestrationConfig - 编排配置
 * @param {Array} config.orchestrationConfig.nodes - 节点列表
 * @param {Array} config.orchestrationConfig.edges - 连线列表
 * @returns {Promise<Object>} 保存结果
 */
export const saveFlowConfig = async (flowId, config) => {
  try {
    const result = await fetchApi(
      buildApiUrl(API_CONFIG.FLOWS.CONFIG, { flowId }),
      {
        method: 'PUT',
        body: JSON.stringify(config)
      }
    );
    return result || {};
  } catch (err) {
    return {};
  }
};
