/**
 * ========================================
 * 流程工具函数库
 * ========================================
 * 
 * 提供连接器和连接流管理功能所需的核心工具函数
 * 包括节点ID生成、位置计算、验证、数据结构转换等功能
 */
import { CARRIER_OPTIONS, CARRIER_OPTIONS_OUTPUT, NODE_TYPE_TO_MAPPING_KEY } from "../pages/ConnectPlatform/FlowEditor/constants";

/**
 * 生成唯一的节点ID
 * @param {string} prefix - ID前缀，默认为'node'
 * @returns {string} 格式：prefix_timestamp_randomString
 * @example generateNodeId('trigger') => "trigger_1715000000000_abc123xyz"
 */
export const generateNodeId = (prefix = 'node') => {
  const timestamp = Date.now();
  const randomStr = Math.random().toString(36).substr(2, 9);
  return `${prefix}_${timestamp}_${randomStr}`;
};

/**
 * 生成唯一的连线ID
 * @returns {string} 格式：edge_timestamp_randomString
 */
export const generateEdgeId = () => {
  const timestamp = Date.now();
  const randomStr = Math.random().toString(36).substr(2, 9);
  return `edge_${timestamp}_${randomStr}`;
};

/**
 * 初始化节点位置（网格对齐）
 * 将节点位置对齐到10px的网格
 * @param {number} x - X坐标
 * @param {number} y - Y坐标
 * @returns {{x: number, y: number}} 对齐后的坐标
 */
export const getInitialNodePosition = (x = 100, y = 100) => ({
  x: Math.round(x / 10) * 10,
  y: Math.round(y / 10) * 10,
});

/**
 * 获取节点的所有上游节点（去重）
 * @param {string} nodeId - 当前节点ID
 * @param {Array} nodes - 节点列表
 * @param {Array} edges - 连线列表
 * @returns {Array} 上游节点数组
 */
export const getUpstreamNodes = (nodeId, nodes = [], edges = []) => {
  const visited = new Set();
  const result = [];

  function traverse(currentId) {
    if (visited.has(currentId)) return;
    visited.add(currentId);

    const incomingEdges = edges.filter(e => e.target === currentId);
    for (const edge of incomingEdges) {
      const upstreamNode = nodes.find(n => n.id === edge.source);
      if (upstreamNode && !visited.has(upstreamNode.id)) {
        result.push(upstreamNode);
        traverse(upstreamNode.id);
      }
    }
  }

  traverse(nodeId);
  return result;
};

/**
 * 递归处理对象形式的 properties（键值对结构），返回平铺的数组
 * @param {Object} propertiesObj - properties 对象（键值对形式）
 * @param {string} carrier - 参数载体类型（body/header/query）
 * @param {string} parentPath - 父级路径
 * @returns {Array} 平铺后的参数数组
 */
const flattenObjectProperties = (propertiesObj, carrier, parentPath = '') => {
  const result = [];

  if (!propertiesObj || typeof propertiesObj !== 'object') {
    return result;
  }

  Object.entries(propertiesObj).forEach(([paramName, paramDef]) => {
    const fullPath = parentPath ? `${parentPath}.${paramName}` : paramName;

    // 创建基本参数对象
    const paramObj = {
      paramName: paramName,
      paramType: paramDef.type || 'string',
      description: paramDef.description || '',
      carrier: carrier,
      paramPath: fullPath,
    };

    result.push(paramObj);

    // 如果有嵌套的 properties，递归处理
    if (paramDef.properties && typeof paramDef.properties === 'object') {
      const nestedParams = flattenObjectProperties(paramDef.properties, carrier, fullPath);
      result.push(...nestedParams);
    }
  });

  return result;
};

/**
 * 获取节点的输出参数
 * @param {Object} node - 节点对象
 * @returns {Array} 参数数组
 */
