export const STATUS_MAP = {
  0: { text: '草稿', color: 'default' },
  1: { text: '待审', color: 'orange' },
  2: { text: '已发布', color: 'green' },
  3: { text: '已下线', color: 'red' },
};

export const AUTH_TYPE_MAP = {
  0: 'Cookie',
  1: 'SOA',
  2: 'APIG',
  3: 'IAM',
  4: '免认证',
  5: 'AKSK',
  6: 'CLITOKEN',
};

export const HTTP_METHOD_OPTIONS = [
  { value: 'GET', label: 'GET' },
  { value: 'POST', label: 'POST' },
  { value: 'PUT', label: 'PUT' },
  { value: 'DELETE', label: 'DELETE' },
  { value: 'PATCH', label: 'PATCH' },
];

export const API_PROPERTY_PRESETS = [
  { value: 'descriptionCn', label: '中文描述', placeholder: 'API的中文描述' },
  { value: 'descriptionEn', label: '英文描述', placeholder: 'API description in English' },
  { value: 'docUrl', label: '文档链接', placeholder: 'https://docs.example.com/api/xxx' },
  { value: 'rateLimit', label: '速率限制', placeholder: '100/minute' },
  { value: '__custom__', label: '自定义...', placeholder: '输入自定义属性名' },
];

export const AUTH_TYPE_OPTIONS = [
  { value: 0, label: 'Cookie' },
  { value: 1, label: 'SOA' },
  { value: 2, label: 'APIG' },
  { value: 3, label: 'IAM' },
  { value: 4, label: '免认证' },
  { value: 5, label: 'AKSK' },
  { value: 6, label: 'CLITOKEN' },
];

export const NEED_REVIEW_OPTIONS = [
  { value: 'all', label: '全部' },
  { value: 'true', label: '需要审核' },
  { value: 'false', label: '无需审核' },
];

export const IDENTITY_TABS = [
  { key: 'BUSINESS_IDENTITY', label: '业务身份权限' },
  { key: 'PERSONAL_IDENTITY', label: '个人身份权限' },
];

export const BUSINESS_BUSINESS_API_TABS = [
  { key: 'api_business_app_soa', label: 'SOA类型' },
  { key: 'api_business_app_apig', label: 'APIG类型' },
];

export const BUSINESS_PERSONAL_API_TABS = [
  { key: 'api_business_user_soa', label: 'SOA类型' },
  { key: 'api_business_user_apig', label: 'APIG类型' },
];

export const PERSONAL_API_TABS = [
  { key: 'api_personal_user_aksk', label: 'AKSK类型' },
];

export const DEFAULT_API_TYPE = {
  business: 'api_business_app_soa',
  personal: 'api_personal_user_aksk',
};

export const getApiListColumns = ({
  renderApiName,
  renderPath,
  renderMethod,
  renderAuthType,
  renderScope,
  renderStatus,
  renderAction,
}) => [
  {
    title: 'API名称',
    dataIndex: 'nameCn',
    key: 'nameCn',
    render: renderApiName,
  },
  {
    title: '分类',
    dataIndex: 'categoryName',
    key: 'categoryName',
  },
  {
    title: '路径',
    dataIndex: 'path',
    key: 'path',
    render: renderPath,
  },
  {
    title: '方法',
    dataIndex: 'method',
    key: 'method',
    render: renderMethod,
  },
  {
    title: '认证方式',
    dataIndex: 'authType',
    key: 'authType',
    render: renderAuthType,
  },
    {
      title: 'Scope',
      dataIndex: 'permission',
      key: 'scope',
      render: renderScope,
    },
  {
    title: '状态',
    dataIndex: 'status',
    key: 'status',
    render: renderStatus,
  },
  {
    title: '操作',
    key: 'action',
    render: renderAction,
  },
];
