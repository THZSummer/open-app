export const mockEvents = [
  { id: 1, name: '用户进入应用', event: 'app_open', status: 'enabled', eventType: '业务应用', permission: '读取应用信息' },
  { id: 2, name: '消息被撤回', event: 'message_revoke', status: 'enabled', eventType: '个人应用', permission: '读取消息记录' },
  { id: 3, name: '用户授权', event: 'user_authorized', status: 'disabled', eventType: '业务应用', permission: '用户基本信息' },
  { id: 4, name: '审批完成', event: 'approval_completed', status: 'disabled', eventType: '业务应用', permission: '审批权限' },
  { id: 5, name: '用户登出', event: 'user_logout', status: 'disabled', eventType: '个人应用', permission: '读取应用信息' },
  { id: 6, name: '文件上传', event: 'file_uploaded', status: 'enabled', eventType: '业务应用', permission: '文件管理' },
  { id: 7, name: '会议开始', event: 'meeting_started', status: 'disabled', eventType: '个人应用', permission: '日历权限' },
  { id: 8, name: '联系人更新', event: 'contact_updated', status: 'enabled', eventType: '业务应用', permission: '通讯录权限' },
];

export const mockSubscriptionConfig = {
  method: 'mqs',
  callbackUrl: '',
};

export const mockAllEvents = [
  { id: 101, name: '用户进入应用', event: 'app_open', eventType: '业务应用', permission: '读取应用信息' },
  { id: 102, name: '消息被撤回', event: 'message_revoke', eventType: '个人应用', permission: '读取消息记录' },
  { id: 103, name: '用户授权', event: 'user_authorized', eventType: '业务应用', permission: '用户基本信息' },
  { id: 104, name: '审批完成', event: 'approval_completed', eventType: '业务应用', permission: '审批权限' },
  { id: 105, name: '用户登出', event: 'user_logout', eventType: '个人应用', permission: '读取应用信息' },
  { id: 106, name: '文件上传', event: 'file_uploaded', eventType: '业务应用', permission: '文件管理' },
  { id: 107, name: '会议开始', event: 'meeting_started', eventType: '个人应用', permission: '日历权限' },
  { id: 108, name: '联系人更新', event: 'contact_updated', eventType: '业务应用', permission: '通讯录权限' },
  { id: 109, name: '用户信息变更', event: 'user_info_changed', eventType: '个人应用', permission: '用户信息' },
  { id: 110, name: '应用创建', event: 'app_created', eventType: '业务应用', permission: '应用管理' },
  { id: 111, name: '应用删除', event: 'app_deleted', eventType: '业务应用', permission: '应用管理' },
  { id: 112, name: '群聊创建', event: 'chat_created', eventType: '业务应用', permission: '群聊管理' },
  { id: 113, name: '群聊解散', event: 'chat_disbanded', eventType: '业务应用', permission: '群聊管理' },
  { id: 114, name: '成员加入群聊', event: 'member_joined_chat', eventType: '业务应用', permission: '群聊管理' },
  { id: 115, name: '成员离开群聊', event: 'member_left_chat', eventType: '业务应用', permission: '群聊管理' },
];
