/**
 * ========================================
 * 连接流编辑器 V2 - API 调用函数
 * ========================================
 *
 * 对齐 plan-api.md v7.0：
 *   - #29 查询连接流版本列表
 *   - #30 查询连接流版本详情
 *   - #31 更新连接流版本（保存草稿）
 *   - #32 发布连接流版本
 *   - #28 创建草稿版本 / #33 复制版本到草稿
 *   - #34 失效版本 / #35 恢复版本 / #37 撤回审批 / #36 删除版本
 *   - #38 催办审批
 *   - #51 调试连接流版本（代理）
 *   - #2 查询连接器列表（status=2 有效可用）
 *   - #9 查询连接器版本列表（status=2 已发布）
 *   - #10 查询连接器版本详情（取入参）
 *
 * 应用级配置（超时上限/限流上限/记录条数上限/日志开关）
 * 后端在 plan-api.md §3.9a 由 market-server Property 提供，
 * 暂未对接独立接口，沿用前端默认配置。
 */

import { API_CONFIG, buildApiUrl, fetchApi } from '../../../configs/web.config';
import {
  buildSortedVersionSummaries,
  buildVersionSummary,
  normalizeJsonConfig,
} from '../../../utils/common';
import {
  buildHttpCarrierParams,
  buildJsonObjectFromParams,
  hasObjectKeys,
  parseJsonObjectToParams,
} from '../../../utils/flowUtilsV2';
import { stripScriptEditorTypes } from './utils';

/**
 * 应用级配置默认值（与 plan-api.md §3.9a #54~#55 字段对齐）
 */
const DEFAULT_APP_CONFIG = {
  maxTimeoutMs: 30000,
  maxQps: 1000,
  maxConcurrency: 100,
  maxRecords: 10000,
  logSwitch: 1,
};

/**
 * 获取应用级配置（暂用前端默认值，后端接口就绪后切换）
 *
 * @returns {Promise<Object>} 应用级配置
 */
export const fetchAppConfig = async () => {
  return { code: '200', data: { ...DEFAULT_APP_CONFIG } };
};

/**
 * 查询连接流版本列表（#29）
 *
 * @param {string} flowId - 连接流 ID
 * @returns {Promise<Object>} 版本列表响应
 */
export const fetchVersionList = async (flowId) => {
  try {
    const result = await fetchApi(
      buildApiUrl(API_CONFIG.FLOWS.VERSIONS_LIST, { flowId })
    );
    if (result?.code !== '200') return result || {};

    // 将后端数据映射为 UI 所需摘要结构（按创建时间倒序）
    const list = buildSortedVersionSummaries(result.data || []);
    return { code: '200', data: list };
  } catch {
    return {};
  }
};

/**
 * 将连接器 JSON Schema 转为连接流节点入参列表
 *
 * @param {Object} jsonSchema - 后端返回的 JSON Schema 对象
 * @returns {Array} 节点入参列表
 */
const transformJsonSchemaToFlowParams = (jsonSchema) => {
  // 复用公共 jsonObjectDef 转参数树方法，保留当前函数名供连接器入参回显使用
  return parseJsonObjectToParams({ jsonObject: jsonSchema });
};

/**
 * 获取连接器版本的首个认证方式
 *
 * @param {Array} authConfigs - 后端返回的认证配置列表
 * @returns {string} 认证方式标识
 */
const getFirstAuthType = (authConfigs) => {
  // 认证配置不是数组时返回空字符串
  if (!Array.isArray(authConfigs)) return '';

  // 当前连接流节点只展示单个认证方式，取接口返回的首个认证类型
  return authConfigs.find(item => item?.type)?.type || '';
};

/**
 * 从 JSON Schema 中提取认证参数
 *
 * @param {Object} jsonSchema - 后端返回的 JSON Schema 对象
 * @param {string} carrier - 参数位置
 * @param {boolean} showMappingValue - 是否展示映射值
 * @returns {Array} 认证参数列表
 */
const extractAuthParamsByCarrier = (jsonSchema, carrier, showMappingValue) => {
  // 缺少 properties 时，返回空列表
  if (!jsonSchema?.properties) return [];

  // 将认证参数转换为连接流认证展示所需结构
  return Object.entries(jsonSchema.properties).map(([name, schema]) => ({
    name,
    type: schema?.type || 'string',
    carrier,
    mappingValue: showMappingValue ? (schema?.value || name) : '',
  }));
};

/**
 * 转换普通认证配置
 *
 * @param {Object} authConfig - 单个认证配置
 * @returns {Array} 认证参数列表
 */
const transformCommonAuthParams = (authConfig) => {
  // Cookie 不展示映射值，只展示来源和值输入
  const isCookie = authConfig?.type === 'Cookie';
  // 合并 header / query / body 三种位置的认证参数
  return [
    ...extractAuthParamsByCarrier(authConfig?.header, 'header', !isCookie),
    ...extractAuthParamsByCarrier(authConfig?.query, 'query', !isCookie),
    ...extractAuthParamsByCarrier(authConfig?.body, 'body', !isCookie),
  ];
};

/**
 * 转换数字签名认证配置
 *
 * @param {Object} authConfig - 数字签名认证配置
 * @returns {Array} 数字签名展示参数列表
 */
const transformSignatureAuthParams = (authConfig) => {
  // 数字签名仅展示 header 固定映射值，不展示签名密钥
  return extractAuthParamsByCarrier(authConfig?.header, 'header', true);
};

/**
 * 将连接器认证配置转换为连接流节点可展示结构
 *
 * @param {Array} authConfigs - 后端返回的认证配置列表
 * @returns {Array} 认证配置展示列表
 */
const transformAuthConfigsToFlowItems = (authConfigs) => {
  // 非数组表示无认证配置
  if (!Array.isArray(authConfigs)) return [];

  // 按认证类型转换参数展示结构
  return authConfigs
    .filter(item => item?.type)
    .map((item) => ({
      type: item.type,
      params: item.type === 'SIGNATURE'
        ? transformSignatureAuthParams(item)
        : transformCommonAuthParams(item),
    }))
    .filter(item => item.params.length > 0);
};

/**
 * 查询连接流版本详情（#30）
 *
 * @param {Object} params - 参数对象
 * 包含以下字段：
 * - flowId: 连接流 ID
 * - versionId: 版本 ID
 *
 * @returns {Promise<Object>} 版本详情响应
 */
