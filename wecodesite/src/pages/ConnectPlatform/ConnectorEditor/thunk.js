/**
 * ========================================
 * 连接器编辑页 - API调用函数
 * ========================================
 *
 * 提供连接器编辑的CRUD操作API调用
 */

import { API_CONFIG, buildApiUrl, fetchApi } from '../../../configs/web.config';
import { DEFAULT_API_CONFIG } from './constants';

/**
 * 将jsonSchemaObjectDef格式转换为SchemaEditor参数数组
 * @param {Object} jsonSchema - jsonSchemaObjectDef格式的对象
 * @param {string} defaultCarrier - 默认carrier值
 * @returns {Array} SchemaEditor格式的参数数组
 */

/**
 * 处理契约配置的辅助方法
 * 根据载体类型将契约中的参数转换并合并到目标数组
 * @param {Object} contract - 输入/输出契约对象
 * @param {Array} targetArray - 目标数组（requestSchema 或 responseSchema）
 * @param {Array} carriers - 支持的载体类型数组，如 ['header', 'query', 'body']
 * @param {Object} extraFields - 需要添加的额外字段
 */
const processContract = (contract, targetArray, carriers, extraFields = {}) => {
  if (!contract) return targetArray;

  let result = [...targetArray];

  // 遍历所有支持的载体类型
  carriers.forEach(carrier => {
    if (contract[carrier]) {
      const params = transformJsonSchemaToParams(contract[carrier], carrier);

      // 如果有额外字段需要添加，则为每个参数添加
      if (Object.keys(extraFields).length > 0) {
        params.forEach(param => {
          result.push({
            ...param,
            ...extraFields,
          });
        });
      } else {
        result = [...result, ...params];
      }
    }
  });

  return result;
};

/**
 * 从 Schema 构建契约配置的辅助方法
 * 根据载体类型从 Schema 中提取参数并构建契约
 * @param {Array} schema - Schema 数组（requestSchema 或 responseSchema）
 * @param {Array} carriers - 支持的载体类型数组，如 ['header', 'query', 'body']
 * @returns {Object|null} 契约对象，如果没有参数则返回 null
 */
const buildContractFromSchema = (schema, carriers) => {
  if (!schema || schema.length === 0) return null;

  const result = {};
  let hasAny = false;

  // 遍历所有支持的载体类型
  carriers.forEach(carrier => {
    // 过滤出该载体类型的参数
    const filteredParams = schema.filter(p => {
      // 对于 body 类型，如果没有明确指定 carrier，也归为 body
      if (carrier === 'body') {
        return p.carrier === 'body' || !p.carrier;
      }
      return p.carrier === carrier;
    });

    // 如果有该载体类型的参数，则添加到结果中
    if (filteredParams.length > 0) {
      result[carrier] = transformParamsToJsonSchema(filteredParams);
      hasAny = true;
    }
  });

  return hasAny ? result : null;
};

export const transformJsonSchemaToParams = (jsonSchema, defaultCarrier = 'body') => {
  // 参数校验：jsonSchema 必须存在且包含 properties 字段
  if (!jsonSchema || !jsonSchema.properties) {
    return [];
  }

  // 初始化参数数组
  const params = [];

  // 遍历 jsonSchema 中的所有属性
  Object.entries(jsonSchema.properties).forEach(([key, value]) => {
    // 判断是否为复杂类型（对象或数组），复杂类型需要递归处理
    const isComplex = value.type === 'object' || value.type === 'array';
    let paramChildren = [];

    // 如果是复杂类型且有子属性，则递归转换子属性
    if (isComplex && value.properties) {
      paramChildren = transformJsonSchemaToParams(value, defaultCarrier);
    }

    // 将每个属性转换为 SchemaEditor 参数格式
    params.push({
      paramName: key,                    // 参数名称
      paramType: value.type,             // 参数类型（string, object, array 等）
      description: value.description || '', // 参数描述
      carrier: defaultCarrier,            // 参数载体（header, body, query）
      children: paramChildren,           // 子参数（复杂类型才有）
    });
  });

  return params;
};

/**
 * 将后端返回的authConfig.fields转换为SchemaEditor参数格式
 * @param {Array} fields - authConfig.fields数组
 * @returns {Array} SchemaEditor格式的参数数组
 */
