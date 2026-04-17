import type { ApiPermission } from '../../types';

export const mockApis: ApiPermission[] = [
  { id: '1', name: '日历权限', codeName: 'calendar', authType: 'user', category: '日历', status: '已审核' },
  { id: '2', name: '云文档权限', codeName: 'docx', authType: 'user', category: '云文档', status: '审核中' },
  { id: '3', name: '云空间权限', codeName: 'drive', authType: 'app', category: '云空间', status: '已审核' },
  { id: '4', name: '消息权限', codeName: 'message', authType: 'user', category: '消息', status: '已中止' },
  { id: '5', name: '通讯录权限', codeName: 'contact', authType: 'app', category: '通讯录', status: '已审核' },
];

export const mockAvailableApis = {
  soa: [
    { id: '6', name: '审批权限', codeName: 'approval', authType: 'user', category: '审批', needAudit: false },
    { id: '7', name: '任务权限', codeName: 'task', authType: 'user', category: '任务', needAudit: true },
    { id: '8', name: '会议权限', codeName: 'meeting', authType: 'user', category: '会议', needAudit: false },
  ],
  apig: [
    { id: '9', name: 'AI 权限', codeName: 'ai', authType: 'app', category: 'AI', needAudit: true },
    { id: '10', name: '搜索权限', codeName: 'search', authType: 'app', category: '搜索', needAudit: false },
  ],
};

export const mockModules = ['all', '日历', '云文档', '云空间', '消息', '通讯录', '审批', '任务', '会议'];
