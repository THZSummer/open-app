/**
 * ========================================
 * 连接器编辑页模块 - 常量配置
 * ========================================
 *
 * 定义连接器编辑页面的配置信息、状态映射、Tab 等基本信息
 */

/**
 * 页面配置信息
 * 定义连接器编辑页面的标题等基本信息
 */
export const editorPageInfo = {
  createTitle: '新建连接器',
  editTitle: '编辑连接器',
};

/**
 * 触发类型映射
 */
export const TRIGGER_TYPE_MAP = {
  webhook: { text: 'Webhook', color: 'blue' },
  api: { text: 'API轮询', color: 'green' },
  schedule: { text: '定时触发', color: 'purple' },
};

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
    paramName: '',
    carrier: 'header',
    fixedValue: 'X-Signature',
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
 */
const buildAuthParam = (options) => {
  // options.paramName: 参数名称
  const { paramName } = options;
  return {
    paramName,
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
    buildAuthParam({ paramName: 'X-Huawei-Auth' }),
  ],
  APIG: [
    buildAuthParam({ paramName: 'X-HW-ID' }),
    buildAuthParam({ paramName: 'X-HW-APPKEY' }),
  ],
  Cookie: [
    buildAuthParam({ paramName: 'Cookie' }),
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
  'PATCH',
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
 * Schema编辑器载体选项
 * 定义参数可以放置的位置
 */
export const CARRIER_OPTIONS = ['header', 'body', 'query'];

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
export const REQUEST_TABS = [
  { key: 'header', label: 'HTTP 请求头', carrier: 'header' },
  { key: 'body', label: 'HTTP 请求体', carrier: 'body' },
  { key: 'query', label: 'URL 查询参数', carrier: 'query' },
];

/**
 * 出参 Tab 配置
 */
export const RESPONSE_TABS = [
  { key: 'header', label: 'HTTP 响应头', carrier: 'header' },
  { key: 'body', label: 'HTTP 响应体', carrier: 'body' },
];

/**
 * 认证请求Schema配置（通用认证：SOA / APIG / Cookie 共用）
 */
export const AUTH_REQUEST_SCHEMA_CONFIG = {
  schemaType: 'authRequestSchema',
  editable: true,
  showCarrier: true,
  carrierOptions: CARRIER_OPTIONS,
  typeOptions: ['string'],
  valueInputType: 'fieldName',
  showActionButtons: false,
  lockedFields: {
    paramName: true,
    paramType: true,
    carrier: false,
  },
};

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
