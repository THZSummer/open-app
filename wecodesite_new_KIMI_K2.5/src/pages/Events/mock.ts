import type { Event } from '../../types';

export const mockEvents: Event[] = [
  { id: '1', name: '应用创建事件', type: 'im', permission: 'app' },
  { id: '2', name: '消息接收事件', type: 'im', permission: 'message' },
  { id: '3', name: '用户加入群聊', type: 'im', permission: 'group' },
  { id: '4', name: '审批状态变更', type: 'approval', permission: 'approval' },
  { id: '5', name: '日程变更事件', type: 'calendar', permission: 'calendar' },
];

export const mockAllEvents: Event[] = [
  ...mockEvents,
  { id: '6', name: '文件上传事件', type: 'drive', permission: 'drive' },
  { id: '7', name: '任务完成事件', type: 'task', permission: 'task' },
  { id: '8', name: '会议开始事件', type: 'meeting', permission: 'meeting' },
  { id: '9', name: '文档分享事件', type: 'docx', permission: 'docx' },
  { id: '10', name: '用户登录事件', type: 'user', permission: 'user' },
];
