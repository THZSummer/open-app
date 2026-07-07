/**
 * ========================================
 * 连接流编辑器 V2 - 常量配置
 * ========================================
 *
 * 定义编排模式、节点类型、版本状态、按钮映射等常量。
 * 与旧版 FlowEditor/constants.js 完全独立。
 */

import {
  HTTP_REQUEST_CARRIER_TABS,
  HTTP_RESPONSE_CARRIER_TABS,
} from '../../../utils/constants';
import { queryParams } from '../../../utils/common';

// ========================================
// 编排模式
// ========================================
export const FLOW_MODE = {
  SINGLE: 'single',
  SERIAL: 'serial',
  PARALLEL: 'parallel',
};

/**
 * 编排模式元数据
 * 用于 ModePanel 渲染模式选择卡片
 */
export const FLOW_MODE_META = {
  [FLOW_MODE.SINGLE]: {
    title: '单节点',
    desc: '触发器 → 连接器 → 数据输出，节点结构固定，不展示步骤条加号',
    icon: 'Ⅰ',
  },
  [FLOW_MODE.SERIAL]: {
    title: '串行编排',
    desc: '按顺序追加连接器节点和脚本处理节点，连接器数量受应用级上限约束',
    icon: '→',
  },
  [FLOW_MODE.PARALLEL]: {
    title: '并行编排',
    desc: '触发器 → 并行节点（默认 2 个分支）→ 数据输出，每分支固定一个连接器',
    icon: '∥',
  },
};

// ========================================
// 节点类型
// ========================================
export const NODE_TYPE = {
  TRIGGER: 'trigger',
  CONNECTOR: 'connector',
  SCRIPT: 'script',
  PARALLEL: 'parallel',
  OUTPUT: 'output',
};

/**
 * 节点类型展示文案映射
 */
export const NODE_TYPE_LABEL = {
  [NODE_TYPE.TRIGGER]: '触发器',
  [NODE_TYPE.CONNECTOR]: '连接器',
  [NODE_TYPE.SCRIPT]: '脚本处理',
  [NODE_TYPE.PARALLEL]: '并行节点',
  [NODE_TYPE.OUTPUT]: '数据输出',
};

// ========================================
// 版本状态
// ========================================
export const VERSION_STATUS = {
  DRAFT: 1,
  PUBLISHED: 5,
  EXPIRED: 6,
  APPROVING: 2,
  REJECTED: 4,
  WITHDRAWN: 3,
};

/**
 * 版本状态展示映射（用于 Tag 颜色与文案）
 */
export const VERSION_STATUS_MAP = {
  [VERSION_STATUS.DRAFT]: { text: '草稿', color: 'processing' },
  [VERSION_STATUS.PUBLISHED]: { text: '已发布', color: 'success' },
  [VERSION_STATUS.EXPIRED]: { text: '已失效', color: 'default' },
  [VERSION_STATUS.APPROVING]: { text: '审批中', color: 'warning' },
  [VERSION_STATUS.REJECTED]: { text: '已驳回', color: 'error' },
  [VERSION_STATUS.WITHDRAWN]: { text: '已撤回', color: 'default' },
};

/**
 * 各版本状态下可展示的顶部操作按钮列表
 * 每个按钮配置：label（文案）、action（事件标识）、type（antd button type）、danger（是否危险按钮）
 */
