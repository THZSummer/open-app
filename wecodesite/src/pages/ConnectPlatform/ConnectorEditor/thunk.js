/**
 * ========================================
 * 连接器编辑页 - API调用函数
 * ========================================
 *
 * 对齐 plan-api.md v7.0：
 *   - #9 查询连接器版本列表
 *   - #10 查询连接器版本详情
 *   - #11 更新连接器版本（保存草稿）
 *   - #12 发布连接器版本
 *   - #8 创建草稿版本 / #13 复制版本到草稿
 *   - #14 失效连接器版本 / #15 恢复连接器版本
 *   - #16 删除连接器版本
 *
 * connectionConfig 结构对齐 plan-json-schema.md §5.2：
 *   - 顶层包裹 `connectionConfig`
 *   - 多选认证：`authConfigs[]`（每项含 type / header / query / sysAccountWhitelist / secretKey）
 *   - 入参：`input`（含 protocol / header / query / body）
 *   - 出参：`output`（含 protocol / header / body）
 */

import { API_CONFIG, buildApiUrl, fetchApi } from '../../../configs/web.config';
import {
  buildSortedVersionSummaries,
  buildVersionSummary,
  normalizeJsonConfig,
} from '../../../utils/common';
import {
  buildJsonFieldFromParam,
  buildJsonObjectFromParams,
  parseJsonObjectToParams,
} from '../../../utils/flowUtilsV2';
import {
  DEFAULT_API_CONFIG,
  MAX_SCHEMA_DEPTH,
  HTTP_URL_REGEX,
  COMMON_AUTH_TYPES,
  SIGNATURE_AUTH_TYPE,
  VALIDATE_SECTION,
} from './constants';

/* ========================================
 * V3 connectionConfig ↔ 前端 apiConfig 转换
 * ======================================== */

/**
 * 处理契约配置的辅助方法
 * 将后端 input/output 中各载体（header/query/body）的 jsonObjectDef 转为 SchemaEditor 参数数组
 *
 * @param {Object} options - 选项对象
 * 包含以下字段：
 * - contract: 后端 input 或 output 对象
 * - carriers: 支持的载体类型数组，如 ['header', 'query', 'body']
 *
 * @returns {Array} SchemaEditor 参数数组
 */
const processContract = (options) => {
  // 解构传入对象中需要使用的参数
  const { contract, carriers } = options;

  if (!contract) return [];

  // 收集所有载体维度的参数
  let result = [];

  // 遍历每个载体类型
  carriers.forEach((carrier) => {
    if (contract[carrier]) {
      const params = transformJsonSchemaToParams(contract[carrier], carrier);
      result = [...result, ...params];
    }
  });

  return result;
};

/**
 * 将 SchemaEditor 参数数组按 carrier 分组并组装为 input/output 结构
 *
 * @param {Object} options - 选项对象
 * 包含以下字段：
 * - schema: SchemaEditor 参数数组
 * - carriers: 支持的载体类型数组
 *
 * @returns {Object} 含 protocol 与各 carrier jsonObjectDef 的对象
 */
const buildContractFromSchema = (options) => {
  // 解构传入对象中需要使用的参数
  const { schema, carriers } = options;

  // 初始化结果对象，固定 protocol=HTTP
  const result = { protocol: 'HTTP' };
  let hasAny = false;

  // 遍历每个载体类型，过滤出对应参数并转 jsonObjectDef
  carriers.forEach((carrier) => {
    const filtered = (schema || []).filter((p) => {
      // body 兼容缺省 carrier 的情况
      if (carrier === 'body') return p.carrier === 'body' || !p.carrier;
      return p.carrier === carrier;
    });

    if (filtered.length > 0) {
      result[carrier] = transformParamsToJsonSchema(filtered);
      hasAny = true;
    }
  });

  // 没有任何参数时返回 null，避免发送空对象
  return hasAny ? result : null;
};

