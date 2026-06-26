/**
 * ========================================
 * 连接器编辑页模块 - 常量配置
 * ========================================
 *
 * 定义连接器编辑页面的配置信息、状态映射、Tab 等基本信息
 */

import {
  HTTP_REQUEST_CARRIER_TABS,
  HTTP_RESPONSE_CARRIER_TABS,
} from '../../../utils/constants';

/**
 * API配置的默认状态
 * 入参 / 出参均按 carrier 维度区分，仍以扁平数组维护
 * 对齐 plan-api.md v7.0 connectionConfig 结构
 */
export const DEFAULT_API_CONFIG = {
  protocolType: '',
  protocolAddress: '',
  // 多选认证方式：数组形式，元素取值为 SOA / APIG / Cookie / SIGNATURE
  authType: [],
  // 各认证方式对应的参数列表，按认证类型分组：{ SOA: [...], APIG: [...], Cookie: [...] }
  authRequestSchema: {},
  // 数字签名独立配置：参数名 / carrier / 固定值 / 密钥
  signatureConfig: {
    paramName: 'X-Signature',
    carrier: 'header',
    fixedValue: 'signature',
    secret: '',
  },
  headerSchema: [],
  requestSchema: [],
  responseSchema: [],
  // ===== V3 新增字段（对齐 plan-json-schema.md §5.2 connectionConfig） =====
  // 连接器中英文标签（版本快照）
  labelCn: '',
  labelEn: '',
  // URL 白名单规则数组（每项 { pattern, description }）
  urlWhitelist: [],
  // 单次调用超时（毫秒）
  timeoutMs: 3000,
  // 出站限流配置（maxQps / maxConcurrency）
  rateLimitConfig: null,
  // SYSTOKEN 认证账号白名单
  sysAccountWhitelist: [],
};

/**
 * 单个认证方式的默认参数项工厂
 * @param {Object} options
 * options.paramName 默认参数名
 * options.fixedValue 默认固定值
 */
const buildAuthParam = (options) => {
  // options.paramName: 参数名称
  // options.fixedValue: 固定值，用于页面值来源展示
  const { paramName, fixedValue } = options;
  console.log('paramName', paramName);
  console.log('fixedValue', fixedValue);
  return {
    paramName,
    fixedValue,
    paramType: 'string',
    description: '',
    carrier: 'header',
    children: [],
  };
};

/**
 * 认证Schema映射
 * 根据认证类型返回对应的默认参数配置
 */
export const AUTH_SCHEMA_MAP = {
  SOA: [
    buildAuthParam({ paramName: 'X-Soa-Token', fixedValue: 'soaToken' }),
  ],
  APIG: [
    buildAuthParam({ paramName: 'apigAppSecret', fixedValue: 'apigAppSecret' }),
    buildAuthParam({ paramName: 'apigAppKey', fixedValue: 'apigAppKey' }),
  ],
  Cookie: [
    buildAuthParam({ paramName: 'Cookie', fixedValue: 'Cookie' }),
  ],
};

/**
 * HTTP方法选项
 * 用于协议类型选择
 */
export const HTTP_METHOD_OPTIONS = [
  'GET',
  'POST',
  'PUT',
  'DELETE',
];

/**
 * 认证类型选项（多选）
 * SOA / APIG / Cookie / 数字签名
 */
export const AUTH_TYPE_OPTIONS = [
  { value: 'SOA', label: 'SOA' },
  { value: 'APIG', label: 'APIG' },
  { value: 'Cookie', label: 'Cookie' },
  { value: 'SIGNATURE', label: '数字签名' },
];

/**
 * 通用认证方式标识（使用统一参数格式）
 * 数字签名 SIGNATURE 单独维护
 */
export const COMMON_AUTH_TYPES = ['SOA', 'APIG', 'Cookie'];

/**
 * 数字签名认证标识
 */
export const SIGNATURE_AUTH_TYPE = 'SIGNATURE';

/**
 * 认证类型中文名称映射（用于子区块标题）
 */
export const AUTH_TYPE_NAMES = {
  SOA: 'SOA认证',
  APIG: 'APIG认证',
  Cookie: 'Cookie认证',
  SIGNATURE: '数字签名认证',
};

/**
 * 认证参数 carrier 可选项
 */
export const AUTH_CARRIER_OPTIONS = ['header', 'body', 'query'];

/**
 * Cookie 认证字段映射占位文案
 */
export const COOKIE_FIELD_MAPPING_PLACEHOLDER = '从连接流引用参数配置';