export const getNodeOutputParams = (node) => {
  if (!node) return [];

  const { type, data } = node;

  switch (type) {
    case 'trigger': {
      // Trigger 格式：{ header: { fieldName: { type, description } } }
      const allParams = [];
      let inputContract = data.inputContract || {};
      if (Array.isArray(data.inputContract)) {
        // 将数组转为带有 header、body、query的可递归对象
        const inputContractObject = transformMappingToNewFormat({mappingArray: data.inputContract});
        inputContract = extractPropertiesFromMappingData(inputContractObject);
      }

      // 合并 body、header、query 三种类型的参数
      CARRIER_OPTIONS.forEach(carrier => {
        const carrierData = inputContract[carrier];
        const flattenedParams = flattenObjectProperties(carrierData, carrier, 'input');
        allParams.push(...flattenedParams);
      });

      return allParams;
    }
    case 'connector': {
      // connector 节点的 outputParams 包含 body、header、query 两种类型的参数
      const outputParams = data.outputParams || {};
      const allParams = [];

      // 合并 body、header、query 三种类型的参数
      CARRIER_OPTIONS.forEach(carrier => {
        const carrierData = outputParams[carrier];
        if (carrierData?.properties) {
          // properties 可能是一个对象（键值对形式）或者数组
          const properties = carrierData.properties;
          const flattenedParams = flattenObjectProperties(properties, carrier, 'output');
          allParams.push(...flattenedParams);
        }
      });

      return allParams;
    }
    case 'exit':
      // exit 节点的 outputMapping
      return data.outputMapping || [];
    default:
      return [];
  }
};

/**
 * 获取节点的上游参数列表（按节点分组）
 * @param {string} nodeId - 当前节点ID
 * @param {Array} nodes - 节点列表
 * @param {Array} edges - 连线列表
 * @returns {Array} 按节点分组的参数列表 [{ nodeName, nodeId, params: [] }]
 */
export const getUpstreamParams = (nodeId, nodes = [], edges = []) => {
  const upstreamNodes = getUpstreamNodes(nodeId, nodes, edges);

  return upstreamNodes.map(node => {
    const outputParams = getNodeOutputParams(node);

    return {
      nodeName: node.data?.labelCn || node.id,
      nodeId: node.id,
      nodeType: node.type,
      params: outputParams,
    };
  });
};

/**
 * 从后端数据转换为React Flow渲染格式（进入编排页时调用）
 * 根据文档5.7节示例，将后端返回的orchestrationConfig转换为React Flow可用的nodes和edges
 * 
 * @param {Object} orchestrationConfig - 后端返回的编排配置
 * @returns {Object} { nodes, edges } React Flow格式的数据
 */
export const transformFromBackend = (orchestrationConfig) => {
  if (!orchestrationConfig) {
    return { nodes: [], edges: [] };
  }

  const nodes = extractPropertiesFromNodes(orchestrationConfig.nodes || []);

  const edges = orchestrationConfig.edges || [];

  return { nodes, edges };
};

/**
 * 根据 carrier 获取对应的目标对象
 * @param {Object} result - 包含 header/query/body 的结果对象
 * @param {string} carrier - 载体类型 (header/query/body)
 * @returns {Object} 目标对象
 */
const getMappingTargetByCarrier = (result, carrier) => {
  const carrierMap = {
    header: result.header.properties,
    query: result.query.properties,
    body: result.body.properties,
  };
  return carrierMap[carrier] || result.body.properties;
};

/**
 * 递归处理 mapping item 及其嵌套的 children
 * @param {Object} result - 包含 header/query/body 的结果对象
 * @param {Object} item - mapping 数组中的单个元素
 * @param {string} parentPath - 父级路径前缀
 * @param {Object} parentTarget - 父级目标对象，用于存放子节点
 */
const processMappingItem = (result, item, parentPath = '', parentTarget = null) => {
  const {
    sourceType,
    paramName,
    paramType,
    description,
    referencePath,
    paramValue,
    carrier,
    children
  } = item;

  if (!paramName || !carrier) {
    return;
  }

  // 计算当前字段的完整路径
  const currentPath = parentPath ? `${parentPath}.${paramName}` : paramName;

  let placeholder;
  if (sourceType === 'reference') {
    placeholder = '${$.node.' + (referencePath || currentPath) + '}';
  } else {
    placeholder = '${$.constant:' + (paramValue || '') + '}';
  }

  // 确定添加到的目标对象
  const target = parentTarget || getMappingTargetByCarrier(result, carrier);

  // 创建当前项的基本结构
  const itemEntry = {
    type: paramType || 'string',
    description: description || '',
    value: placeholder,
  };

  // 如果存在 children 且为数组或对象类型，递归处理子属性
  if (children && Array.isArray(children) && children.length > 0) {
    const isComplexType = paramType === 'object' || paramType === 'array';
    
    if (isComplexType) {
      // 创建子属性的容器
      itemEntry.properties = {};
      
      // 递归处理每个子节点，将结果添加到 properties 下
      children.forEach(childItem => {
        processMappingItem(result, childItem, currentPath, itemEntry.properties);
      });
    }
  }

  // 将当前项添加到目标对象
  target[paramName] = itemEntry;
};