/**
 * 将 jsonObjectDef 格式递归转为 SchemaEditor 参数数组
 *
 * @param {Object} jsonSchema - jsonObjectDef 对象
 * @param {string} defaultCarrier - 默认载体（header / query / body）
 * @returns {Array} SchemaEditor 参数数组
 */
export const transformJsonSchemaToParams = (jsonSchema, defaultCarrier = 'body') => {
  // 复用公共 jsonObjectDef 转参数树方法，保留原导出供历史引用使用
  return parseJsonObjectToParams({ jsonObject: jsonSchema, carrier: defaultCarrier });
};

/**
 * 将 SchemaEditor 单个参数递归转 jsonObjectDef field 定义
 *
 * @param {Object} param - SchemaEditor 单个参数对象
 * @returns {Object} jsonObjectDef field 定义
 */
export const transformParamToJsonSchemaField = (param) => {
  // 复用公共参数节点转 jsonObjectDef 字段方法，保留原导出供历史引用使用
  return buildJsonFieldFromParam(param);
};

/**
 * 将 SchemaEditor 参数数组转 jsonObjectDef
 *
 * @param {Array} params - SchemaEditor 参数数组
 * @returns {Object|null} jsonObjectDef 对象（无参数返回 null）
 */
export const transformParamsToJsonSchema = (params) => {
  // 复用公共参数数组转 jsonObjectDef 方法，空数组保持 null
  return buildJsonObjectFromParams(params);
};

/**
 * 解析 V3 authConfigs[] 数组为前端 apiConfig 的 authType[] + authRequestSchema{}
 * 同时提取 signatureConfig（用于数字签名独立配置区）
 *
 * @param {Array} authConfigs - V3 authConfigs 数组
 * @returns {Object} 含 authType / authRequestSchema / signatureConfig 的对象
 */
const parseAuthConfigs = (authConfigs) => {
  // 初始化各结构
  const authType = [];
  const authRequestSchema = {};
  let signatureConfig = { ...DEFAULT_API_CONFIG.signatureConfig };

  // 参数校验：authConfigs 必须为数组
  if (!Array.isArray(authConfigs)) {
    return { authType, authRequestSchema, signatureConfig };
  }

  // 遍历每一项 authConfig，按 type 分发
  authConfigs.forEach((item) => {
    const { type } = item || {};
    if (!type) return;

    // 记录已勾选的认证类型
    if (!authType.includes(type)) authType.push(type);

    if (COMMON_AUTH_TYPES.includes(type)) {
      // 通用认证：合并 header / query 字段为单一参数数组
      const headerParams = transformJsonSchemaToParams(item.header, 'header');
      const queryParams = transformJsonSchemaToParams(item.query, 'query');
      authRequestSchema[type] = [...headerParams, ...queryParams];
    } else if (type === SIGNATURE_AUTH_TYPE) {
      // 数字签名：解析 secretKey + header 的首个字段
      const secretField = item.secretKey?.properties
        ? Object.entries(item.secretKey.properties)[0]
        : null;
      const headerField = item.header?.properties
        ? Object.entries(item.header.properties)[0]
        : null;

      signatureConfig = {
        paramName: headerField?.[0] || '',
        carrier: 'header',
        fixedValue: headerField?.[1]?.value || '',
        secret: secretField?.[1]?.value || '',
      };
    }
  });

  return { authType, authRequestSchema, signatureConfig };
};

/**
 * 将 V3 connectionConfig 转换为前端表单可编辑的 apiConfig
 *
 * @param {Object|string} connectionConfig - V3 connectionConfig 对象或其 JSON 字符串
 * @returns {Object} 前端表单 apiConfig
 */