export const fetchVersionDetail = async (params) => {
  // 解构传入对象中需要使用的参数
  const { flowId, versionId } = params;

  try {
    const result = await fetchApi(
      buildApiUrl(API_CONFIG.FLOWS.VERSION_DETAIL, { flowId, versionId })
    );
    if (result?.code !== '200') return result || {};

    // 将后端版本摘要字段、编排配置字段与其余业务字段合并，供页面直接回显
    const detail = result.data || {};
    // orchestrationConfig 当前接口返回 JSON 字符串，这里统一归一化为前端可读对象
    const orchestrationConfig = normalizeJsonConfig(detail.orchestrationConfig);
    return {
      code: '200',
      data: {
        ...detail,
        ...buildVersionSummary(detail),
        ...(transformOrchestrationConfigToFlowData(orchestrationConfig) || {}),
      },
    };
  } catch {
    return {};
  }
};

/**
 * 将文档值表达式还原为前端保存的引用路径或静态值
 * @param {string} rawValue 文档值表达式
 * @returns {Object} 前端取值对象
 */
const parseValueExpression = (rawValue) => {
  // 非字符串值按静态空值处理，避免回显异常
  const value = typeof rawValue === 'string' ? rawValue.trim() : '';
  const matched = value.match(/^\$\{(.+)\}$/);
  if (!matched) return { mode: 'static', value };

  const expression = matched[1];
  if (expression.startsWith('$.node.')) {
    return { mode: 'ref', value: expression.replace(/^\$\.node\./, '') };
  }
  if (expression.startsWith('$.constant:')) {
    return { mode: 'static', value: expression.replace(/^\$\.constant:/, '') };
  }
  return { mode: 'static', value };
};

/**
 * 将 httpInputDef 还原为前端三段参数对象
 * @param {Object} input 文档 input 对象
 * @returns {Object} 前端 inputParams 对象
 */
const parseHttpInputToParams = (input) => {
  // 前端要求固定存在 header/query/body 三段数组
  return {
    header: parseJsonObjectToParams({ jsonObject: input?.header, carrier: 'header' }),
    query: parseJsonObjectToParams({ jsonObject: input?.query, carrier: 'query' }),
    body: parseJsonObjectToParams({ jsonObject: input?.body, carrier: 'body' }),
  };
};

/**
 * 将 HTTP 分段映射还原为前端 inputMappings
 * @param {Object} input 文档 connector input 对象
 * @returns {Object} 前端 inputMappings 对象
 */
const parseHttpInputToMappings = (input) => {
  // 按 header/query/body 三段还原取值模式和值
  const mappings = {};
  ['header', 'query', 'body'].forEach((carrier) => {
    const properties = input?.[carrier]?.properties || {};
    const carrierMap = {};
    Object.keys(properties).forEach((paramName) => {
      carrierMap[paramName] = parseValueExpression(properties[paramName]?.value);
    });
    if (hasObjectKeys(carrierMap)) mappings[carrier] = carrierMap;
  });
  return mappings;
};

/**
 * 将连接器认证映射还原为前端 authMappings
 * @param {Object} authMappings 文档连接器认证映射对象
 * @returns {Object} 前端 authMappings 对象
 */
const parseAuthMappings = (authMappings) => {
  // 按认证方式分组还原每个认证参数的取值模式和值
  const result = {};
  Object.keys(authMappings || {}).forEach((authType) => {
    const authProperties = authMappings?.[authType] || {};
    const carrierMap = {};
    Object.keys(authProperties).forEach((paramName) => {
      carrierMap[paramName] = parseValueExpression(authProperties[paramName]?.value);
    });
    if (hasObjectKeys(carrierMap)) result[authType] = carrierMap;
  });
  return result;
};

/**
 * 从 authConfigs 中还原 Cookie 认证映射编辑态
 * @param {Array} authConfigs 文档连接器认证配置数组
 * @returns {Object} 前端 Cookie 认证映射对象
 */
const parseCookieAuthMappingsFromAuthConfigs = (authConfigs) => {
  // Cookie 的 value 已保存到 authConfigs 中，这里还原为页面现有编辑态结构
  const cookieConfig = (authConfigs || []).find(item => item?.type === 'Cookie');
  const cookieMappings = {};
  ['header', 'query', 'body'].forEach((carrier) => {
    const properties = cookieConfig?.[carrier]?.properties || {};
    Object.keys(properties).forEach((paramName) => {
      cookieMappings[paramName] = parseValueExpression(properties[paramName]?.value);
    });
  });
  return hasObjectKeys(cookieMappings) ? { Cookie: cookieMappings } : {};
};

/**
 * 将 jsonObjectDef 输出字段还原为输出节点参数
 * @param {Object} params 参数对象
 * 包含以下字段：
 * - name: 参数名称
 * - field: jsonObjectDef 字段
 * @returns {Object} 输出参数节点
 */
const parseJsonFieldToOutputParam = (params) => {
  // params.name / params.field / params.carrier
  const { name, field, carrier } = params;
  const type = field?.type || 'string';
  const parsedValue = parseValueExpression(field?.value);
  const children = type === 'object'
    ? parseJsonObjectToOutputParams({ jsonObject: field, carrier })
    : parseJsonObjectToOutputParams({ jsonObject: field?.items, carrier });
  return {
    paramName: name,
    paramType: type,
    sourceType: parsedValue.mode,
    paramValue: parsedValue.value,
    children,
    carrier,
  };
};

/**
 * 将 jsonObjectDef 还原为输出节点参数数组
 * @param {Object} params 参数对象
 * 包含以下字段：
 * - jsonObject: jsonObjectDef 对象
 * - carrier: 参数位置
 * @returns {Array} 输出参数数组
 */
const parseJsonObjectToOutputParams = (params) => {
  // params.jsonObject / params.carrier
  const { jsonObject, carrier } = params || {};
  if (!jsonObject?.properties) return [];
  return Object.entries(jsonObject.properties).map(([name, field]) => parseJsonFieldToOutputParam({ name, field, carrier }));
};

/**
 * 从触发器认证配置中提取 SYSACCOUNT 白名单
 * @param {Array} authConfigs 文档认证配置数组
 * @returns {Array} SYSACCOUNT 白名单
 */
const parseTriggerSystokens = (authConfigs) => {
  // 当前前端只回显 SYSTOKEN 的 sysAccountWhitelist
  const systokenConfig = (authConfigs || []).find(item => item?.type === 'SYSTOKEN');
  return systokenConfig?.sysAccountWhitelist || [];
};

/**
 * 将 React Flow 触发器节点还原为前端触发器节点
 * @param {Object} node React Flow 节点
 * @returns {Object} 前端触发器节点
 */
