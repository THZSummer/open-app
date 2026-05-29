/**
 * ========================================
 * 连接流管理 - API调用函数
 * ========================================
 *
 * 提供连接流的详情获取、配置保存接口调用
 */

import { API_CONFIG, buildApiUrl, fetchApi } from '../../../configs/web.config';
import { transformConnectorConfigToInputMapping } from '../ConnectorEditor/thunk';

/**
 * 比较两个值是否发生变化
 *
 * @param {*} newValue - 新值
 * @param {*} currentValue - 当前值
 * @returns {boolean} 是否发生变化
 */
export const hasChanged = (newValue, currentValue) => {
  return JSON.stringify(newValue) !== JSON.stringify(currentValue);
};

/**
 * 从连接器配置中提取需要更新的字段
 * 根据配置内容判断 inputMapping 和 outputParams 是否需要更新
 * 注意：只有在字段为空时才进行初始化，不会覆盖已存在的用户配置
 *
 * @param {Object} params - 函数参数
 * @param {Object} params.parsedConnectionConfig - 解析后的连接器配置
 * @param {Object} params.currentNode - 当前节点数据
 * @returns {Object} 需要更新的字段对象
 */
export const extractUpdatedFields = ({
  parsedConnectionConfig,
  currentNode,
}) => {
  const updatedFields = {};

  if (parsedConnectionConfig.inputContract) {
    const newInputMapping = transformConnectorConfigToInputMapping(parsedConnectionConfig);
    const currentInputMapping = currentNode.data.inputMapping;

    /** 仅当 inputMapping 为空时才初始化，不会覆盖已存在的配置 */
    if (!currentInputMapping) {
      updatedFields.inputMapping = newInputMapping;
    }
  }

  if (parsedConnectionConfig.outputContract) {
    const newOutputParams = parsedConnectionConfig.outputContract || [];
    const currentOutputParams = currentNode.data.outputParams;

    /** 仅当 outputParams 为空时才初始化，不会覆盖已存在的配置 */
    if (!currentOutputParams) {
      updatedFields.outputParams = newOutputParams;
    }
  }

  return updatedFields;
};

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