export const transformFromSchemaFormat = (connectionConfig) => {
  // 先做字符串/对象的兼容归一化
  const normalized = normalizeJsonConfig(connectionConfig);

  // 数据校验：缺失或解析失败时返回默认配置
  if (!normalized) {
    return { ...DEFAULT_API_CONFIG };
  }

  // 解析认证配置
  const auth = parseAuthConfigs(normalized.authConfigs);

  // 解析入参（含 header / query / body）
  const requestSchema = processContract({
    contract: normalized.input,
    carriers: ['header', 'query', 'body'],
  });

  // 解析出参（含 header / body）
  const responseSchema = processContract({
    contract: normalized.output,
    carriers: ['header', 'body'],
  });

  // 组装前端表单结构
  return {
    protocolType: normalized.protocolConfig?.method || '',
    protocolAddress: normalized.protocolConfig?.url || '',
    authType: auth.authType,
    authRequestSchema: auth.authRequestSchema,
    signatureConfig: auth.signatureConfig,
    headerSchema: [],
    requestSchema,
    responseSchema,
    // V3 新增字段直接透传
    labelCn: normalized.labelCn || '',
    labelEn: normalized.labelEn || '',
    urlWhitelist: normalized.urlWhitelist || [],
    timeoutMs: normalized.timeoutMs || 3000,
    rateLimitConfig: normalized.rateLimitConfig || null,
    sysAccountWhitelist: normalized.sysAccountWhitelist || [],
  };
};

/**
 * 将前端 apiConfig.authType + authRequestSchema 序列化为 V3 authConfigs[]
 *
 * @param {Object} apiConfig - 前端 apiConfig
 * @returns {Array} V3 authConfigs 数组
 */
const buildAuthConfigs = (apiConfig) => {
  const authTypes = Array.isArray(apiConfig.authType) ? apiConfig.authType : [];
  const authRequestSchema = apiConfig.authRequestSchema || {};
  const signatureConfig = apiConfig.signatureConfig || {};

  // 收集每种认证方式的 authConfig 项
  const authConfigs = [];

  // 通用认证：SOA / APIG / Cookie
  authTypes.forEach((type) => {
    if (!COMMON_AUTH_TYPES.includes(type)) return;

    const items = authRequestSchema[type] || [];
    const headerItems = items.filter((p) => p.carrier === 'header' || !p.carrier);
    const queryItems = items.filter((p) => p.carrier === 'query');

    // 构建 authConfig 项
    const config = { type };
    const headerSchema = transformParamsToJsonSchema(headerItems);
    const querySchema = transformParamsToJsonSchema(queryItems);

    if (headerSchema) config.header = headerSchema;
    if (querySchema) config.query = querySchema;

    authConfigs.push(config);
  });

  // 数字签名：独立结构 -> secretKey + header
  if (authTypes.includes(SIGNATURE_AUTH_TYPE)) {
    const headerField = signatureConfig.paramName
      ? {
        type: 'object',
        properties: {
          [signatureConfig.paramName]: {
            type: 'string',
            required: true,
            sensitive: true,
            value: signatureConfig.fixedValue || '',
            description: '签名头',
          },
        },
      }
      : null;

    const secretKey = signatureConfig.secret
      ? {
        type: 'object',
        properties: {
          signSecretKey: {
            type: 'string',
            required: true,
            sensitive: true,
            value: signatureConfig.secret,
            description: '签名密钥',
          },
        },
      }
      : null;

    const config = { type: SIGNATURE_AUTH_TYPE };
    if (secretKey) config.secretKey = secretKey;
    if (headerField) config.header = headerField;
    authConfigs.push(config);
  }

  return authConfigs;
};

/**
 * 将前端 apiConfig 转换为 V3 connectionConfig 结构
 *
 * @param {Object} apiConfig - 前端 apiConfig
 * @returns {Object} V3 connectionConfig 对象
 */
