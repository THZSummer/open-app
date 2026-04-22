export const mockEvents = [
  {
    id: "301",
    appId: "10",
    permissionId: "201",
    permission: {
      nameCn: "读取应用信息",
      scope: "event:app:open"
    },
    event: {
      topic: "app_open",
      docUrl: "/docs/app-open"
    },
    category: {
      id: "1",
      nameCn: "业务应用",
      path: "/1/",
      categoryPath: ["业务应用"]
    },
    status: 1,
    channelType: 0,
    channelAddress: "",
    authType: 0,
    approver: {
      userId: "user001",
      userName: "张三"
    },
    approvalUrl: "https://approval.example.com/event/1",
    createTime: "2026-04-20T10:00:00.000Z"
  },
  {
    id: "302",
    appId: "10",
    permissionId: "202",
    permission: {
      nameCn: "读取消息记录",
      scope: "event:message:revoke"
    },
    event: {
      topic: "message_revoke",
      docUrl: "/docs/message-revoke"
    },
    category: {
      id: "2",
      nameCn: "个人应用",
      path: "/2/",
      categoryPath: ["个人应用"]
    },
    status: 0,
    channelType: 0,
    channelAddress: "",
    authType: 0,
    approver: {
      userId: "user002",
      userName: "李四"
    },
    approvalUrl: "https://approval.example.com/event/2",
    createTime: "2026-04-20T10:00:00.000Z"
  },
  {
    id: "303",
    appId: "10",
    permissionId: "203",
    permission: {
      nameCn: "用户基本信息",
      scope: "event:user:authorized"
    },
    event: {
      topic: "user_authorized",
      docUrl: "/docs/user-authorized"
    },
    category: {
      id: "1",
      nameCn: "业务应用",
      path: "/1/",
      categoryPath: ["业务应用"]
    },
    status: 2,
    channelType: 0,
    channelAddress: "",
    authType: 0,
    approver: {
      userId: "user003",
      userName: "王五"
    },
    approvalUrl: "https://approval.example.com/event/3",
    createTime: "2026-04-20T10:00:00.000Z"
  },
  {
    id: "304",
    appId: "10",
    permissionId: "204",
    permission: {
      nameCn: "审批权限",
      scope: "event:approval:completed"
    },
    event: {
      topic: "approval_completed",
      docUrl: "/docs/approval-completed"
    },
    category: {
      id: "1",
      nameCn: "业务应用",
      path: "/1/",
      categoryPath: ["业务应用"]
    },
    status: 1,
    channelType: 0,
    channelAddress: "",
    authType: 0,
    approver: {
      userId: "user004",
      userName: "赵六"
    },
    approvalUrl: "https://approval.example.com/event/4",
    createTime: "2026-04-20T10:00:00.000Z"
  },
  {
    id: "305",
    appId: "10",
    permissionId: "205",
    permission: {
      nameCn: "读取应用信息",
      scope: "event:user:logout"
    },
    event: {
      topic: "user_logout",
      docUrl: "/docs/user-logout"
    },
    category: {
      id: "2",
      nameCn: "个人应用",
      path: "/2/",
      categoryPath: ["个人应用"]
    },
    status: 0,
    channelType: 0,
    channelAddress: "",
    authType: 0,
    approver: {
      userId: "user005",
      userName: "孙七"
    },
    approvalUrl: "https://approval.example.com/event/5",
    createTime: "2026-04-20T10:00:00.000Z"
  },
  {
    id: "306",
    appId: "10",
    permissionId: "206",
    permission: {
      nameCn: "文件管理",
      scope: "event:file:uploaded"
    },
    event: {
      topic: "file_uploaded",
      docUrl: "/docs/file-uploaded"
    },
    category: {
      id: "1",
      nameCn: "业务应用",
      path: "/1/",
      categoryPath: ["业务应用"]
    },
    status: 1,
    channelType: 0,
    channelAddress: "",
    authType: 0,
    approver: {
      userId: "user006",
      userName: "周八"
    },
    approvalUrl: "https://approval.example.com/event/6",
    createTime: "2026-04-20T10:00:00.000Z"
  },
  {
    id: "307",
    appId: "10",
    permissionId: "207",
    permission: {
      nameCn: "日历权限",
      scope: "event:meeting:started"
    },
    event: {
      topic: "meeting_started",
      docUrl: "/docs/meeting-started"
    },
    category: {
      id: "2",
      nameCn: "个人应用",
      path: "/2/",
      categoryPath: ["个人应用"]
    },
    status: 2,
    channelType: 0,
    channelAddress: "",
    authType: 0,
    approver: {
      userId: "user007",
      userName: "吴九"
    },
    approvalUrl: "https://approval.example.com/event/7",
    createTime: "2026-04-20T10:00:00.000Z"
  },
  {
    id: "308",
    appId: "10",
    permissionId: "208",
    permission: {
      nameCn: "通讯录权限",
      scope: "event:contact:updated"
    },
    event: {
      topic: "contact_updated",
      docUrl: "/docs/contact-updated"
    },
    category: {
      id: "1",
      nameCn: "业务应用",
      path: "/1/",
      categoryPath: ["业务应用"]
    },
    status: 0,
    channelType: 0,
    channelAddress: "",
    authType: 0,
    approver: {
      userId: "user008",
      userName: "郑十"
    },
    approvalUrl: "https://approval.example.com/event/8",
    createTime: "2026-04-20T10:00:00.000Z"
  }
];