const parseTriggerNode = (node) => ({
  id: node?.id || 'trigger',
  type: 'trigger',
  triggerType: node?.data?.triggerType || 'http',
  systokens: parseTriggerSystokens(node?.data?.authConfigs),
  inputParams: parseHttpInputToParams(node?.data?.input),
});

/**
 * 将 React Flow 连接器节点还原为前端连接器节点
 * @param {Object} node React Flow 节点
 * @returns {Object} 前端连接器节点
 */
const parseConnectorNode = (node) => {
  // connectorVersionConfig 是连接器版本快照，旧数据没有该字段时回退读取节点顶层字段
  const connectorVersionConfig = node?.data?.connectorVersionConfig || {};
  const authConfigs = connectorVersionConfig.authConfigs || node?.data?.authConfigs || [];
  const output = connectorVersionConfig.output || node?.data?.output || {};
  return {
    id: node?.id,
    type: 'connector',
    connectorId: node?.data?.connectorId || '',
    versionId: node?.data?.connectorVersionId || '',
    connectorVersionConfig,
    authMethodId: '',
    timeout: Number(node?.data?.timeoutMs || 0),
    inputMappings: parseHttpInputToMappings(node?.data?.input),
    authConfigs,
    authMappings: parseCookieAuthMappingsFromAuthConfigs(authConfigs),
    outputParams: {
      header: parseJsonObjectToParams({ jsonObject: output?.header, carrier: 'header' }),
      body: parseJsonObjectToParams({ jsonObject: output?.body, carrier: 'body' }),
    },
  };
};

/**
 * 将 React Flow 脚本节点还原为前端脚本节点
 * @param {Object} node React Flow 节点
 * @returns {Object} 前端脚本节点
 */
const parseScriptNode = (node) => ({
  id: node?.id,
  type: 'script',
  script: node?.data?.editorScript || node?.data?.script || '',
  outputParams: parseJsonObjectToParams({ jsonObject: node?.data?.output, carrier: 'body' }),
});

/**
 * 根据边关系获取并行节点直连的连接器节点
 * @param {Object} params 参数对象
 * 包含以下字段：
 * - parallelNode: 并行节点
 * - nodes: React Flow 节点列表
 * - edges: React Flow 边列表
 * @returns {Array} 并行分支连接器节点列表
 */
const getParallelConnectorNodes = (params) => {
  // params.parallelNode / params.nodes / params.edges
  const { parallelNode, nodes, edges } = params;
  const nodeMap = new Map((nodes || []).map(node => [node?.id, node]));
  return (edges || [])
    .filter(edge => edge?.source === parallelNode?.id)
    .map(edge => nodeMap.get(edge?.target))
    .filter(node => node?.type === 'connector');
};

/**
 * 将并行节点后的连接器还原为前端分支结构
 * @param {Object} params 参数对象
 * 包含以下字段：
 * - parallelNode: 并行节点
 * - branchConnectorNodes: 并行分支连接器节点列表
 * @returns {Object} 前端并行节点
 */
const parseParallelNode = (params) => {
  // params.parallelNode / params.branchConnectorNodes
  const { parallelNode, branchConnectorNodes } = params;
  const branches = (branchConnectorNodes || []).map((connectorNode, index) => ({
    id: `branch_${connectorNode?.id}`,
    label: `分支${index + 1}`,
    connector: parseConnectorNode(connectorNode),
  }));

  return {
    id: parallelNode?.id,
    type: 'parallel',
    activeBranchId: branches[0]?.id || '',
    branches,
  };
};

/**
 * 将 React Flow 出口节点还原为前端输出节点
 * @param {Object} node React Flow 节点
 * @returns {Object} 前端输出节点
 */
const parseExitNode = (node) => ({
  id: node?.id || 'output',
  type: 'output',
  assembleParams: {
    header: parseJsonObjectToOutputParams({ jsonObject: node?.data?.output?.header, carrier: 'header' }),
    body: parseJsonObjectToOutputParams({ jsonObject: node?.data?.output?.body, carrier: 'body' }),
  },
});

/**
 * 将 flowConfig 还原为前端更多配置字段
 * @param {Object} flowConfig 文档 flowConfig 对象
 * @returns {Object} 前端更多配置字段
 */
const parseFlowConfigToFlowData = (flowConfig) => {
  // flowMode/rateLimit/cache 从 flowConfig 中还原
  const cacheKeys = flowConfig?.cache?.key || [];
  return {
    flowMode: flowConfig?.flowMode || '',
    ...(flowConfig?.rateLimitConfig?.maxQps !== undefined ? { rateLimit: flowConfig.rateLimitConfig.maxQps } : {}),
    cacheEnabled: cacheKeys.length > 0,
    ...(flowConfig?.cache?.ttl !== undefined ? { cacheTime: flowConfig.cache.ttl } : {}),
    ...(cacheKeys.length > 0 ? { cacheKeys: cacheKeys.map(item => parseValueExpression(item).value) } : {}),
  };
};

/**
 * 判断是否为旧版前端连接流编辑结构
 * @param {Object} orchestrationConfig 编排配置
 * @returns {boolean} 是否为旧版结构
 */
const isLegacyFlowDataConfig = (orchestrationConfig) => {
  // 旧结构直接包含 flowMode/trigger/steps/output，可直接回显
  return !!orchestrationConfig?.flowMode
    || !!orchestrationConfig?.trigger
    || !!orchestrationConfig?.steps
    || !!orchestrationConfig?.output;
};

/**
 * 将文档 orchestrationConfig 转换为页面可回显的前端 flowData
 * @param {Object} orchestrationConfig 文档编排配置
 * @returns {Object|null} 前端 flowData 字段
 */
