export const STATUS_MAP = {
  0: { text: '草稿', color: 'default' },
  1: { text: '待审', color: 'orange' },
  2: { text: '已发布', color: 'green' },
  3: { text: '已下线', color: 'red' },
};

export const CALLBACK_PROPERTY_PRESETS = [
  { value: 'descriptionCn', label: '中文描述', placeholder: '回调的中文描述' },
  { value: 'descriptionEn', label: '英文描述', placeholder: 'Callback description in English' },
  { value: 'docUrl', label: '文档链接', placeholder: 'https://docs.example.com/callback/xxx' },
  { value: 'timeout', label: '超时时间', placeholder: '30000 (毫秒)' },
  { value: 'retryCount', label: '重试次数', placeholder: '3' },
  { value: '__custom__', label: '自定义...', placeholder: '输入自定义属性名' },
];

export const getCallbackListColumns = ({
  renderCallbackName,
  renderScope,
  renderStatus,
  renderAction,
}) => [
    {
      title: '回调名称',
      dataIndex: 'nameCn',
      key: 'nameCn',
      render: renderCallbackName,
    },
    {
      title: '分类',
      dataIndex: 'categoryName',
      key: 'categoryName',
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