export const mockSubscriptionConfig = {
  channelType: 0,
  channelAddress: "",
};

export const mockAllEvents = [
  { id: 101, name: '用户进入应用', event: 'app_open', eventType: '业务应用', permission: '读取应用信息', needReview: false, docUrl: '/docs/app-open' },
  { id: 102, name: '消息被撤回', event: 'message_revoke', eventType: '个人应用', permission: '读取消息记录', needReview: true, docUrl: '/docs/message-revoke' },
  { id: 103, name: '用户授权', event: 'user_authorized', eventType: '业务应用', permission: '用户基本信息', needReview: true, docUrl: '/docs/user-authorized' },
  { id: 104, name: '审批完成', event: 'approval_completed', eventType: '业务应用', permission: '审批权限', needReview: false, docUrl: '/docs/approval-completed' },
  { id: 105, name: '用户登出', event: 'user_logout', eventType: '个人应用', permission: '读取应用信息', needReview: true, docUrl: '/docs/user-logout' },
  { id: 106, name: '文件上传', event: 'file_uploaded', eventType: '业务应用', permission: '文件管理', needReview: false, docUrl: '/docs/file-uploaded' },
  { id: 107, name: '会议开始', event: 'meeting_started', eventType: '个人应用', permission: '日历权限', needReview: true, docUrl: '/docs/meeting-started' },
  { id: 108, name: '联系人更新', event: 'contact_updated', eventType: '业务应用', permission: '通讯录权限', needReview: true, docUrl: '/docs/contact-updated' },
  { id: 109, name: '用户信息变更', event: 'user_info_changed', eventType: '个人应用', permission: '用户信息', needReview: false, docUrl: '/docs/user-info-changed' },
  { id: 110, name: '应用创建', event: 'app_created', eventType: '业务应用', permission: '应用管理', needReview: true, docUrl: '/docs/app-created' },
  { id: 111, name: '应用删除', event: 'app_deleted', eventType: '业务应用', permission: '应用管理', needReview: true, docUrl: '/docs/app-deleted' },
  { id: 112, name: '群聊创建', event: 'chat_created', eventType: '业务应用', permission: '群聊管理', needReview: false, docUrl: '/docs/chat-created' },
  { id: 113, name: '群聊解散', event: 'chat_disbanded', eventType: '业务应用', permission: '群聊管理', needReview: true, docUrl: '/docs/chat-disbanded' },
  { id: 114, name: '成员加入群聊', event: 'member_joined_chat', eventType: '业务应用', permission: '群聊管理', needReview: false, docUrl: '/docs/member-joined-chat' },
  { id: 115, name: '成员离开群聊', event: 'member_left_chat', eventType: '业务应用', permission: '群聊管理', needReview: false, docUrl: '/docs/member-left-chat' },
];