const transformOrchestrationConfigToFlowData = (orchestrationConfig) => {
  // 空配置交给页面走默认空态
  if (!orchestrationConfig) return null;

  // 兼容已经保存过的旧版前端结构
  if (isLegacyFlowDataConfig(orchestrationConfig)) return orchestrationConfig;

  const nodes = orchestrationConfig.nodes || [];
  const edges = orchestrationConfig.edges || [];
  const triggerNode = nodes.find(node => node?.type === 'trigger');
  const outputNode = nodes.find(node => node?.type === 'exit');
  const flowData = parseFlowConfigToFlowData(orchestrationConfig.flowConfig || {});
  const parallelNode = nodes.find(node => node?.type === 'parallel');

  if (flowData.flowMode === 'parallel' && parallelNode) {
    // 并行模式需要根据 edges 将并行节点后的连接器还原成分支，而不是平铺进 steps。
    const branchConnectorNodes = getParallelConnectorNodes({ parallelNode, nodes, edges });
    const branchConnectorIds = new Set(branchConnectorNodes.map(node => node?.id));
    const stepNodes = nodes.filter(node => (
      !['trigger', 'exit', 'text', 'connector'].includes(node?.type)
      || (node?.type === 'connector' && !branchConnectorIds.has(node?.id))
    ));

    return {
      ...flowData,
      trigger: parseTriggerNode(triggerNode),
      steps: stepNodes.map((node) => {
        if (node?.type === 'script') return parseScriptNode(node);
        if (node?.type === 'connector') return parseConnectorNode(node);
        if (node?.type === 'parallel') return parseParallelNode({ parallelNode: node, branchConnectorNodes });
        return { id: node?.id, type: node?.type };
      }),
      output: parseExitNode(outputNode),
    };
  }

  const stepNodes = nodes.filter(node => !['trigger', 'exit', 'text'].includes(node?.type));

  return {
    ...flowData,
    trigger: parseTriggerNode(triggerNode),
    steps: stepNodes.map((node) => {
      if (node?.type === 'script') return parseScriptNode(node);
      if (node?.type === 'connector') return parseConnectorNode(node);
      return { id: node?.id, type: node?.type };
    }),
    output: parseExitNode(outputNode),
  };
};

/**
 * React Flow 节点默认横向间距
 */
const FLOW_NODE_X_GAP = 260;

/**
 * 判断 HTTP 分段对象是否包含有效 carrier
 * @param {Object} httpObject HTTP 分段对象
 * @returns {boolean} 是否包含 header/query/body 中任意有效配置
 */
const hasHttpCarrier = (httpObject) => {
  return ['header', 'query', 'body'].some(carrier => hasObjectKeys(httpObject?.[carrier]?.properties));
};

/**
 * 将前端引用路径转换为文档要求的值表达式
 * @param {string} rawValue 前端保存的引用路径
 * @returns {string} 文档值表达式
 */
const buildRefExpression = (rawValue) => {
  // 空引用保持为空，避免生成无效表达式
  const value = String(rawValue || '').trim();
  if (!value) return '';

  // 已经是完整表达式时直接复用
  if (/^\$\{.+\}$/.test(value)) return value;

  // 已经带 $. 根路径时只补外层表达式
  if (value.startsWith('$.')) return '${' + value + '}';

  // 前端引用一般保存为 nodeId.input.body.name，这里补齐 $.node 前缀
  return '${$.node.' + value + '}';
};

/**
 * 将静态值转换为文档要求的常量表达式
 * @param {string} rawValue 前端保存的静态值
 * @returns {string} 常量表达式
 */
const buildConstantExpression = (rawValue) => {
  // 空值保持为空，避免把空字符串包装成常量表达式
  const value = String(rawValue ?? '').trim();
  if (!value) return '';

  // 已经是完整表达式时直接复用
  if (/^\$\{.+\}$/.test(value)) return value;

  return '${$.constant:' + value + '}';
};

/**
 * 根据前端取值模式生成文档值表达式
 * @param {Object} params 参数对象
 * 包含以下字段：
 * - mode: 取值模式，static/ref
 * - value: 原始取值
 * @returns {string} 文档值表达式
 */
const buildValueExpression = (params) => {
  // params.mode / params.value
  const { mode, value } = params;
  if (mode === 'ref') return buildRefExpression(value);
  return buildConstantExpression(value);
};

/**
 * 将前端 HTTP 三段参数转换为文档 httpInputDef
 * @param {Object} sourceMap 前端 header/query/body 参数数组对象
 * @returns {Object} httpInputDef 对象
 */
const buildHttpInputFromParams = (sourceMap) => {
  // 触发器入参需要固定写入协议标识
  const input = buildHttpCarrierParams({
    sourceMap,
    carriers: ['header', 'query', 'body'],
  });
  return input;
};

/**
 * 将单个前端映射配置转换为 jsonObjectDef 叶子字段
 * @param {*} rawMapping 前端映射配置
 * @returns {Object} jsonObjectDef 叶子字段
 */
const buildJsonFieldFromMapping = (rawMapping) => {
  // 兼容旧字符串值和新版 { mode, value } 结构
  const mapping = rawMapping && typeof rawMapping === 'object'
    ? rawMapping
    : { mode: 'static', value: rawMapping };
  return {
    type: 'string',
    value: buildValueExpression({
      mode: mapping.mode || 'static',
      value: mapping.value,
    }),
  };
};

/**
 * 将连接器入参映射转换为 HTTP 分段 jsonObjectDef
 * @param {Object} mappings 前端 inputMappings/authMappings 映射对象
 * @returns {Object} HTTP 分段对象
 */
const buildHttpInputFromMappings = (mappings) => {
  // 按 header/query/body 三段分别转换映射值
  const input = {};
  ['header', 'query', 'body'].forEach((carrier) => {
    const carrierMap = mappings?.[carrier] || {};
    const properties = {};
    Object.keys(carrierMap).forEach((paramName) => {
      properties[paramName] = buildJsonFieldFromMapping(carrierMap[paramName]);
    });
    if (hasObjectKeys(properties)) {
      input[carrier] = { type: 'object', properties };
    }
  });
  return input;
};

/**
 * 将前端认证映射转换为连接器节点独立认证字段
 * @param {Object} authMappings 前端认证映射对象
 * @returns {Object} 连接器节点认证映射对象
 */
const buildConnectorAuthMappings = (authMappings) => {
  // 按认证方式保留分组，避免与普通 inputMappings 混在 data.input 中
  const result = {};
  Object.keys(authMappings || {}).forEach((authType) => {
    const mappingGroup = authMappings?.[authType] || {};
    const properties = {};
    Object.keys(mappingGroup).forEach((paramName) => {
      properties[paramName] = buildJsonFieldFromMapping(mappingGroup[paramName]);
    });
    if (hasObjectKeys(properties)) result[authType] = properties;
  });
  return result;
};

/**
 * 获取系统认证变量表达式
 * @param {Object} params 参数对象
 * 包含以下字段：
 * - authType: 认证方式
 * - paramName: 参数名称
 * @returns {string} 系统变量表达式
 */
const getSystemAuthValueExpression = (params) => {
  // params.authType / params.paramName
  const { authType, paramName } = params;
  if (authType === 'SOA') return '${$.system.env.soaToken}';
  if (authType === 'SIGNATURE') return '${$.system.env.signature}';
  if (authType === 'APIG' && paramName === 'apigAppKey') return '${$.system.env.apigAppKey}';
  if (authType === 'APIG' && paramName === 'apigAppSecret') return '${$.system.env.apigAppSecret}';
  return '';
};

