export const STATUS_MAP = {
  0: { text: '草稿', color: 'default' },
  1: { text: '待审', color: 'orange' },
  2: { text: '已发布', color: 'green' },
  3: { text: '已下线', color: 'red' },
};

export const EVENT_PROPERTY_PRESETS = [
  { value: 'descriptionCn', label: '中文描述', placeholder: '事件的中文描述' },
  { value: 'descriptionEn', label: '英文描述', placeholder: 'Event description in English' },
  { value: 'docUrl', label: '文档链接', placeholder: 'https://docs.example.com/event/xxx' },
  { value: 'dataSchema', label: '数据结构', placeholder: 'JSON Schema 或数据格式说明' },
  { value: '__custom__', label: '自定义...', placeholder: '输入自定义属性名' },
];

export const getEventListColumns = ({
  renderEventName,
  renderTopic,
  renderScope,
  renderStatus,
  renderAction,
}) => [
    {
      title: '事件名称',
      dataIndex: 'nameCn',
      key: 'nameCn',
      render: renderEventName,
    },
    {
      title: '分类',
      dataIndex: 'categoryName',
      key: 'categoryName',
    },
    {
      title: 'Topic',
      dataIndex: 'topic',
      key: 'topic',
      render: renderTopic,
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
