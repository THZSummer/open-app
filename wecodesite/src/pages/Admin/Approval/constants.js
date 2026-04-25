export const STATUS_MAP = {
  0: { text: '待审', color: 'orange' },
  1: { text: '已通过', color: 'green' },
  2: { text: '已拒绝', color: 'red' },
  3: { text: '已撤销', color: 'default' },
};

/**
 * 审批级别映射
 */
export const LEVEL_MAP = {
  'resource': { text: '资源审批', color: 'blue' },
  'scene': { text: '场景审批', color: 'orange' },
  'global': { text: '全局审批', color: 'green' },
};

/**
 * 审批节点状态映射（用于显示审批节点处理状态）
 * 后端节点状态定义：0=待审批, 1=已通过, 2=已拒绝
 */
export const NODE_STATUS_MAP = {
  null: { text: '待审', color: 'default' },
  0: { text: '待审批', color: 'default' },
  1: { text: '已同意', color: 'success' },
  2: { text: '已拒绝', color: 'error' },
};

export const APPROVAL_TYPE_MAP = {
  'resource_register': '资源注册',
  'permission_apply': '权限申请',
};

export const APPROVAL_TABS = [
  { key: 'pending', label: '我的待审' },
  { key: 'mine', label: '我发起的' },
  { key: 'all', label: '全部' },
];

export const getApprovalColumns = ({
  renderStatus,
  renderAction,
}) => [
    {
      title: '申请编号',
      dataIndex: 'id',
      key: 'id',
    },
    {
      title: '申请人',
      dataIndex: 'applicantName',
      key: 'applicantName',
    },
    {
      title: '业务类型',
      dataIndex: 'businessType',
      key: 'businessType',
    },
    {
      title: '业务名称',
      dataIndex: 'businessName',
      key: 'businessName',
    },
    {
      title: '业务ID',
      dataIndex: 'businessId',
      key: 'businessId',
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

export const getMyApprovalColumns = ({
  renderStatus,
  renderAction,
}) => [
    {
      title: '申请编号',
      dataIndex: 'id',
      key: 'id',
    },
    {
      title: '业务类型',
      dataIndex: 'businessType',
      key: 'businessType',
    },
    {
      title: '业务名称',
      dataIndex: 'businessName',
      key: 'businessName',
    },
    {
      title: '业务ID',
      dataIndex: 'businessId',
      key: 'businessId',
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