/**
 * 构建 Cookie 认证参数的值表达式
 * @param {Object} params 参数对象
 * 包含以下字段：
 * - authMappings: 前端认证映射对象
 * - paramName: 参数名称
 * @returns {string} Cookie 值表达式
 */
const buildCookieAuthValueExpression = (params) => {
  // params.authMappings / params.paramName
  const { authMappings, paramName } = params;
  const mapping = authMappings?.Cookie?.[paramName];
  if (!mapping) return '';
  return buildValueExpression({
    mode: mapping.mode || 'static',
    value: mapping.value,
  });
};

/**
 * 构建连接器节点认证字段
 * @param {Object} params 参数对象
 * 包含以下字段：
 * - authType: 认证方式
 * - paramName: 参数名称
 * - field: 原始认证字段配置
 * - authMappings: 前端认证映射对象
 * @returns {Object} 认证字段配置
 */
const buildConnectorAuthField = (params) => {
  // params.authType / params.paramName / params.field / params.authMappings
  const { authType, paramName, field, authMappings } = params;
  const result = { ...(field || {}) };
  if (authType === 'Cookie') {
    const cookieValue = buildCookieAuthValueExpression({ authMappings, paramName });
    if (cookieValue) result.value = cookieValue;
    return result;
  }
  const systemValue = getSystemAuthValueExpression({ authType, paramName });
  if (systemValue) result.value = systemValue;
  if (authType === 'SIGNATURE') result.sensitive = true;
  return result;
};

/**
 * 构建连接器节点单个认证载体对象
 * @param {Object} params 参数对象
 * 包含以下字段：
 * - authType: 认证方式
 * - carrierSchema: 原始载体 schema
 * - authMappings: 前端认证映射对象
 * @returns {Object|null} 认证载体对象
 */
const buildConnectorAuthCarrier = (params) => {
  // params.authType / params.carrierSchema / params.authMappings
  const { authType, carrierSchema, authMappings } = params;
  if (!carrierSchema?.properties) return null;
  const properties = {};
  Object.keys(carrierSchema.properties).forEach((paramName) => {
    properties[paramName] = buildConnectorAuthField({
      authType,
      paramName,
      field: carrierSchema.properties[paramName],
      authMappings,
    });
  });
  return { ...carrierSchema, properties };
};

/**
 * 构建连接器节点 authConfigs
 * @param {Object} params 参数对象
 * 包含以下字段：
 * - authConfigs: 连接器版本认证配置
 * - authMappings: 前端认证映射对象
 * @returns {Array} 连接器节点认证配置
 */
const buildConnectorAuthConfigs = (params) => {
  // params.authConfigs / params.authMappings
  const { authConfigs, authMappings } = params;
  return (authConfigs || []).map((authConfig) => {
    const config = { type: authConfig.type };
    ['header', 'query', 'body', 'secretKey'].forEach((carrier) => {
      const carrierSchema = buildConnectorAuthCarrier({
        authType: authConfig.type,
        carrierSchema: authConfig[carrier],
        authMappings,
      });
      if (carrierSchema) config[carrier] = carrierSchema;
    });
    return config;
  }).filter(item => item.type);
};

/**
 * 构建连接器版本配置快照
 * @param {Object} params 参数对象
 * 包含以下字段：
 * - connectorVersionConfig: 连接器版本原始快照
 * - authConfigs: 已写入映射值的认证配置
 * @returns {Object} 连接器版本配置快照
 */
const buildConnectorVersionConfig = (params) => {
  // params.connectorVersionConfig / params.authConfigs
  const { connectorVersionConfig, authConfigs } = params;
  const config = { ...(connectorVersionConfig || {}) };
  if (authConfigs.length > 0) config.authConfigs = authConfigs;
  return config;
};

/**
 * 将输出参数节点转换为带 value 的 jsonObjectDef 字段
 * @param {Object} param 前端输出参数节点
 * @returns {Object} jsonObjectDef 字段
 */
const buildJsonFieldFromOutputParam = (param) => {
  // param.paramName / param.paramType / param.sourceType / param.paramValue / param.children
  const type = param.paramType || param.type || 'string';
  if (type === 'object') {
    const childrenObject = buildJsonObjectFromOutputParams(param.children || []);
    return {
      type: 'object',
      properties: childrenObject.properties,
    };
  }

  if (type === 'array') {
    return {
      type: 'array',
      items: buildJsonObjectFromOutputParams(param.children || []),
    };
  }

  return {
    type,
    value: buildValueExpression({
      mode: param.sourceType === 'ref' ? 'ref' : 'static',
      value: param.paramValue,
    }),
  };
};

/**
 * 将输出参数数组转换为 jsonObjectDef
 * @param {Array} params 前端输出参数数组
 * @returns {Object} jsonObjectDef 对象
 */
const buildJsonObjectFromOutputParams = (params) => {
  // 按输出字段名收集 properties，空名称字段不写入保存参数
  const properties = {};
  (params || []).forEach((param) => {
    const name = param.paramName || param.name || '';
    if (!name) return;
    properties[name] = buildJsonFieldFromOutputParam(param);
  });
  return { type: 'object', properties };
};

/**
 * 构建触发器节点的认证配置
 * @param {Object} trigger 前端触发器节点
 * @returns {Array} 文档 authConfigs 数组
 */
const buildTriggerAuthConfigs = (trigger) => {
  // SYSACCOUNT 白名单转换为 SYSTOKEN 认证配置
  const whitelist = (trigger?.systokens || []).filter(Boolean);
  if (whitelist.length === 0) return [];

  return [
    {
      type: 'SYSTOKEN',
      header: {
        type: 'object',
        properties: {
          'X-Sys-Token': {
            type: 'string',
            required: true,
            sensitive: true,
            value: '${$.system.env.sysToken}',
            description: '系统凭证令牌。值来源：凭据库',
          },
        },
      },
      sysAccountWhitelist: whitelist,
    },
  ];
};

/**
 * 构建 React Flow 触发器节点
 * @param {Object} params 参数对象
 * 包含以下字段：
 * - trigger: 前端触发器节点
 * @returns {Object} React Flow 节点
 */