export const transformAuthFieldsToParams = (fields) => {
  // 参数校验：fields 必须存在且为数组
  if (!fields || !Array.isArray(fields)) {
    return [];
  }

  // 将后端认证字段数组转换为 SchemaEditor 参数格式
  return fields.map(field => ({
    paramName: field.name || '',      // 认证参数字段名
    paramType: 'string',              // 认证参数类型固定为 string
    fieldName: field.fieldName || '', // 字段名称映射
    carrier: field.carrier || 'header', // 参数载体，默认 header
    children: [],                     // 认证参数无子参数
  }));
};

/**
 * 将后端返回的4.3格式数据转换为表单可编辑的apiConfig格式
 * @param {Object} schemaData - 后端返回的数据
 * @returns {Object} 表单可编辑的apiConfig格式
 */
export const transformFromSchemaFormat = (schemaData) => {
  // 数据校验：如果 schemaData 为空，返回默认配置
  if (!schemaData) {
    return { ...DEFAULT_API_CONFIG };
  }

  // 提取协议配置（HTTP 方法和地址）
  const result = {
    protocolType: schemaData.protocolConfig?.method || '',
    protocolAddress: schemaData.protocolConfig?.url || '',

    // 提取认证配置
    authType: schemaData.authConfig?.type || '',
    authRequestSchema: transformAuthFieldsToParams(schemaData.authConfig?.fields || []),

    // 初始化请求和响应 Schema（后续会根据 inputContract/outputContract 填充）
    headerSchema: [],
    requestSchema: [],
    responseSchema: [],
  };

  // 处理入参配置（inputContract），支持 header、query、body 三种载体
  result.requestSchema = processContract(
    schemaData.inputContract,
    result.requestSchema,
    ['header', 'query', 'body']
  );

  // 处理出参配置（outputContract），支持 header、body 两种载体
  result.responseSchema = processContract(
    schemaData.outputContract,
    result.responseSchema,
    ['header', 'body']
  );

  return result;
};

/**
 * 将SchemaEditor参数递归转换为jsonSchema格式
 * @param {Object} param - 单个参数对象
 * @returns {Object} jsonSchema格式的字段定义
 */
export const transformParamToJsonSchemaField = (param) => {
  // 判断参数是否为复杂类型（对象或数组）
  const isComplex = param.paramType === 'object' || param.paramType === 'array';

  // 构建基本的字段定义
  const fieldDef = {
    type: param.paramType,              // 字段类型
    description: param.description,      // 字段描述
  };

  // 如果是复杂类型且有子参数，则递归处理子参数
  if (isComplex && param.children && param.children.length > 0) {
    const properties = {};

    // 遍历所有子参数
    param.children.forEach(child => {
      // 只有有参数名的才处理
      if (child.paramName) {
        // 递归转换子参数为 JSON Schema 格式
        properties[child.paramName] = transformParamToJsonSchemaField(child);
      }
    });

    // 将子属性添加到字段定义中
    fieldDef.properties = properties;
  }

  return fieldDef;
};

/**
 * 将SchemaEditor参数数组转换为jsonSchemaObjectDef格式
 * @param {Array} params - SchemaEditor收集的参数数组
 * @returns {Object} jsonSchemaObjectDef格式的对象
 */
export const transformParamsToJsonSchema = (params) => {
  // 参数校验：参数数组不能为空
  if (!params || params.length === 0) {
    return null;
  }

  // 初始化属性对象
  const properties = {};

  // 遍历所有参数，转换为 JSON Schema 属性
  params.forEach(param => {
    if (param.paramName) {
      // 将参数转换为 JSON Schema 字段格式
      properties[param.paramName] = transformParamToJsonSchemaField(param);
    }
  });

  // 构建最终的 JSON Schema 对象
  const result = {
    type: 'object',      // 顶层类型为对象
    properties,           // 对象的所有属性
  };

  return result;
};

/**
 * 将SchemaEditor认证参数数组转换为authConfig.fields格式
 * @param {Array} authParams - 认证参数数组
 * @returns {Array} authConfig.fields格式的数组
 */
