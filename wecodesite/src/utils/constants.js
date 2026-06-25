export const PAGE_SIZE_OPTIONS = [10, 20, 50];

export const INIT_PAGECONFIG = {
  pageSize: 10,
  curPage: 1,
  total: 0,
}

export const SUBSCRIPTION_STATUS = {
  0: { text: '审核中', color: 'orange' },
  1: { text: '已审核', color: 'green' },
  2: { text: '已驳回', color: 'red' },
  3: { text: '已撤回', color: 'default' },
  4: { text: '已中止', color: 'red' },
  99: { text: '未订阅', color: 'default' }
};

export const STATUS_MAP = {
  0: { text: '已撤回', color: 'default' },
  1: { text: '待审', color: 'orange' },
  2: { text: '已发布', color: 'green' },
  3: { text: '已下线', color: 'red' },
};

export const PROPERTY_PRESETS = [
  { value: 'descriptionCn', label: '中文描述', placeholder: '回调的中文描述' },
  { value: 'descriptionEn', label: '英文描述', placeholder: 'Callback description in English' },
  { value: 'docUrl', label: '文档链接', placeholder: 'https://docs.example.com/callback/xxx' },
  { value: 'timeout', label: '超时时间', placeholder: '30000 (毫秒)' },
  { value: 'retryCount', label: '重试次数', placeholder: '3' },
  { value: '__custom__', label: '自定义...', placeholder: '输入自定义属性名' },
];

export const CHANNEL_TYPE = {
  0: 'MQS',
  1: 'WebHook',
  2: 'SSE',
  3: 'WebSocket'
};

export const AUTH_TYPE = {
  0: 'Cookie',
  1: 'SOA',
  2: 'APIG',
  3: 'IAM',
  4: '免认证',
  5: 'AKSK',
  6: 'CLITOKEN',
};

export const ADMIN_MENU_CONFIG = [
  { title: '分类列表', router: '/admin/categories' },
  { title: 'API列表', router: '/admin/apis' },
  { title: '事件列表', router: '/admin/events' },
  { title: '回调列表', router: '/admin/callbacks' },
  { title: '审批中心', router: '/admin/approvals' }
];

export const REMIND_BUSINESSTYPE = {
  api: 'api_permission_apply',
  event: 'event_permission_apply',
  callback: 'callback_permission_apply',
}

// ==================== 操作确认弹窗配置 ====================

export const ACTION_CONFIG = {
  delete: {
    content: '确定要删除吗？',
    confirmButtonText: '确认删除',
    loadingText: '删除中...',
    dangerColor: '#ff4d4f'
  },
  withdraw: {
    content: '确定要撤回吗？撤回后将无法恢复。',
    confirmButtonText: '确认撤回',
    loadingText: '撤回中...',
    dangerColor: '#faad14'
  },
  stop: {
    content: '确定要停止吗？',
    confirmButtonText: '确认停止',
    loadingText: '停止中...',
    dangerColor: '#ff4d4f'
  },
  // 失效（连接器/连接流/版本等）
  disable: {
    content: '确定要失效吗？',
    confirmButtonText: '确认失效',
    loadingText: '处理中...',
    dangerColor: '#ff4d4f'
  }
};

// ==================== 应用管理模块枚举 (APP-MGMT-001) ====================

export const APP_TYPE_MAP = {
  0: { text: '个人应用', color: 'default' },
  1: { text: '业务应用', color: 'blue' },
};

export const APP_SUB_TYPE_MAP = {
  0: { text: '存量个人应用', color: 'orange' },
  1: { text: '技能', color: 'default' },
  2: { text: '个人助理', color: 'default' },
  3: { text: '业务助理', color: 'default' },
  4: { text: '业务应用-标准', color: 'blue' },
};

export const VERIFY_TYPE_MAP = {
  0: { text: 'Cookie', label: 'Cookie', needApiSecret: false, order: 0 },
  2: { text: '数字签名', label: '数字签名', needApiSecret: true, order: 1 },
  1: { text: 'SOAHeader', label: 'SOAHeader', needApiSecret: false, order: 2 },
  3: { text: 'SOAURL', label: 'SOAURL', needApiSecret: false, order: 3 },
  4: { text: 'APIG', label: 'APIG', needApiSecret: false, order: 4 },
  5: { text: 'IntegrateToken', label: 'Token', needApiSecret: false, order: 5 },
};

export const ROLE_MAP = {
  0: { text: '开发者', color: 'default' },
  1: { text: '所有者', color: 'gold' },
  2: { text: '管理员', color: 'blue' },
};

export const VERSION_STATUS_MAP = {
  1: { text: '待发布', color: 'blue' },
  2: { text: '审批中', color: 'orange' },
  3: { text: '审批未通过', color: 'red' },
  4: { text: '已发布', color: 'green' },
};

/** 版本状态常量（与后端 VersionStatusEnum 一致） */
export const VERSION_STATUS = {
  DRAFT: 1,         // 待发布
  UNDER_REVIEW: 2,  // 审批中
  REJECTED: 3,      // 审批未通过
  APPROVED: 4,      // 已发布
};