/**
 * 转换 mapping 数组到新格式
 * 根据 sourceType 生成对应的占位符字符串参数
 *
 * @param {Object} options - 转换选项
 * @param {Array} options.mappingArray - mapping 数组，每个元素包含 sourceType, paramName, paramType, description, referencePath/paramValue
 * @returns {Object} 新格式的 mapping 对象 { header: { type: 'object', properties: {...} }, body: { type: 'object', properties: {...} }, query: { type: 'object', properties: {...} } }
 */
export const transformMappingToNewFormat = ({
  mappingArray = []
} = {}) => {
  const result = {
    header: { type: 'object', properties: {} },
    query: { type: 'object', properties: {} },
    body: { type: 'object', properties: {} },
  };

  mappingArray.forEach(item => {
    processMappingItem(result, item);
  });

  return result;
};

/**
 * 确保 carrier 数据被包装成 { type: 'object', properties: {...} } 格式
 * 如果数据已经是这种格式，则直接返回
 * 如果数据是普通对象，则包装成 { type: 'object', properties: {...} } 格式
 *
 * @param {Object} carrierData - carrier 数据
 * @returns {Object} 包装后的数据
 */
const wrapCarrierData = (carrierData) => {
  if (!carrierData || typeof carrierData !== 'object') {
    return { type: 'object', properties: {} };
  }

  // 如果已经是 { type: 'object', properties: {...} } 格式，直接返回
  if (carrierData.type === 'object' && carrierData.properties) {
    return carrierData;
  }

  // 否则包装成 { type: 'object', properties: carrierData } 格式
  return {
    type: 'object',
    properties: carrierData,
  };
};

/**
 * 处理 mapping 数据的 carrier 包装
 * 将 { header: {...}, query: {...}, body: {...} }
 * 转换为 { header: { type: 'object', properties: {...} }, ... }
 *
 * @param {Object} mappingData - 包含 header/query/body 的数据对象
 * @returns {Object} 包装后的数据对象
 */
const wrapMappingDataCarriers = (mappingData) => {
  if (!mappingData || typeof mappingData !== 'object') {
    return mappingData;
  }

  const result = {};

  // 处理 header, query, body
  ['header', 'query', 'body'].forEach(carrier => {
    if (mappingData[carrier]) {
      result[carrier] = wrapCarrierData(mappingData[carrier]);
    }
  });

  return result;
};

/**
 * @param {Array} nodes - React Flow节点列表
 * @param {Array} edges - React Flow连线列表
 * @returns {Object} orchestrationConfig 后端格式的编排配置
 */
export const transformToBackend = (nodes = [], edges = []) => {
  const transformedNodes = nodes.map(node => {
    const { data } = node;

    const transformedData = { ...data };
    delete transformedData.config;

    // 通过 NODE_TYPE_TO_MAPPING_KEY 获取需要处理的 mapping 字段（支持数组）
    const mappingKeys = NODE_TYPE_TO_MAPPING_KEY[node.type] || [];

    // 统一处理 mapping 字段（trigger/inputContract, connector/inputMapping/outputParams, exit/outputMapping）
    mappingKeys.forEach(mappingKey => {
      const mappingData = data[mappingKey];

      if (mappingData) {
        if (Array.isArray(mappingData)) {
          transformedData[mappingKey] = transformMappingToNewFormat({
            mappingArray: mappingData,
          });
        } else if (typeof mappingData === 'object') {
          transformedData[mappingKey] = wrapMappingDataCarriers(mappingData);
        }
      }
    });

    return {
      ...node,
      data: transformedData,
    };
  });

  const transformedEdges = edges.map(edge => ({
    ...edge,
    type: edge.type || 'smoothstep',
    data: edge.data || { businessType: 'default' },
  }));

  return {
    nodes: transformedNodes,
    edges: transformedEdges,
  };
};