/**
 * 数字签名固定值默认值
 */
export const SIGNATURE_DEFAULT_FIXED_VALUE = 'X-Signature';

/**
 * 认证参数行展示配置
 * 用于统一控制通用认证和数字签名的值来源列展示差异
 */
export const AUTH_PARAM_ROW_CONFIG = {
  // SOA 认证：展示系统值来源
  SOA: {
    valuePlaceholder: '值来源',
  },
  // APIG 认证：展示系统值来源
  APIG: {
    valuePlaceholder: '值来源',
  },
  // Cookie 认证：值来源由连接流引用参数配置
  Cookie: {
    value: '',
    valuePlaceholder: COOKIE_FIELD_MAPPING_PLACEHOLDER,
  },
  // 数字签名：展示固定签名值，并额外展示签名密钥
  SIGNATURE: {
    value: SIGNATURE_DEFAULT_FIXED_VALUE,
    valuePlaceholder: '签名固定值',
    showSecret: true,
  },
};

/**
 * Schema编辑器载体选项（仅请求）
 */
export const REQUEST_CARRIER_OPTIONS = ['header', 'body', 'query'];

/**
 * Schema编辑器载体选项（仅响应）
 */
export const RESPONSE_CARRIER_OPTIONS = ['header', 'body'];

/**
 * 入参 Tab 配置
 * label 用于 Tab 标题，carrier 用于按位置过滤参数
 */
export const REQUEST_TABS = HTTP_REQUEST_CARRIER_TABS;

/**
 * 出参 Tab 配置
 */
export const RESPONSE_TABS = HTTP_RESPONSE_CARRIER_TABS;

/**
 * 请求Schema配置（入参，全 carrier）
 * 传入 SchemaEditorV2 的精简 props
 */
export const REQUEST_SCHEMA_CONFIG = {
  schemaType: 'requestSchema',
  editable: true,
  carrierOptions: REQUEST_CARRIER_OPTIONS,
};

/**
 * 响应Schema配置（出参，header/body）
 * 传入 SchemaEditorV2 的精简 props
 */
export const RESPONSE_SCHEMA_CONFIG = {
  schemaType: 'responseSchema',
  editable: true,
  carrierOptions: RESPONSE_CARRIER_OPTIONS,
};

/**
 * 版本状态枚举
 * 对齐 plan-api.md §1.8.2：1=草稿 / 2=已发布 / 3=已失效 / 4=物理删除
 */
export const VERSION_STATUS = {
  DRAFT: 1,
  PUBLISHED: 2,
  EXPIRED: 3,
  DELETED: 4,
};

/**
 * 版本状态展示映射
 * text：展示文案；color：Ant Design Tag 颜色
 */
export const VERSION_STATUS_MAP = {
  [VERSION_STATUS.DRAFT]: { text: '草稿', color: 'processing' },
  [VERSION_STATUS.PUBLISHED]: { text: '已发布', color: 'success' },
  [VERSION_STATUS.EXPIRED]: { text: '已失效', color: 'default' },
  [VERSION_STATUS.DELETED]: { text: '已删除', color: 'default' },
};

/**
 * 参数嵌套最大层级（发布校验上限）
 */
export const MAX_SCHEMA_DEPTH = 10;

/**
 * 校验错误类型
 * 用于发布前校验失败时，定位到对应的页面 section
 */
export const VALIDATE_SECTION = {
  BASE: 'base',
  AUTH: 'auth',
  REQUEST: 'request',
  RESPONSE: 'response',
};

/**
 * URL 简易正则（http / https）
 */
export const HTTP_URL_REGEX = /^https?:\/\/.+/i;

/**
 * 连接器版本失效二次确认弹窗配置
 */
export const CONNECTOR_VERSION_EXPIRE_SECOND_MODAL_INFO = {
  action: 'disable',
  getConfirmText: ({ objectName }) => `确认要将这个连接器版本：${objectName} 置为失效吗？`,
  impactText: '操作影响：失效后该版本不可被使用，已引用此版本的下游能力会同步失效，请先切换到其他可用版本。',
};

/**
 * 连接器版本删除二次确认弹窗配置
 */
export const CONNECTOR_VERSION_DELETE_SECOND_MODAL_INFO = {
  action: 'delete',
  getConfirmText: ({ objectName }) => `确认要删除这个连接器版本：${objectName} 吗？`,
  impactText: '操作影响：删除后版本配置将无法恢复，仅允许对草稿和已失效的版本执行此操作。',
};