export const ABILITY_TYPE_MAP = {
  1: { text: '群置顶', icon: 'PushpinOutlined' },
  2: { text: '群通知', icon: 'NotificationOutlined' },
  3: { text: '链接增强', icon: 'LinkOutlined' },
  4: { text: '点对点通知', icon: 'MessageOutlined' },
  5: { text: 'We码', icon: 'QrcodeOutlined' },
  6: { text: '应用入群通知', icon: 'TeamOutlined' },
  7: { text: '助手广场卡片', icon: 'CreditCardOutlined' },
};

/** 能力场景分组（spec §2.4：当前所有能力均归属于「消息场景」） */
export const ABILITY_SCENE_MAP = {
  message: {
    name: '消息场景',
    description: '群置顶、群通知、链接增强、点对点通知、we码、助手广场卡片',
    types: [1, 2, 3, 4, 5, 7], // 包含的能力类型（排除6=应用入群通知）
  },
};

export const FORM_VALIDATION_RULES = {
  appName: { max: 255, message: '不超过255字符' },
  appDesc: { max: 2000, message: '描述不超过2000字符' },
  versionCode: { pattern: /^\d+\.\d+\.\d+$/, message: '输入版本号不符合要求' },
  versionDesc: { max: 2000, message: '描述不超过2000字符' },
  apiSecret: { pattern: /^(?=.*[A-Za-z])(?=.*\d)[A-Za-z\d]{16}$/, message: 'AppSecret 格式错误，需16位同时含字母和数字' },
};

// ==================== 连接器列表（对齐 plan-api.md §1.8.1） ====================

/**
 * 连接器状态枚举
 * 1=有效不可用（无已发布版本） / 2=有效可用（有已发布版本） / 3=已失效 / 4=物理删除
 */
export const CONNECTOR_STATUS = {
  INACTIVE: 1,   // 有效不可用
  ACTIVE: 2,     // 有效可用
  INVALID: 3,    // 已失效
  DELETED: 4,    // 物理删除
};

/**
 * 连接器状态映射（用于表格展示）
 */
export const CONNECTOR_STATUS_MAP = {
  1: { text: '有效不可用', color: 'default' },
  2: { text: '有效可用', color: 'green' },
  3: { text: '已失效', color: 'red' },
  4: { text: '已删除', color: 'default' },
};

// ==================== 连接流列表（对齐 plan-api.md §1.8.3） ====================

/**
 * 连接流生命周期状态枚举
 * 1=已停止 / 2=运行中 / 3=已失效 / 4=物理删除
 */
export const FLOW_LIFECYCLE_STATUS = {
  STOPPED: 1,    // 已停止
  RUNNING: 2,    // 运行中
  INVALID: 3,    // 已失效
  DELETED: 4,    // 物理删除
};

/**
 * 连接流状态映射（用于表格展示）
 */
export const FLOW_LIFECYCLE_STATUS_MAP = {
  1: { text: '已停止', color: 'red' },
  2: { text: '运行中', color: 'green' },
  3: { text: '已失效', color: 'default' },
  4: { text: '已删除', color: 'default' },
};

// ==================== 执行记录（对齐 plan-api.md §1.8.5 / §1.8.6） ====================

/**
 * 执行记录状态枚举
 * 0=success / 1=failed / 2=timeout
 */
export const EXECUTION_STATUS = {
  SUCCESS: 0,
  FAILED: 1,
  TIMEOUT: 2,
};

/**
 * 执行记录状态映射
 */
export const EXECUTION_STATUS_MAP = {
  0: { text: '成功', color: 'green' },
  1: { text: '失败', color: 'red' },
  2: { text: '超时', color: 'orange' },
};

/**
 * 触发方式枚举
 * 1=http（HTTP 触发） / 2=debug（调试触发）
 */
export const TRIGGER_TYPE = {
  HTTP: 1,
  DEBUG: 2,
};

/**
 * 触发方式映射
 */
export const TRIGGER_TYPE_MAP = {
  1: { text: 'HTTP触发', color: 'blue' },
  2: { text: '调试触发', color: 'purple' },
};

export const FILE_VALIDATION = {
  icon: {
    types: ['image/png', 'image/jpeg', 'image/jpg'],
    maxSize: 100 * 1024,
    width: 128,
    height: 128,
    typeMessage: '图标仅支持png/jpg/jpeg',
    sizeMessage: '图标大小不超过100KB',
    dimensionMessage: '图标尺寸必须为128x128px',
  },
  diagram: {
    types: ['image/png', 'image/jpeg', 'image/jpg'],
    maxSize: 500 * 1024,
    width: 360,
    height: 200,
    typeMessage: '示意图仅支持png/jpg/jpeg',
    sizeMessage: '示意图大小不超过500KB',
    dimensionMessage: '示意图尺寸360x200px',
  },
};

/**
 * HTTP 请求载体 Tab 配置
 */
export const HTTP_REQUEST_CARRIER_TABS = [
  { key: 'header', label: 'HTTP 请求头', carrier: 'header' },
  { key: 'body', label: 'HTTP 请求体', carrier: 'body' },
  { key: 'query', label: 'URL 查询参数', carrier: 'query' },
];

/**
 * HTTP 响应载体 Tab 配置
 */
export const HTTP_RESPONSE_CARRIER_TABS = [
  { key: 'header', label: 'HTTP 响应头', carrier: 'header' },
  { key: 'body', label: 'HTTP 响应体', carrier: 'body' },
];