export const VERSION_ACTIONS = {
  [VERSION_STATUS.DRAFT]: [
    { label: '更多配置', action: 'moreConfig', type: 'default' },
    { label: '调试', action: 'debug', type: 'default' },
    { label: '保存', action: 'save', type: 'primary' },
    { label: '发布', action: 'publish', type: 'default' },
    { label: '删除', action: 'delete', type: 'default', danger: true },
  ],
  [VERSION_STATUS.PUBLISHED]: [
    { label: '新增草稿', action: 'newDraft', type: 'default' },
    { label: '更多配置', action: 'moreConfig', type: 'default' },
    { label: '调试', action: 'debug', type: 'default' },
    { label: '失效', action: 'expire', type: 'default', danger: true },
  ],
  [VERSION_STATUS.EXPIRED]: [
    { label: '新增草稿', action: 'newDraft', type: 'default' },
    { label: '更多配置', action: 'moreConfig', type: 'default' },
    { label: '调试', action: 'debug', type: 'default' },
    { label: '恢复', action: 'restore', type: 'primary' },
    { label: '删除', action: 'delete', type: 'default', danger: true },
  ],
  [VERSION_STATUS.APPROVING]: [
    { label: '更多配置', action: 'moreConfig', type: 'default' },
    { label: '调试', action: 'debug', type: 'default' },
    { label: '撤回', action: 'withdraw', type: 'default', danger: true },
  ],
  [VERSION_STATUS.REJECTED]: [
    { label: '更多配置', action: 'moreConfig', type: 'default' },
    { label: '调试', action: 'debug', type: 'default' },
    { label: '保存', action: 'save', type: 'primary' },
    { label: '删除', action: 'delete', type: 'default', danger: true },
  ],
  [VERSION_STATUS.WITHDRAWN]: [
    { label: '更多配置', action: 'moreConfig', type: 'default' },
    { label: '调试', action: 'debug', type: 'default' },
    { label: '保存', action: 'save', type: 'primary' },
    { label: '删除', action: 'delete', type: 'default', danger: true },
  ],
};

/**
 * 版本栏按钮渲染顺序（从左到右）
 * 详情按钮独立渲染在最左侧；其余按钮按下方优先级稳定重排。
 * 顺序：新增草稿 / 编辑 / 取消编辑 / 保存 → 发布 / 撤回 → 失效 / 恢复 / 删除
 */
export const VERSION_BUTTON_ORDER = [
  'newDraft',
  'edit',
  'cancelEdit',
  'save',
  'publish',
  'withdraw',
  'expire',
  'restore',
  'delete',
];

// ========================================
// 应用级配置 lookup
// ========================================

/**
 * 连接流全局配置 lookup 查询键
 */
export const FLOW_APP_CONFIG_LOOKUP_KEY = 'CEC.Open/Connector.Platform.Config';

/**
 * 连接流应用级配置 lookup 查询键
 */
export const FLOW_APP_INSTANCE_CONFIG_LOOKUP_KEY = 'CEC.Open/Connector.Platform.{appId}.Config';

/**
 * 连接流 lookup 配置字段映射
 */
export const FLOW_APP_CONFIG_FIELD_MAP = {
  /** 限流配置 */
  'Flow.Max.Qps': 'rateLimitMax',
  /** 串行编排连接器节点最大上限 */
  'Flow.Max.Serial.Connector.Nodes': 'serialConnectorMax',
  /** 并行编排并行节点并行分支上限 */
  'Flow.Max.Parallel.Branches': 'parallelBranchMax',
  /** 连接器超时时间配置 */
  'Node.Max.Timeout.Seconds': 'connectorTimeoutMax',
};

/**
 * 编排模式默认可见性
 */
export const DEFAULT_FLOW_MODE_VISIBILITY = {
  single: true,
  serial: true,
  parallel: true,
};

/**
 * 将 lookup items 转换为连接流上限配置
 * @param {Array} items lookup 配置项
 * @returns {Object} 连接流上限配置
 */
export const transformLookupItemsToFlowConfig = (items = []) => {
  // 非数组配置不参与转换，避免异常响应影响默认值兜底。
  if (!Array.isArray(items)) return {};

  return items.reduce((config, item) => {
    const targetKey = FLOW_APP_CONFIG_FIELD_MAP[item?.itemCode];
    const value = Number(item?.itemValue);

    if (!targetKey || Number.isNaN(value)) return config;

    return {
      ...config,
      [targetKey]: value,
    };
  }, {});
};

/**
 * 从 lookup 响应中提取连接流配置项
 * @param {Object} params 参数对象
 * @returns {Array} lookup 配置项
 */
