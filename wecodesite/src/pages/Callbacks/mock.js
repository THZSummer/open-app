export const mockCallbacks = [
  {
    id: "401",
    appId: "10",
    permissionId: "301",
    permission: {
      nameCn: "用户基本信息",
      scope: "callback:user:login"
    },
    callback: {
      docUrl: "/docs/user-login-callback"
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
    approvalUrl: "https://approval.example.com/callback/1",
    createTime: "2026-04-20T10:00:00.000Z"
  },
  {
    id: "402",
    appId: "10",
    permissionId: "302",
    permission: {
      nameCn: "订单管理",
      scope: "callback:order:created"
    },
    callback: {
      docUrl: "/docs/order-created-callback"
    },
    category: {
      id: "1",
      nameCn: "业务应用",
      path: "/1/",
      categoryPath: ["业务应用"]
    },
    status: 0,
    channelType: 1,
    channelAddress: "https://example.com/order-callback",
    authType: 0,
    approver: {
      userId: "user002",
      userName: "李四"
    },
    approvalUrl: "https://approval.example.com/callback/2",
    createTime: "2026-04-20T10:00:00.000Z"
  },
  {
    id: "403",
    appId: "10",
    permissionId: "303",
    permission: {
      nameCn: "支付权限",
      scope: "callback:order:paid"
    },
    callback: {
      docUrl: "/docs/order-paid-callback"
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
    approvalUrl: "https://approval.example.com/callback/3",
    createTime: "2026-04-20T10:00:00.000Z"
  },
  {
    id: "404",
    appId: "10",
    permissionId: "304",
    permission: {
      nameCn: "用户信息",
      scope: "callback:user:info-changed"
    },
    callback: {
      docUrl: "/docs/user-info-changed-callback"
    },
    category: {
      id: "2",
      nameCn: "个人应用",
      path: "/2/",
      categoryPath: ["个人应用"]
    },
    status: 1,
    channelType: 0,
    channelAddress: "",
    authType: 0,
    approver: {
      userId: "user004",
      userName: "赵六"
    },
    approvalUrl: "https://approval.example.com/callback/4",
    createTime: "2026-04-20T10:00:00.000Z"
  },
  {
    id: "405",
    appId: "10",
    permissionId: "305",
    permission: {
      nameCn: "文件管理",
      scope: "callback:file:uploaded"
    },
    callback: {
      docUrl: "/docs/file-uploaded-callback"
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
      userId: "user005",
      userName: "孙七"
    },
    approvalUrl: "https://approval.example.com/callback/5",
    createTime: "2026-04-20T10:00:00.000Z"
  },
  {
    id: "406",
    appId: "10",
    permissionId: "306",
    permission: {
      nameCn: "日历权限",
      scope: "callback:meeting:started"
    },
    callback: {
      docUrl: "/docs/meeting-started-callback"
    },
    category: {
      id: "2",
      nameCn: "个人应用",
      path: "/2/",
      categoryPath: ["个人应用"]
    },
    status: 1,
    channelType: 0,
    channelAddress: "",
    authType: 0,
    approver: {
      userId: "user006",
      userName: "周八"
    },
    approvalUrl: "https://approval.example.com/callback/6",
    createTime: "2026-04-20T10:00:00.000Z"
  },
  {
    id: "407",
    appId: "10",
    permissionId: "307",
    permission: {
      nameCn: "通讯录权限",
      scope: "callback:contact:updated"
    },
    callback: {
      docUrl: "/docs/contact-updated-callback"
    },
    category: {
      id: "1",
      nameCn: "业务应用",
      path: "/1/",
      categoryPath: ["业务应用"]
    },
    status: 2,
    channelType: 1,
    channelAddress: "https://example.com/contact-callback",
    authType: 1,
    approver: {
      userId: "user007",
      userName: "吴九"
    },
    approvalUrl: "https://approval.example.com/callback/7",
    createTime: "2026-04-20T10:00:00.000Z"
  },
  {
    id: "408",
    appId: "10",
    permissionId: "308",
    permission: {
      nameCn: "审批权限",
      scope: "callback:approval:completed"
    },
    callback: {
      docUrl: "/docs/approval-completed-callback"
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
    approvalUrl: "https://approval.example.com/callback/8",
    createTime: "2026-04-20T10:00:00.000Z"
  }
];

export const mockAllCallbacks = [
  { id: 101, name: '用户登录回调', callback: 'user_login_callback', callbackType: '业务应用', permission: '用户基本信息', needReview: false, docUrl: '/docs/user-login-callback' },
  { id: 102, name: '订单创建回调', callback: 'order_created_callback', callbackType: '业务应用', permission: '订单管理', needReview: true, docUrl: '/docs/order-created-callback' },
  { id: 103, name: '订单支付回调', callback: 'order_paid_callback', callbackType: '业务应用', permission: '支付权限', needReview: true, docUrl: '/docs/order-paid-callback' },
  { id: 104, name: '用户信息变更', callback: 'user_info_changed_callback', callbackType: '个人应用', permission: '用户信息', needReview: false, docUrl: '/docs/user-info-changed-callback' },
  { id: 105, name: '文件上传完成', callback: 'file_uploaded_callback', callbackType: '业务应用', permission: '文件管理', needReview: true, docUrl: '/docs/file-uploaded-callback' },
  { id: 106, name: '会议开始回调', callback: 'meeting_started_callback', callbackType: '个人应用', permission: '日历权限', needReview: false, docUrl: '/docs/meeting-started-callback' },
  { id: 107, name: '联系人更新', callback: 'contact_updated_callback', callbackType: '业务应用', permission: '通讯录权限', needReview: true, docUrl: '/docs/contact-updated-callback' },
  { id: 108, name: '审批完成回调', callback: 'approval_completed_callback', callbackType: '业务应用', permission: '审批权限', needReview: false, docUrl: '/docs/approval-completed-callback' },
  { id: 109, name: '用户登出回调', callback: 'user_logout_callback', callbackType: '个人应用', permission: '用户基本信息', needReview: false, docUrl: '/docs/user-logout-callback' },
  { id: 110, name: '应用创建回调', callback: 'app_created_callback', callbackType: '业务应用', permission: '应用管理', needReview: true, docUrl: '/docs/app-created-callback' },
  { id: 111, name: '应用删除回调', callback: 'app_deleted_callback', callbackType: '业务应用', permission: '应用管理', needReview: true, docUrl: '/docs/app-deleted-callback' },
  { id: 112, name: '群聊创建回调', callback: 'chat_created_callback', callbackType: '业务应用', permission: '群聊管理', needReview: false, docUrl: '/docs/chat-created-callback' },
  { id: 113, name: '群聊解散回调', callback: 'chat_disbanded_callback', callbackType: '业务应用', permission: '群聊管理', needReview: true, docUrl: '/docs/chat-disbanded-callback' },
  { id: 114, name: '成员加入群聊', callback: 'member_joined_chat_callback', callbackType: '业务应用', permission: '群聊管理', needReview: false, docUrl: '/docs/member-joined-chat-callback' },
  { id: 115, name: '成员离开群聊', callback: 'member_left_chat_callback', callbackType: '业务应用', permission: '群聊管理', needReview: false, docUrl: '/docs/member-left-chat-callback' },
];