const buildTriggerFlowNode = (params) => {
  // params.trigger
  const { trigger } = params;
  const data = {
    type: 'trigger',
    triggerType: trigger?.triggerType || 'http',
  };
  const authConfigs = buildTriggerAuthConfigs(trigger);
  const input = buildHttpInputFromParams(trigger?.inputParams || {});

  if (authConfigs.length > 0) data.authConfigs = authConfigs;
  if (hasHttpCarrier(input)) data.input = input;

  return {
    id: trigger?.id || 'trigger',
    type: 'trigger',
    data,
  };
};

/**
 * 构建 React Flow 连接器节点
 * @param {Object} params 参数对象
 * 包含以下字段：
 * - node: 前端连接器节点
 * @returns {Object} React Flow 节点
 */
const buildConnectorFlowNode = (params) => {
  // params.node
  const { node } = params;
  const input = buildHttpInputFromMappings(node?.inputMappings || {});
  const authConfigs = buildConnectorAuthConfigs({
    authConfigs: node?.authConfigs || [],
    authMappings: node?.authMappings || {},
  });
  const connectorVersionConfig = buildConnectorVersionConfig({
    connectorVersionConfig: node?.connectorVersionConfig || {},
    authConfigs,
  });
  const data = {
    type: 'connector',
    connectorId: node?.connectorId || '',
    connectorVersionId: node?.versionId || '',
    timeoutMs: Number(node?.timeout || 0),
    input,
  };

  if (hasObjectKeys(connectorVersionConfig)) data.connectorVersionConfig = connectorVersionConfig;

  return {
    id: node?.id,
    type: 'connector',
    data,
  };
};

/**
 * 构建 React Flow 脚本节点
 * @param {Object} params 参数对象
 * 包含以下字段：
 * - node: 前端脚本节点
 * - upstreamNodeIds: 当前节点之前已生成的上游节点 ID
 * @returns {Object} React Flow 节点
 */
const buildScriptFlowNode = (params) => {
  // params.node / params.upstreamNodeIds
  const { node, upstreamNodeIds } = params;
  const script = node?.script || '';
  const normalizedScript = stripScriptEditorTypes(script)
    .replace(/export\s+function\s+transform\s*\(([^)]*)\)/, (match, args) => {
      // 运行时执行 JavaScript，因此保存时移除 transform 参数上的 TypeScript 类型标注。
      const runtimeArgs = args
        .split(',')
        .map(item => item.trim().split(':')[0].trim())
        .filter(Boolean)
        .join(', ');
      return `function transform(${runtimeArgs})`;
    });
  const wrappedScript = normalizedScript.includes('function main(')
    ? normalizedScript
    : `${normalizedScript}\n\n/**\n * 脚本运行入口，负责把运行时 Context 传入 transform。\n * @param ctx 上游节点参数上下文\n */\nfunction main(ctx) {\n  return transform(ctx);\n}`;
  return {
    id: node?.id,
    type: 'script',
    data: {
      type: 'script',
      script: wrappedScript,
      editorScript: script,
      upstreamNodeIds: upstreamNodeIds || [],
      output: buildJsonObjectFromParams(node?.outputParams || []) || { type: 'object', properties: {} },
    },
  };
};

/**
 * 构建 React Flow 并行主节点
 * @param {Object} params 参数对象
 * 包含以下字段：
 * - node: 前端并行节点
 * @returns {Object} React Flow 节点
 */
const buildParallelFlowNode = (params) => {
  // params.node
  const { node } = params;
  return {
    id: node?.id,
    type: 'parallel',
    data: {
      type: 'parallel',
    },
  };
};

/**
 * 构建 React Flow 出口节点
 * @param {Object} params 参数对象
 * 包含以下字段：
 * - output: 前端输出节点
 * @returns {Object} React Flow 节点
 */
const buildExitFlowNode = (params) => {
  // params.output
  const { output } = params;
  const dataOutput = {};
  ['header', 'body'].forEach((carrier) => {
    const jsonObject = buildJsonObjectFromOutputParams(output?.assembleParams?.[carrier] || []);
    if (hasObjectKeys(jsonObject.properties)) {
      dataOutput[carrier] = jsonObject;
    }
  });

  return {
    id: output?.id || 'output',
    type: 'exit',
    data: {
      type: 'exit',
      output: dataOutput,
    },
  };
};

/**
 * 根据前端节点类型构建 React Flow 节点
 * @param {Object} params 参数对象
 * 包含以下字段：
 * - node: 前端节点
 * - upstreamNodeIds: 当前节点之前已生成的上游节点 ID
 * @returns {Object|null} React Flow 节点
 */
const buildStepFlowNode = (params) => {
  // params.node / params.upstreamNodeIds
  const { node, upstreamNodeIds } = params;
  if (node?.type === 'connector') return buildConnectorFlowNode({ node });
  if (node?.type === 'script') return buildScriptFlowNode({ node, upstreamNodeIds });
  if (node?.type === 'parallel') return buildParallelFlowNode({ node });
  return null;
};

/**
 * 构建一条 React Flow 边
 * @param {Object} params 参数对象
 * 包含以下字段：
 * - source: 起点节点 ID
 * - target: 终点节点 ID
 * - connectionMode: 连接模式
 * @returns {Object} React Flow 边
 */
const buildFlowEdge = (params) => {
  // params.source / params.target / params.connectionMode
  const { source, target, connectionMode } = params;
  return {
    id: `edge_${source}_${target}`,
    source,
    target,
    type: 'smoothstep',
    data: {
      businessType: 'default',
      connectionMode,
    },
  };
};

/**
 * 构建串行或单节点模式的 nodes/edges
 * @param {Object} flowData 前端连接流数据
 * @returns {Object} React Flow nodes/edges
 */
const buildSerialFlowGraph = (flowData) => {
  // 串行图按照 trigger -> steps -> output 顺序生成，并为脚本节点记录已经生成的上游节点 ID。
  const nodes = [buildTriggerFlowNode({ trigger: flowData.trigger })];
  (flowData.steps || []).forEach((step) => {
    const upstreamNodeIds = nodes.map(node => node.id);
    const flowNode = buildStepFlowNode({ node: step, upstreamNodeIds });
    if (flowNode) nodes.push(flowNode);
  });
  nodes.push(buildExitFlowNode({ output: flowData.output }));

  const edges = [];
  for (let i = 0; i < nodes.length - 1; i++) {
    edges.push(buildFlowEdge({
      source: nodes[i].id,
      target: nodes[i + 1].id,
      connectionMode: 'serial',
    }));
  }
  return { nodes, edges };
};

/**
 * 构建并行模式的 nodes/edges
 * @param {Object} flowData 前端连接流数据
 * @returns {Object} React Flow nodes/edges
 */