/**
 * 解析 mapping value 为 sourceType、referencePath、paramValue
 * @param {string} value - 占位符值，如 ${$.node.xxx} 或 ${$.constant:xxx}
 * @returns {Object} { sourceType, referencePath, paramValue }
 */
const parseMappingValue = (value) => {
  if (!value || typeof value !== 'string') {
    return { sourceType: 'static', referencePath: '', paramValue: '' };
  }

  if (value.startsWith('${$.node.')) {
    return {
      sourceType: 'reference',
      referencePath: value.replace('${$.node.', '').replace('}', ''),
      paramValue: '',
    };
  }

  if (value.startsWith('${$.constant:')) {
    return {
      sourceType: 'static',
      referencePath: '',
      paramValue: value.replace('${$.constant:', '').replace('}', ''),
    };
  }

  return {
    sourceType: 'static',
    referencePath: '',
    paramValue: value,
  };
};

/**
 * 递归处理嵌套属性，生成 children 数组
 * 支持无限层级的嵌套结构
 * @param {Object} propertiesObj - 属性对象
 * @param {string} carrier - 载体类型 (header/query/body)
 * @returns {Array} 子参数数组
 */
const processNestedProperties = (propertiesObj, carrier) => {
  if (!propertiesObj || typeof propertiesObj !== 'object') {
    return [];
  }

  const children = [];

  Object.entries(propertiesObj).forEach(([paramName, paramObj]) => {
    if (!paramObj || typeof paramObj !== 'object') return;

    const processedValue = parseMappingValue(paramObj.value);
    const isComplex = paramObj.type === 'object' || paramObj.type === 'array';

    const item = {
      carrier,
      paramName,
      paramType: paramObj.type || 'string',
      sourceType: processedValue.sourceType,
      paramValue: processedValue.paramValue,
      description: paramObj.description || '',
      referencePath: processedValue.referencePath,
      children: [],
    };

    if (isComplex && paramObj.properties) {
      item.children = processNestedProperties(paramObj.properties, carrier);
    }

    children.push(item);
  });

  return children;
};

/**
 * 处理单个 carrier 下的字段列表
 * @param {Array} result - 结果数组
 * @param {string} carrier - 载体类型 (header/query/body)
 * @param {Object} fields - carrier 下的字段对象
 */
const processMappingCarrierFields = (result, carrier, fields) => {
  if (!fields || typeof fields !== 'object') return;

  Object.entries(fields).forEach(([paramName, paramObj]) => {
    if (!paramObj || typeof paramObj !== 'object') return;

    const processedValue = parseMappingValue(paramObj.value);
    const isComplex = paramObj.type === 'object' || paramObj.type === 'array';

    const item = {
      carrier,
      paramName,
      paramType: paramObj.type || 'string',
      sourceType: processedValue.sourceType,
      paramValue: processedValue.paramValue,
      description: paramObj.description || '',
      referencePath: processedValue.referencePath,
      children: [],
    };

    if (isComplex && paramObj.properties) {
      item.children = processNestedProperties(paramObj.properties, carrier);
    }

    result.push(item);
  });
};

/**
 * 将接口返回的嵌套 mapping 格式转换为 SchemaEditor 可用的数组格式（通用函数）
 * 接口返回格式：{ header: {}, query: {}, body: { paramName: { type, description, value, properties } } }
 * SchemaEditor 格式：[{ paramName, paramType, description, carrier, sourceType, referencePath, paramValue, children }]
 *
 * @param {Object} mappingObj - 接口返回的嵌套对象格式
 * @param {Array} carriers - 要处理的 carrier 列表，如 ['header', 'query', 'body']
 * @returns {Array} SchemaEditor 格式的数组
 */
const transformMappingFromNested = (mappingObj, carriers) => {
  const result = [];

  carriers.forEach(carrier => {
    if (mappingObj[carrier]) {
      processMappingCarrierFields(result, carrier, mappingObj[carrier]);
    }
  });

  return result;
};