export const getFlowConfigItems = (params) => {
  // params.res / params.lookupKey
  const { res, lookupKey } = params;
  return res?.data?.lookups?.[lookupKey]?.items || [];
};

// ========================================
// 应用级配置默认上限
// ========================================
export const DEFAULT_APP_LIMITS = {
  /** 限流默认上限 */
  rateLimitMax: 1000,
  /** 连接器超时默认上限（毫秒） */
  connectorTimeoutMax: 300000,
  /** 串行模式连接器数量上限 */
  serialConnectorMax: 3,
  /** 并行模式分支数量上限 */
  parallelBranchMax: 3,
  /** 缓存时间上限（秒） */
  cacheTimeMax: 1296000,
};

/**
 * 解析连接流应用配置
 * @param {Object} params 参数对象
 * @returns {Object} 连接流应用配置
 */
export const parseFlowAppConfig = (params) => {
  // params.globalRes / params.appRes
  const { globalRes, appRes } = params;
  const globalConfig = transformLookupItemsToFlowConfig(
    getFlowConfigItems({ res: globalRes, lookupKey: FLOW_APP_CONFIG_LOOKUP_KEY })
  );
  const appId = queryParams('appId');
  const appConfig = transformLookupItemsToFlowConfig(
    getFlowConfigItems({ res: appRes, lookupKey: FLOW_APP_INSTANCE_CONFIG_LOOKUP_KEY.replace('{appId}', appId) })
  );

  // 按默认配置、全局配置、应用级配置的顺序合并，后面的同名字段覆盖前面的值。
  return {
    flowModeVisibility: DEFAULT_FLOW_MODE_VISIBILITY,
    ...DEFAULT_APP_LIMITS,
    ...globalConfig,
    ...appConfig,
  };
};

// ========================================
// HTTP 请求载体 Tab 配置
// ========================================
export const CARRIER_TABS = HTTP_REQUEST_CARRIER_TABS;

// ========================================
// 二次确认弹窗配置（版本失效 / 撤回 / 删除）
// 使用 utils/common.js 中的 getSecondModalInfo 注入 ACTION_CONFIG
// ========================================

/**
 * 连接流版本失效二次确认弹窗配置
 */
export const FLOW_VERSION_EXPIRE_SECOND_MODAL_INFO = {
  action: 'disable',
  getConfirmText: ({ objectName }) => `确认要将这个连接流版本：${objectName} 置为失效吗？`,
  impactText: '操作影响：失效后该版本不可被使用，已绑定该版本的部署需要切换到其他可用版本。',
};

/**
 * 连接流版本撤回二次确认弹窗配置
 */
export const FLOW_VERSION_WITHDRAW_SECOND_MODAL_INFO = {
  action: 'withdraw',
  getConfirmText: ({ objectName }) => `确认要撤回这个连接流版本：${objectName} 吗？`,
  impactText: '操作影响：撤回后该版本进入"已撤回"状态，将退出审批流程，如需上线需重新发布。',
};

/**
 * 连接流版本删除二次确认弹窗配置
 */
export const FLOW_VERSION_DELETE_SECOND_MODAL_INFO = {
  action: 'delete',
  getConfirmText: ({ objectName }) => `确认要删除这个连接流版本：${objectName} 吗？`,
  impactText: '操作影响：删除后版本配置将无法恢复，仅允许对草稿和已失效的版本执行此操作。',
};

/** 输出节点载体 Tab（无 query） */
export const OUTPUT_CARRIER_TABS = HTTP_RESPONSE_CARRIER_TABS;

// ========================================
// 触发方式
// ========================================
export const TRIGGER_TYPE_OPTIONS = [
  { value: 'http', label: 'HTTP 触发' },
];

// ========================================
// 节点校验错误定位
// ========================================
export const VALIDATE_SECTION = {
  TRIGGER: 'trigger',
  CONNECTOR: 'connector',
  SCRIPT: 'script',
  PARALLEL: 'parallel',
  OUTPUT: 'output',
};