const buildParallelFlowGraph = (flowData) => {
  // 并行图按照 trigger -> parallel -> branch connectors -> output 生成
  const triggerNode = buildTriggerFlowNode({ trigger: flowData.trigger });
  const parallel = (flowData.steps || []).find(item => item?.type === 'parallel');
  if (!parallel) return buildSerialFlowGraph(flowData);

  const parallelNode = buildParallelFlowNode({ node: parallel });
  const branchNodes = (parallel.branches || []).map((branch) => buildConnectorFlowNode({
    node: branch.connector,
  }));
  const outputNode = buildExitFlowNode({ output: flowData.output });
  const nodes = [triggerNode, parallelNode, ...branchNodes, outputNode];
  const edges = [buildFlowEdge({
    source: triggerNode.id,
    target: parallelNode.id,
    connectionMode: 'serial',
  })];

  branchNodes.forEach((branchNode) => {
    edges.push(buildFlowEdge({
      source: parallelNode.id,
      target: branchNode.id,
      connectionMode: 'parallel',
    }));
    edges.push(buildFlowEdge({
      source: branchNode.id,
      target: outputNode.id,
      connectionMode: 'parallel',
    }));
  });

  return { nodes, edges };
};

/**
 * 构建流级配置
 * @param {Object} flowData 前端连接流数据
 * @returns {Object} 文档 flowConfig 对象
 */
const buildFlowConfig = (flowData) => {
  // 限流和缓存从前端更多配置中转换为文档字段
  const flowConfig = {
    flowMode: flowData.flowMode || '',
  };
  if (flowData.rateLimit !== undefined && flowData.rateLimit !== null) {
    flowConfig.rateLimitConfig = { maxQps: flowData.rateLimit };
  }
  if (flowData.cacheEnabled) {
    flowConfig.cache = {
      key: (flowData.cacheKeys || []).filter(Boolean).map(buildRefExpression),
      ttl: flowData.cacheTime,
    };
  }
  return flowConfig;
};

/**
 * 将前端连接流编辑结构转换为文档要求的 orchestrationConfig
 * @param {Object} flowData 前端连接流数据
 * @returns {Object} 文档 orchestrationConfig
 */
const buildOrchestrationConfig = (flowData) => {
  // 保存接口只提交 flowConfig/nodes/edges，避免把前端编辑态字段透传给后端
  const graph = flowData?.flowMode === 'parallel'
    ? buildParallelFlowGraph(flowData)
    : buildSerialFlowGraph(flowData || {});
  const flowConfig = buildFlowConfig(flowData || {});
  return {
    ...(hasObjectKeys(flowConfig) ? { flowConfig } : {}),
    nodes: graph.nodes,
    edges: graph.edges,
  };
};

/**
 * 保存草稿版本（#31 更新连接流版本）
 *
 * @param {Object} params - 参数对象
 * 包含以下字段：
 * - flowId: 连接流 ID
 * - versionId: 版本 ID（仅草稿状态可编辑）
 * - config: 编排配置（orchestrationConfig 内容：flowConfig / nodes / edges）
 *
 * @returns {Promise<Object>} 保存结果
 */
export const saveDraft = async (params) => {
  // 解构传入对象中需要使用的参数
  const { flowId, versionId, config } = params;

  try {
    const result = await fetchApi(
      buildApiUrl(API_CONFIG.FLOWS.VERSION_UPDATE, { flowId, versionId }),
      {
        method: 'PUT',
        body: JSON.stringify({ orchestrationConfig: buildOrchestrationConfig(config) }),
      }
    );
    return result || {};
  } catch {
    return {};
  }
};

/**
 * 发布连接流版本（#32）
 * 仅触发发布接口，不在发布时保存草稿内容
 *
 * @param {Object} params - 参数对象
 * 包含以下字段：
 * - flowId: 连接流 ID
 * - versionId: 版本 ID
 *
 * @returns {Promise<Object>} 发布结果
 */
export const publishVersion = async (params) => {
  // 解构传入对象中需要使用的参数
  const { flowId, versionId } = params;

  try {
    const result = await fetchApi(
      buildApiUrl(API_CONFIG.FLOWS.VERSION_PUBLISH, { flowId, versionId }),
      { method: 'POST' }
    );
    return result || {};
  } catch {
    return {};
  }
};

/**
 * 创建草稿版本（#28 / #33）
 * 传入 baseVersionId 时走「复制到草稿」#33；否则走「创建空草稿」#28
 *
 * @param {Object} params - 参数对象
 * 包含以下字段：
 * - flowId: 连接流 ID
 * - baseVersionId: 基础版本 ID（可选）
 *
 * @returns {Promise<Object>} 创建结果
 */
export const createDraftVersion = async (params) => {
  // 解构传入对象中需要使用的参数
  const { flowId, baseVersionId } = params;

  try {
    let result;
    if (baseVersionId) {
      // 基于已发布/已失效版本复制到草稿（#33）
      result = await fetchApi(
        buildApiUrl(API_CONFIG.FLOWS.VERSION_COPY_TO_DRAFT, {
          flowId,
          versionId: baseVersionId,
        }),
        { method: 'POST' }
      );
    } else {
      // 创建空草稿（#28）
      result = await fetchApi(
        buildApiUrl(API_CONFIG.FLOWS.VERSION_CREATE, { flowId }),
        { method: 'POST' }
      );
    }
    return result || {};
  } catch {
    return {};
  }
};

/**
 * 失效版本（#34）
 *
 * @param {Object} params - 参数对象
 * 包含以下字段：
 * - flowId: 连接流 ID
 * - versionId: 版本 ID（仅已发布状态可失效）
 *
 * @returns {Promise<Object>} 操作结果
 */
export const expireVersion = async (params) => {
  // 解构传入对象中需要使用的参数
  const { flowId, versionId } = params;

  try {
    const result = await fetchApi(
      buildApiUrl(API_CONFIG.FLOWS.VERSION_INVALIDATE, { flowId, versionId }),
      { method: 'PUT' }
    );
    return result || {};
  } catch {
    return {};
  }
};

/**
 * 恢复版本（#35）
 *
 * @param {Object} params - 参数对象
 * 包含以下字段：
 * - flowId: 连接流 ID
 * - versionId: 版本 ID（仅已失效状态可恢复）
 *
 * @returns {Promise<Object>} 操作结果
 */
