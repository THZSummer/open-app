/**
 * ========================================
 * 连接流管理 - API调用函数
 * ========================================
 *
 * 提供连接流的详情获取、配置保存接口调用
 */

import { API_CONFIG, buildApiUrl, fetchApi } from '../../../configs/web.config';
import { extractPropertiesFromMappingData } from '../../../utils/flowUtils';
/**
 * 从连接器配置中提取需要更新的字段
 * 使用智能合并策略：保留用户配置，只补充缺失参数和更新元数据
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
    const newInputMapping = extractPropertiesFromMappingData(parsedConnectionConfig);
    const currentInputMapping = currentNode.data.inputMapping;

    if (!currentInputMapping) {
      updatedFields.inputMapping = newInputMapping;
    } else {
      const mergedInputMapping = smartMergeMapping(
        newInputMapping,
        currentInputMapping
      );
      if (mergedInputMapping) {
        updatedFields.inputMapping = mergedInputMapping;
      }
    }
  }

  if (parsedConnectionConfig.outputContract) {
    const newOutputParams = parsedConnectionConfig.outputContract || [];
    const currentOutputParams = currentNode.data.outputParams;

    if (!currentOutputParams) {
      updatedFields.outputParams = newOutputParams;
    } else {
      const mergedOutputParams = smartMergeOutputParams(
        newOutputParams,
        currentOutputParams
      );
      if (mergedOutputParams) {
        updatedFields.outputParams = mergedOutputParams;
      }
    }
  }

  return updatedFields;
};

/**
 * 智能合并 mapping 配置
 * 保留用户的值配置，只补充缺失参数和更新元数据
 *
 * @param {Object} newMapping - 接口返回的新配置
 * @param {Object} currentMapping - 当前节点的配置
 * @returns {Object|null} 合并后的配置，如果没有变化则返回 null
 */
const smartMergeMapping = (newMapping, currentMapping) => {
  const merged = JSON.parse(JSON.stringify(currentMapping));
  let hasChanges = false;

  for (const carrier of Object.keys(newMapping)) {
    if (!merged[carrier]) {
      merged[carrier] = {};
    }

    const newCarrierParams = newMapping[carrier] || {};
    const currentCarrierParams = merged[carrier] || {};

    for (const paramName of Object.keys(newCarrierParams)) {
      const newParam = newCarrierParams[paramName];

      if (!currentCarrierParams[paramName]) {
        merged[carrier][paramName] = newParam;
        hasChanges = true;
      } else {
        const currentParam = currentCarrierParams[paramName];

        if (
          currentParam.type !== newParam.type ||
          currentParam.description !== newParam.description
        ) {
          merged[carrier][paramName] = {
            ...currentParam,
            type: newParam.type,
            description: newParam.description,
          };
          hasChanges = true;
        }

        if (currentParam.properties && newParam.properties) {
          const mergedChildren = smartMergeNestedProperties(
            newParam.properties,
            currentParam.properties
          );
          if (mergedChildren) {
            merged[carrier][paramName].properties = mergedChildren;
            hasChanges = true;
          }
        } else if (newParam.properties && !currentParam.properties) {
          merged[carrier][paramName].properties = newParam.properties;
          hasChanges = true;
        }
      }
    }

    for (const paramName of Object.keys(currentCarrierParams)) {
      if (!newCarrierParams[paramName]) {
        delete merged[carrier][paramName];
        hasChanges = true;
      }
    }
  }

  return hasChanges ? merged : null;
};

/**
 * 智能合并嵌套属性配置
 *
 * @param {Object} newProperties - 接口返回的新属性
 * @param {Object} currentProperties - 当前节点的属性
 * @returns {Object|null} 合并后的属性，如果没有变化则返回 null
 */
const smartMergeNestedProperties = (newProperties, currentProperties) => {
  const merged = { ...currentProperties };
  let hasChanges = false;

  for (const paramName of Object.keys(newProperties)) {
    const newParam = newProperties[paramName];

    if (!currentProperties[paramName]) {
      merged[paramName] = newParam;
      hasChanges = true;
    } else {
      const currentParam = currentProperties[paramName];

      if (
        currentParam.type !== newParam.type ||
        currentParam.description !== newParam.description
      ) {
        merged[paramName] = {
          ...currentParam,
          type: newParam.type,
          description: newParam.description,
        };
        hasChanges = true;
      }
    }
  }

  for (const paramName of Object.keys(currentProperties)) {
    if (!newProperties[paramName]) {
      delete merged[paramName];
      hasChanges = true;
    }
  }

  return hasChanges ? merged : null;
};

/**
 * 智能合并 outputParams 配置
 *
 * @param {Array} newOutputParams - 接口返回的新配置
 * @param {Array} currentOutputParams - 当前节点的配置
 * @returns {Array|null} 合并后的配置，如果没有变化则返回 null
 */
const smartMergeOutputParams = (newOutputParams, currentOutputParams) => {
  if (JSON.stringify(newOutputParams) === JSON.stringify(currentOutputParams)) {
    return null;
  }
  return newOutputParams;
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
