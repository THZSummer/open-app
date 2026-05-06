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
};

export const RESOURCE_STATUS = {
  0: { text: '草稿', color: 'default' },
  1: { text: '待审', color: 'orange' },
  2: { text: '已发布', color: 'green' },
  3: { text: '已下线', color: 'red' }
};

export const PROPERTY_PRESETS = [
  { value: 'descriptionCn', label: '中文描述', placeholder: '回调的中文描述' },
  { value: 'descriptionEn', label: '英文描述', placeholder: 'Callback description in English' },
  { value: 'docUrl', label: '文档链接', placeholder: 'https://docs.example.com/callback/xxx' },
  { value: 'timeout', label: '超时时间', placeholder: '30000 (毫秒)' },
  { value: 'retryCount', label: '重试次数', placeholder: '3' },
  { value: '__custom__', label: '自定义...', placeholder: '输入自定义属性名' },
];

export const EVENT_CHANNEL_TYPE = {
  0: 'MQS',
  1: 'WebHook'
};

export const CALLBACK_CHANNEL_TYPE = {
  0: 'WebHook',
  1: 'SSE',
  2: 'WebSocket'
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