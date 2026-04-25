export const PAGE_SIZE_OPTIONS = [10, 20, 50];

export const getEventColumns = ({
  renderEventName,
  renderChannelType,
  renderStatus,
  renderAction,
}) => [
    {
      title: '事件名称',
      dataIndex: ['permission', 'nameCn'],
      key: 'nameCn',
      render: renderEventName,
    },
    {
      title: '分类',
      dataIndex: ['category', 'nameCn'],
      key: 'category',
    },
    {
      title: '所需权限',
      dataIndex: ['permission', 'nameCn'],
      key: 'permissionName',
    },
    {
      title: '订阅方式',
      dataIndex: 'channelType',
      key: 'channelType',
      render: renderChannelType,
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

export const getEventDrawerColumns = ({
  renderEventName,
  renderNeedApproval,
  renderIsSubScribed,
  renderAction,
}) => [
    {
      title: '事件名称',
      dataIndex: 'nameCn',
      key: 'nameCn',
      render: renderEventName,
    },
    {
      title: '是否需要审核',
      dataIndex: 'needApproval',
      key: 'needApproval',
      render: renderNeedApproval,
    },
    {
      title: '订阅状态',
      dataIndex: 'isSubscribed',
      key: 'isSubscribed',
      width: 100,
      render: renderIsSubScribed,
    },
    {
      title: '操作',
      key: 'action',
      render: renderAction,
    },
  ];
