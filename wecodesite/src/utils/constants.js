export const useTrueFetch = true;

export const API_BASE_URL = '';

export const PAGE_SIZE_OPTIONS = [10, 20, 50];

export const INIT_PAGECONFIG = {
  pageSize: 10,
  curPage: 1,
  total: 0,
}

export const SUBSCRIPTION_STATUS = {
  0: { text: '审核中', color: 'orange' },
  1: { text: '已审核', color: 'green' },
  2: { text: '已中止', color: 'red' },
  3: { text: '已撤回', color: 'default' }
};

export const RESOURCE_STATUS = {
  0: { text: '草稿', color: 'default' },
  1: { text: '待审', color: 'orange' },
  2: { text: '已发布', color: 'green' },
  3: { text: '已下线', color: 'red' }
};

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
  0: 'SOA',
  1: 'APIG'
};

export const formatCategoryPath = (category) => {
  if (!category) return '';
  if (typeof category === 'string') return category;
  return category.categoryPath?.join(' > ') || category.nameCn || '';
};

export const getPermissionName = (permission) => {
  if (!permission) return '-';
  if (typeof permission === 'string') return permission;
  return permission.nameCn || permission.scope || '-';
};

export const ADMIN_MENU_CONFIG = [
  { title: 'API列表', router: '/admin/apis' },
  { title: '事件列表', router: '/admin/events' },
  { title: '回调列表', router: '/admin/callbacks' },
  { title: '分类列表', router: '/admin/categories' },
  { title: '审批中心', router: '/admin/approvals' }
];