export const transformAuthFields = (authParams) => {
  // 参数校验：认证参数数组不能为空
  if (!authParams || authParams.length === 0) {
    return [];
  }

  // 过滤有效的认证参数，并转换为后端格式
  return authParams
    .filter(param => param.paramName)  // 只保留有参数名的
    .map(param => ({
      name: param.paramName,              // 认证参数字段名
      carrier: param.carrier || 'header', // 载体类型，默认 header
      fieldName: param.fieldName,         // 字段名称映射
    }));
};

/**
 * 将连接器配置转换为连接流编排页面的入参映射格式
 * - 将inputContract中的参数转换为可编辑的inputMapping格式
 * - 添加sourceType字段，默认值为'static'
 * - 保持carrier字段不可编辑
 * @param {Object} connectorConfig - 连接器配置
 * @returns {Array} 入参映射格式的参数数组
 */
export const transformConnectorConfigToInputMapping = (connectorConfig) => {
  // 参数校验：connectorConfig 必须存在且包含 inputContract
  if (!connectorConfig || !connectorConfig.inputContract) {
    return [];
  }

  // 定义连接流需要的额外字段
  const extraFields = {
    sourceType: 'static',    // 数据源类型默认为静态
    paramValue: '',          // 静态值初始化为空
    referencePath: '',       // 引用路径初始化为空
  };

  // 处理入参配置，支持 header、query、body 三种载体，并添加额外字段
  return processContract(
    connectorConfig.inputContract,
    [],
    ['header', 'query', 'body'],
    extraFields
  );
};

/**
 * 将apiConfig转换为符合文档4.3示例的数据格式
 * @param {Object} apiConfig - 当前表单收集的apiConfig
 * @returns {Object} 符合文档要求的数据格式
 */
export const transformToSchemaFormat = (apiConfig) => {
  // 构建协议配置部分
  const result = {
    protocol: 'HTTP',                             // 协议类型固定为 HTTP
    protocolConfig: {
      url: apiConfig.protocolAddress || '',       // 接口地址
      method: apiConfig.protocolType || '',       // HTTP 方法
    },
  };

  // 处理认证配置（如果选择了认证方式）
  if (apiConfig.authType) {
    result.authConfig = {
      type: apiConfig.authType,                    // 认证类型（SOA/APIG）
      fields: transformAuthFields(apiConfig.authRequestSchema || []), // 认证参数字段
    };
  }

  // 从请求 Schema 构建 inputContract（支持 header、query、body 三种载体）
  const inputContract = buildContractFromSchema(
    apiConfig.requestSchema,
    ['header', 'query', 'body']
  );
  if (inputContract) {
    result.inputContract = inputContract;
  }

  // 从响应 Schema 构建 outputContract（支持 header、body 两种载体）
  const outputContract = buildContractFromSchema(
    apiConfig.responseSchema,
    ['header', 'body']
  );
  if (outputContract) {
    result.outputContract = outputContract;
  }

  return result;
};

/**
 * 获取连接器配置
 *
 * @param {string} connectorId - 连接器ID
 * @returns {Promise<Object>} 连接器配置信息
 */
export const fetchConnectorConfig = async (connectorId) => {
  try {
    // 构建 API 请求 URL 并获取连接器配置
    const result = await fetchApi(
      buildApiUrl(API_CONFIG.CONNECTORS.CONFIG, { connectorId })
    );

    // 返回结果或空对象
    return result || {};
  } catch (err) {
    // 捕获错误，返回空对象避免组件崩溃
    return {};
  }
};

/**
 * 保存连接器配置（编辑即生效）
 *
 * @param {string} connectorId - 连接器ID
 * @param {Object} config - 连接器配置数据
 * @param {Object} config.connectionConfig - 连接配置
 * @returns {Promise<Object>} 保存结果
 */
export const saveConnectorConfig = async (connectorId, config) => {
  try {
    // 构建 API 请求 URL，使用 PUT 方法保存连接器配置
    const result = await fetchApi(
      buildApiUrl(API_CONFIG.CONNECTORS.CONFIG_UPDATE, { connectorId }),
      {
        method: 'PUT',                          // 使用 PUT 方法更新数据
        body: JSON.stringify(config)             // 将配置对象序列化为 JSON 字符串
      }
    );

    // 返回保存结果
    return result || {};
  } catch (err) {
    // 记录错误日志
    console.error('保存连接器配置失败:', err);

    // 返回空对象，避免组件崩溃
    return {};
  }
};