export const transformToSchemaFormat = (apiConfig) => {
  // 协议配置部分
  const result = {
    protocol: 'HTTP',
    protocolConfig: {
      url: apiConfig.protocolAddress || '',
      method: apiConfig.protocolType || '',
    },
  };

  // V3 元数据
  if (apiConfig.labelCn) result.labelCn = apiConfig.labelCn;
  if (apiConfig.labelEn) result.labelEn = apiConfig.labelEn;
  if (Array.isArray(apiConfig.urlWhitelist)) result.urlWhitelist = apiConfig.urlWhitelist;
  if (apiConfig.timeoutMs) result.timeoutMs = apiConfig.timeoutMs;
  if (apiConfig.rateLimitConfig) result.rateLimitConfig = apiConfig.rateLimitConfig;

  // 认证配置（数组形式）
  result.authConfigs = buildAuthConfigs(apiConfig);

  // 入参（含 header / query / body）
  const input = buildContractFromSchema({
    schema: apiConfig.requestSchema,
    carriers: ['header', 'query', 'body'],
  });
  if (input) result.input = input;

  // 出参（含 header / body）
  const output = buildContractFromSchema({
    schema: apiConfig.responseSchema,
    carriers: ['header', 'body'],
  });
  if (output) result.output = output;

  return result;
};

/* ========================================
 * V3 真实接口调用层
 * ======================================== */

/**
 * 查询连接器版本列表（#9）
 *
 * @param {string} connectorId - 连接器 ID
 * @returns {Promise<Object>} 版本列表响应
 */
