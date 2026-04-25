export const PAGE_SIZE_OPTIONS = [10, 20, 50];

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

export const getApiManagementColumns = ({
  renderScope,
  renderAuthType,
  renderStatus,
  renderAction,
}) => [
  {
    title: '权限名称',
    dataIndex: ['permission', 'nameCn'],
    key: 'nameCn',
  },
  {
    title: 'scope',
    dataIndex: ['permission', 'scope'],
    key: 'scope',
    render: renderScope,
  },
  {
    title: '认证方式',
    dataIndex: 'authType',
    key: 'authType',
    render: renderAuthType,
  },
  {
    title: '分类',
    dataIndex: ['category', 'nameCn'],
    key: 'category',
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
