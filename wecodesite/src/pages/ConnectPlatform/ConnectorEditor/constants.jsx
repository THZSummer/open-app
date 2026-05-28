/**
 * ========================================
 * 连接器编辑页模块 - 常量配置
 * ========================================
 *
 * 定义连接器编辑页面的配置信息、状态映射等
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
 */
export const DEFAULT_API_CONFIG = {
  protocolType: '',
  protocolAddress: '',
  authType: '',
  authRequestSchema: [],
  headerSchema: [],
  requestSchema: [],
  responseSchema: [],
};

/**
 * 认证Schema映射
 * 根据认证类型返回对应的默认参数配置
 */
export const AUTH_SCHEMA_MAP = {
  SOA: [
    {
      paramName: 'X-Huawei-Auth',
      paramType: 'string',
      description: '',
      carrier: 'header',
      children: [],
    },
  ],
  APIG: [
    {
      paramName: 'X-HW-ID',
      paramType: 'string',
      description: '',
      carrier: 'header',
      children: [],
    },
    {
      paramName: 'X-HW-APPKEY',
      paramType: 'string',
      description: '',
      carrier: 'header',
      children: [],
    },
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
 * 认证类型选项
 * 用于认证方式选择
 */
export const AUTH_TYPE_OPTIONS = [
  { value: 'SOA', label: 'SOA' },
  { value: 'APIG', label: 'APIG' },
];

/**
 * Schema编辑器载体选项
 * 定义参数可以放置的位置
 */
export const CARRIER_OPTIONS = ['header', 'body', 'query'];

/**
 * Schema编辑器载体选项（仅请求）
 * 请求参数可以包含header、body、query
 */
export const REQUEST_CARRIER_OPTIONS = ['header', 'body', 'query'];

/**
 * Schema编辑器载体选项（仅响应）
 * 响应参数只能包含header、body
 */
export const RESPONSE_CARRIER_OPTIONS = ['header', 'body'];

/**
 * 认证请求Schema配置
 * 用于认证参数配置的SchemaEditor组件
 *
 * 配置说明：
 * - schemaType: 数据在apiConfig中的字段名
 * - editable: 是否可编辑
 * - showCarrier: 是否显示载体选择
 * - carrierOptions: 可选的载体类型
 * - typeOptions: 可选的参数类型
 * - valueInputType: 值输入类型
 * - showActionButtons: 是否显示操作按钮
 * - lockedFields: 锁定字段配置
 *   - paramName: 参数名锁定（SOA/APIG模式下由系统决定，不可修改）
 *   - paramType: 参数类型锁定（固定为string）
 *   - carrier: 载体类型可修改
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
 * 请求Schema配置
 * 用于入参配置的SchemaEditor组件
 *
 * 配置说明：
 * - schemaType: 数据在apiConfig中的字段名
 * - editable: 是否可编辑
 * - showCarrier: 是否显示载体选择
 * - carrierOptions: 可选的载体类型
 */
export const REQUEST_SCHEMA_CONFIG = {
  schemaType: 'requestSchema',
  editable: true,
  showCarrier: true,
  carrierOptions: REQUEST_CARRIER_OPTIONS,
};

/**
 * 响应Schema配置
 * 用于出参配置的SchemaEditor组件
 *
 * 配置说明：
 * - schemaType: 数据在apiConfig中的字段名
 * - editable: 是否可编辑
 * - showCarrier: 是否显示载体选择
 * - carrierOptions: 可选的载体类型（响应不支持query）
 */
export const RESPONSE_SCHEMA_CONFIG = {
  schemaType: 'responseSchema',
  editable: true,
  showCarrier: true,
  carrierOptions: RESPONSE_CARRIER_OPTIONS,
};