/**
 * 将接口返回的嵌套 inputMapping 格式转换为 SchemaEditor 可用的数组格式
 * 接口返回格式：{ header: {}, query: {}, body: { paramName: { type, description, value, properties } } }
 * SchemaEditor 格式：[{ paramName, paramType, description, carrier, sourceType, referencePath, paramValue, children }]
 *
 * @param {Object} inputMappingObj - 接口返回的嵌套对象格式
 * @returns {Array} SchemaEditor 格式的数组
 */
export const transformInputMappingFromNested = (inputMappingObj = {}) => {
  return transformMappingFromNested(inputMappingObj, CARRIER_OPTIONS);
};

/**
 * 将接口返回的嵌套 outputMapping 格式转换为 SchemaEditor 可用的数组格式
 * 接口返回格式：{ header: {}, body: { paramName: { type, description, value, properties } } }
 * SchemaEditor 格式：[{ paramName, paramType, description, carrier, sourceType, referencePath, paramValue, children }]
 *
 * @param {Object} outputMappingObj - 接口返回的嵌套对象格式
 * @returns {Array} SchemaEditor 格式的数组
 */
export const transformOutputMappingFromNested = (outputMappingObj = {}) => {
  return transformMappingFromNested(outputMappingObj, CARRIER_OPTIONS_OUTPUT);
};

/**
 * 将 carrier 字段（header/query/body）的值从 { type: 'object', properties: {...} } 转换为 {...}
 * 用于处理后端返回的数据结构，将嵌套的 properties 层提取出来
 *
 * @param {Object} carrierData - carrier 数据，可能是 { type: 'object', properties: {...} } 或直接是 {...}
 * @returns {Object} 转换后的数据
 */
const extractPropertiesFromCarrier = (carrierData) => {
  if (!carrierData || typeof carrierData !== 'object') {
    return carrierData;
  }

  // 如果有 type 和 properties 字段，提取 properties
  if (carrierData.type === 'object' && carrierData.properties) {
    return carrierData.properties;
  }

  // 否则直接返回原数据
  return carrierData;
};

/**
 * 处理单个节点的 header/query/body 字段，提取 properties 层
 *
 * @param {Object} mappingData - 包含 header/query/body 的数据对象
 * @returns {Object} 转换后的数据对象
 */
export const extractPropertiesFromMappingData = (mappingData) => {
  if (!mappingData || typeof mappingData !== 'object') {
    return mappingData;
  }

  const result = {};

  // 处理 header, query, body
  ['header', 'query', 'body'].forEach(carrier => {
    if (mappingData[carrier]) {
      result[carrier] = extractPropertiesFromCarrier(mappingData[carrier]);
    }
  });

  return result;
};

/**
 * 将 nodes 中的 inputContract、inputMapping、outputParams、outputMapping
 * 的 header、body、query 值从 { type: 'object', properties: {...} } 转换为 {...}
 *
 * 后端返回格式：{ header: { type: 'object', properties: {...} }, query: {...} }
 * 转换后格式：{ header: {...}, query: {...} }
 *
 * @param {Array} nodes - React Flow 节点列表
 * @returns {Array} 转换后的节点列表
 */
export const extractPropertiesFromNodes = (nodes = []) => {
  if (!Array.isArray(nodes)) {
    return nodes;
  }

  return nodes.map(node => {
    const { type, data } = node;

    if (!data) {
      return node;
    }

    const transformedData = { ...data };

    // 处理 trigger 节点的 inputContract
    if (type === 'trigger' && data.inputContract) {
      transformedData.inputContract = extractPropertiesFromMappingData(data.inputContract);
    }

    // 处理 connector 节点的 inputMapping 和 outputParams
    if (type === 'connector' && data.inputMapping) {
      transformedData.inputMapping = extractPropertiesFromMappingData(data.inputMapping);
    }

    // 处理 exit 节点的 outputMapping
    if (type === 'exit' && data.outputMapping) {
      transformedData.outputMapping = extractPropertiesFromMappingData(data.outputMapping);
    }

    return {
      ...node,
      data: transformedData,
    };
  });
};

