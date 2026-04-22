export const useTrueFetch = true;

export const API_BASE_URL = '';

export const SUBSCRIPTION_STATUS = {
  0: { text: '审核中', color: 'orange' },
  1: { text: '已审核', color: 'green' },
  2: { text: '已中止', color: 'red' }
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