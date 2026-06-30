// API config keys (NOT actual URLs - those are in web.config.js)
export const API_CONFIG = {
  PENDING_LIST: 'APPROVAL_PENDING_LIST',
  PUBLISHED_LIST: 'APPROVAL_PUBLISHED_LIST',
  PROCESS: 'APPROVAL_PROCESS',
};

export const APPROVAL_ACTION = {
  APPROVE: 0,
  REJECT: 1,
};

// Pending tab columns
export const getPendingColumns = ({ renderAppName, renderAction }) => [
  { title: '应用名称', dataIndex: 'appNameCn', key: 'appNameCn', width: 150, render: renderAppName },
  { title: '版本号', dataIndex: 'versionNo', key: 'versionNo', width: 90 },
  { title: '应用ID', dataIndex: 'hisAppId', key: 'hisAppId', width: 120 },
  { title: '申请时间', dataIndex: 'createTime', key: 'createTime', width: 155 },
  { title: '应用能力', dataIndex: 'capabilityNames', key: 'capabilityNames', width: 160, ellipsis: true },
  { title: '申请账号', dataIndex: 'applicantId', key: 'applicantId', width: 100 },
  { title: '操作', key: 'action', width: 180, fixed: 'right', render: renderAction },
];

// Published tab columns
export const getPublishedColumns = ({ renderAppName, renderAction }) => [
  { title: '应用名称', dataIndex: 'appNameCn', key: 'appNameCn', width: 150, render: renderAppName },
  { title: '应用能力', dataIndex: 'capabilityNames', key: 'capabilityNames', width: 160, ellipsis: true },
  { title: '版本号', dataIndex: 'versionNo', key: 'versionNo', width: 90 },
  { title: '应用ID', dataIndex: 'hisAppId', key: 'hisAppId', width: 120 },
  { title: '创建时间', dataIndex: 'createTime', key: 'createTime', width: 155 },
  { title: '申请账号', dataIndex: 'applicantId', key: 'applicantId', width: 100 },
  { title: '操作', key: 'action', width: 100, fixed: 'right', render: renderAction },
];