export const restoreVersion = async (params) => {
  // 解构传入对象中需要使用的参数
  const { flowId, versionId } = params;

  try {
    const result = await fetchApi(
      buildApiUrl(API_CONFIG.FLOWS.VERSION_RECOVER, { flowId, versionId }),
      { method: 'PUT' }
    );
    return result || {};
  } catch {
    return {};
  }
};

/**
 * 撤回版本审批（#37）
 *
 * @param {Object} params - 参数对象
 * 包含以下字段：
 * - flowId: 连接流 ID
 * - versionId: 版本 ID（仅待审批状态可撤回）
 *
 * @returns {Promise<Object>} 操作结果
 */
export const withdrawVersion = async (params) => {
  // 解构传入对象中需要使用的参数
  const { flowId, versionId } = params;

  try {
    const result = await fetchApi(
      buildApiUrl(API_CONFIG.FLOWS.VERSION_CANCEL, { flowId, versionId }),
      { method: 'POST' }
    );
    return result || {};
  } catch {
    return {};
  }
};

/**
 * 删除版本（#36）
 *
 * @param {Object} params - 参数对象
 * 包含以下字段：
 * - flowId: 连接流 ID
 * - versionId: 版本 ID
 *
 * @returns {Promise<Object>} 操作结果
 */
export const deleteVersion = async (params) => {
  // 解构传入对象中需要使用的参数
  const { flowId, versionId } = params;

  try {
    const result = await fetchApi(
      buildApiUrl(API_CONFIG.FLOWS.VERSION_DELETE, { flowId, versionId }),
      { method: 'DELETE' }
    );
    return result || {};
  } catch {
    return {};
  }
};

/**
 * 查询连接器列表（#2，过滤为有效可用 status=2）
 *
 * @returns {Promise<Object>} 连接器列表
 */
export const fetchConnectorList = async () => {
  try {
    const result = await fetchApi(API_CONFIG.CONNECTORS.LIST, {
      params: { status: 2 },
    });
    if (result?.code !== '200') return result || {};

    // 将连接器列表字段统一映射为节点卡片下拉框使用的 id / name 结构
    const list = (result.data || []).map((item) => ({
      ...item,
      id: item.connectorId,
      name: item.nameCn || item.nameEn || '',
    }));
    return { ...result, data: list };
  } catch {
    return {};
  }
};

/**
 * 查询连接器版本列表（#9，过滤为已发布 status=2）
 *
 * @param {string} connectorId - 连接器 ID
 * @returns {Promise<Object>} 版本列表
 */
export const fetchConnectorVersions = async (connectorId) => {
  try {
    const result = await fetchApi(
      buildApiUrl(API_CONFIG.CONNECTORS.VERSIONS_LIST, { connectorId }),
      { params: { status: 2 } }
    );
    if (result?.code !== '200') return result || {};

    // 将连接器版本列表映射为 UI 展示所需摘要结构（版本号 / 时间 / 状态），并按创建时间倒序
    const list = buildSortedVersionSummaries(result.data || []);
    return { ...result, data: list };
  } catch {
    return {};
  }
};

/**
 * 查询连接器版本入参（#10，仅取 connectionConfig.input）
 *
 * @param {Object} params - 参数对象
 * 包含以下字段：
 * - connectorId: 连接器 ID
 * - versionId: 版本 ID
 *
 * @returns {Promise<Object>} 入参配置（含 header / query / body）
 */
export const fetchConnectorInputParams = async (params) => {
  // 解构传入对象中需要使用的参数
  const { connectorId, versionId } = params;

  try {
    const result = await fetchApi(
      buildApiUrl(API_CONFIG.CONNECTORS.VERSION_DETAIL, { connectorId, versionId })
    );
    if (result?.code !== '200') return result || {};

    // 解析连接器版本详情中的 connectionConfig，提取认证方式与入参配置
    const connectionConfig = normalizeJsonConfig(result.data?.connectionConfig);
    return {
      code: '200',
      data: {
        connectorVersionConfig: connectionConfig || {},
        authType: getFirstAuthType(connectionConfig?.authConfigs),
        authConfigs: transformAuthConfigsToFlowItems(connectionConfig?.authConfigs),
        rawAuthConfigs: connectionConfig?.authConfigs || [],
        header: transformJsonSchemaToFlowParams(connectionConfig?.input?.header),
        body: transformJsonSchemaToFlowParams(connectionConfig?.input?.body),
        query: transformJsonSchemaToFlowParams(connectionConfig?.input?.query),
        outputParams: {
          header: transformJsonSchemaToFlowParams(connectionConfig?.output?.header),
          body: transformJsonSchemaToFlowParams(connectionConfig?.output?.body),
        },
      },
    };
  } catch {
    return {};
  }
};

/**
 * 查询版本详情信息（含状态关联字段）
 * 与 #30 等价，UI 层用于展示审批/驳回/撤回等元数据
 *
 * @param {Object} params - 参数对象
 * 包含以下字段：
 * - flowId: 连接流 ID
 * - versionId: 版本 ID
 *
 * @returns {Promise<Object>} 版本详情信息
 */
export const fetchVersionDetailInfo = async (params) => {
  return fetchVersionDetail(params);
};

/**
 * 调试连接流（#51 调试连接流版本（代理））
 *
 * @param {Object} params - 参数对象
 * 包含以下字段：
 * - flowId: 连接流 ID
 * - versionId: 版本 ID
 * - inputParams: 模拟触发数据（与触发器入参 Schema 对齐）
 *
 * @returns {Promise<Object>} 调试结果（含 executionId / status / durationMs / nodes）
 */
export const debugFlow = async (params) => {
  // 解构传入对象中需要使用的参数
  const { flowId, versionId, inputParams } = params;

  try {
    const result = await fetchApi(
      buildApiUrl(API_CONFIG.FLOWS.VERSION_DEBUG, { flowId, versionId }),
      {
        method: 'POST',
        body: JSON.stringify({ triggerData: inputParams || {} }),
      }
    );
    return result || {};
  } catch {
    return {};
  }
};

/**
 * 催办审批（#38）
 *
 * @param {Object} params - 参数对象
 * 包含以下字段：
 * - flowId: 连接流 ID
 * - versionId: 版本 ID
 *
 * @returns {Promise<Object>} 催办结果
 */
export const urgeApproval = async (params) => {
  // 解构传入对象中需要使用的参数
  const { flowId, versionId } = params;

  try {
    const result = await fetchApi(
      buildApiUrl(API_CONFIG.FLOWS.VERSION_URGE, { flowId, versionId }),
      { method: 'POST' }
    );
    return result || {};
  } catch {
    return {};
  }
};