export const fetchVersionList = async (connectorId) => {
  try {
    const result = await fetchApi(
      buildApiUrl(API_CONFIG.CONNECTORS.VERSIONS_LIST, { connectorId })
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
 * 查询连接器版本详情（#10）
 *
 * @param {Object} options - 选项对象
 * 包含以下字段：
 * - connectorId: 连接器 ID
 * - versionId: 版本 ID
 *
 * @returns {Promise<Object>} 版本详情响应（config 字段已转为前端表单结构）
 */
export const fetchVersionDetail = async (options) => {
  // 解构传入对象中需要使用的参数
  const { connectorId, versionId } = options;

  try {
    const result = await fetchApi(
      buildApiUrl(API_CONFIG.CONNECTORS.VERSION_DETAIL, { connectorId, versionId })
    );
    if (result?.code !== '200') return result || {};

    // 拆分版本元数据 + connectionConfig
    const detail = result.data || {};
    return {
      code: '200',
      data: {
        ...buildVersionSummary(detail),
        config: transformFromSchemaFormat(detail.connectionConfig),
      },
    };
  } catch {
    return {};
  }
};

/**
 * 保存草稿版本（#11 更新连接器版本）
 *
 * @param {Object} options - 选项对象
 * 包含以下字段：
 * - connectorId: 连接器 ID
 * - versionId: 版本 ID（仅草稿状态可编辑）
 * - config: 前端 apiConfig
 *
 * @returns {Promise<Object>} 保存结果
 */
export const saveDraft = async (options) => {
  // 解构传入对象中需要使用的参数
  const { connectorId, versionId, config } = options;

  try {
    // 将前端表单结构转换为 V3 connectionConfig
    const connectionConfig = transformToSchemaFormat(config);
    const result = await fetchApi(
      buildApiUrl(API_CONFIG.CONNECTORS.VERSION_UPDATE, { connectorId, versionId }),
      {
        method: 'PUT',
        body: JSON.stringify({ connectionConfig }),
      }
    );
    return result || {};
  } catch {
    return {};
  }
};

/**
 * 发布草稿版本（#12）
 * 流程：先调用 #11 保存最新草稿内容，再调用 #12 发布
 *
 * @param {Object} options - 选项对象
 * 包含以下字段：
 * - connectorId: 连接器 ID
 * - versionId: 版本 ID
 * - config: 前端 apiConfig
 *
 * @returns {Promise<Object>} 发布结果
 */
export const publishVersion = async (options) => {
  // 解构传入对象中需要使用的参数
  const { connectorId, versionId, config } = options;

  try {
    // 1. 先保存最新草稿
    const saveRes = await saveDraft({ connectorId, versionId, config });
    if (saveRes?.code !== '200') return saveRes || {};

    // 2. 再调发布接口
    const result = await fetchApi(
      buildApiUrl(API_CONFIG.CONNECTORS.VERSION_PUBLISH, { connectorId, versionId }),
      { method: 'PUT' }
    );
    return result || {};
  } catch {
    return {};
  }
};

/**
 * 创建草稿版本（#8 创建草稿 / #13 复制版本到草稿）
 *
 * @param {Object} options - 选项对象
 * 包含以下字段：
 * - connectorId: 连接器 ID
 * - baseVersionId: 基础版本 ID（传入则走 copy-to-draft，否则走创建空草稿）
 *
 * @returns {Promise<Object>} 创建结果
 */
export const createDraftVersion = async (options) => {
  // 解构传入对象中需要使用的参数
  const { connectorId, baseVersionId } = options;

  try {
    let result;
    if (baseVersionId) {
      // 基于已发布/已失效版本复制到草稿（#13）
      result = await fetchApi(
        buildApiUrl(API_CONFIG.CONNECTORS.VERSION_COPY_TO_DRAFT, {
          connectorId,
          versionId: baseVersionId,
        }),
        { method: 'POST' }
      );
    } else {
      // 创建空草稿（#8）
      result = await fetchApi(
        buildApiUrl(API_CONFIG.CONNECTORS.VERSION_CREATE, { connectorId }),
        { method: 'POST' }
      );
    }
    return result || {};
  } catch {
    return {};
  }
};

/**
 * 失效连接器版本（#14）
 *
 * @param {Object} options - 选项对象
 * 包含以下字段：
 * - connectorId: 连接器 ID
 * - versionId: 版本 ID
 *
 * @returns {Promise<Object>} 失效结果
 */
export const expireVersion = async (options) => {
  // 解构传入对象中需要使用的参数
  const { connectorId, versionId } = options;

  try {
    const result = await fetchApi(
      buildApiUrl(API_CONFIG.CONNECTORS.VERSION_INVALIDATE, { connectorId, versionId }),
      { method: 'PUT' }
    );
    return result || {};
  } catch {
    return {};
  }
};

/**
 * 恢复连接器版本（#15）
 *
 * @param {Object} options - 选项对象
 * 包含以下字段：
 * - connectorId: 连接器 ID
 * - versionId: 版本 ID
 *
 * @returns {Promise<Object>} 恢复结果
 */
export const restoreVersion = async (options) => {
  // 解构传入对象中需要使用的参数
  const { connectorId, versionId } = options;

  try {
    const result = await fetchApi(
      buildApiUrl(API_CONFIG.CONNECTORS.VERSION_RECOVER, { connectorId, versionId }),
      { method: 'PUT' }
    );
    return result || {};
  } catch {
    return {};
  }
};

/**
 * 删除连接器版本（#16）
 *
 * @param {Object} options - 选项对象
 * 包含以下字段：
 * - connectorId: 连接器 ID
 * - versionId: 版本 ID
 *
 * @returns {Promise<Object>} 删除结果
 */
export const deleteVersion = async (options) => {
  // 解构传入对象中需要使用的参数
  const { connectorId, versionId } = options;

  try {
    const result = await fetchApi(
      buildApiUrl(API_CONFIG.CONNECTORS.VERSION_DELETE, { connectorId, versionId }),
      { method: 'DELETE' }
    );
    return result || {};
  } catch {
    return {};
  }
};

/* ========================================
 * 发布前校验
 * ======================================== */

/**
 * 递归校验参数树
 *
 * @param {Object} options - 选项对象
 * 包含以下字段：
 * - params: 参数数组
 * - depth: 当前层级
 * - maxDepth: 最大允许层级
 * - sectionLabel: 错误提示前缀
 *
 * @returns {string|null} 错误描述；null 表示通过
 */
const validateParamTree = (options) => {
  // 解构传入对象中需要使用的参数
  const { params, depth, maxDepth, sectionLabel } = options;

  if (!Array.isArray(params)) return null;

  // 层级超限
  if (depth > maxDepth) {
    return `${sectionLabel}参数层级超过最大限制 ${maxDepth} 层`;
  }

  for (let i = 0; i < params.length; i += 1) {
    const p = params[i];
    // 参数名必填
    if (!p.paramName || !p.paramName.trim()) {
      return `${sectionLabel}存在未填写参数名称的参数`;
    }
    // 类型合法性
    const allowedTypes = ['object', 'array', 'string', 'number', 'boolean'];
    if (!allowedTypes.includes(p.paramType)) {
      return `${sectionLabel}参数 ${p.paramName} 类型非法`;
    }
    // 复杂类型必须包含基础类型子参数
    if (p.paramType === 'object' || p.paramType === 'array') {
      const children = p.children || [];
      const hasPrimitive = children.some((c) => ['string', 'number', 'boolean'].includes(c.paramType));
      if (!hasPrimitive) {
        return `${sectionLabel}参数 ${p.paramName} 必须包含至少一个基础类型子参数`;
      }
      // 递归校验子参数
      const childErr = validateParamTree({
        params: children,
        depth: depth + 1,
        maxDepth,
        sectionLabel,
      });
      if (childErr) return childErr;
    }
  }
  return null;
};

/**
 * 发布前校验
 *
 * @param {Object} apiConfig - 前端表单 apiConfig
 * @returns {Object|null} 返回 { section, message } 表示错误；null 表示通过
 */
export const validateForPublish = (apiConfig) => {
  const cfg = apiConfig || {};

  // 1. 接口配置
  if (!cfg.protocolType) {
    return { section: VALIDATE_SECTION.BASE, message: '协议类型不能为空' };
  }
  if (!cfg.protocolAddress || !cfg.protocolAddress.trim()) {
    return { section: VALIDATE_SECTION.BASE, message: '协议地址不能为空' };
  }
  if (!HTTP_URL_REGEX.test(cfg.protocolAddress.trim())) {
    return { section: VALIDATE_SECTION.BASE, message: '协议地址必须为 http/https 地址' };
  }

  // 2. 认证配置：至少勾选一种
  const authTypes = Array.isArray(cfg.authType) ? cfg.authType : [];
  if (authTypes.length === 0) {
    return { section: VALIDATE_SECTION.AUTH, message: '请至少勾选一种认证方式' };
  }

  // 2.1 通用认证（SOA / APIG / Cookie）：参数名称必填
  for (let i = 0; i < authTypes.length; i += 1) {
    const t = authTypes[i];
    if (COMMON_AUTH_TYPES.includes(t)) {
      const items = (cfg.authRequestSchema || {})[t] || [];
      if (items.length === 0) {
        return { section: VALIDATE_SECTION.AUTH, message: `${t} 认证参数不能为空` };
      }
      const empty = items.find((it) => !it.paramName || !it.paramName.trim());
      if (empty) {
        return { section: VALIDATE_SECTION.AUTH, message: `${t} 参数名称不能为空` };
      }
    }
  }

  // 2.2 数字签名
  if (authTypes.includes(SIGNATURE_AUTH_TYPE)) {
    const sig = cfg.signatureConfig || {};
    if (!sig.paramName || !sig.paramName.trim()) {
      return { section: VALIDATE_SECTION.AUTH, message: '数字签名参数名称不能为空' };
    }
    if (!sig.fixedValue || !sig.fixedValue.trim()) {
      return { section: VALIDATE_SECTION.AUTH, message: '签名固定值不能为空' };
    }
    if (!sig.secret || !sig.secret.trim()) {
      return { section: VALIDATE_SECTION.AUTH, message: '签名密钥不能为空' };
    }
  }

  // 3. 入参校验
  const reqErr = validateParamTree({
    params: cfg.requestSchema || [],
    depth: 1,
    maxDepth: MAX_SCHEMA_DEPTH,
    sectionLabel: '入参',
  });
  if (reqErr) return { section: VALIDATE_SECTION.REQUEST, message: reqErr };

  // 4. 出参校验
  const respErr = validateParamTree({
    params: cfg.responseSchema || [],
    depth: 1,
    maxDepth: MAX_SCHEMA_DEPTH,
    sectionLabel: '出参',
  });
  if (respErr) return { section: VALIDATE_SECTION.RESPONSE, message: respErr };

  return null;
};
