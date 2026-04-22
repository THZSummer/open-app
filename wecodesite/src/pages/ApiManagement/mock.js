export const mockApis = [
  {
    id: "300",
    appId: "10",
    permissionId: "200",
    permission: {
      nameCn: "获取用户信息",
      scope: "user.get"
    },
    api: {
      path: "/api/v1/users",
      method: "GET",
      docUrl: "/docs/user-get"
    },
    category: {
      id: "1",
      nameCn: "用户管理",
      path: "/1/",
      categoryPath: ["用户管理"]
    },
    approver: {
      userId: "user001",
      userName: "李四"
    },
    status: 1,
    authType: 0,
    approvalUrl: "https://approval.example.com/api/1",
    createTime: "2026-04-20T10:00:00.000Z"
  },
  {
    id: "301",
    appId: "10",
    permissionId: "201",
    permission: {
      nameCn: "发送消息",
      scope: "message.send"
    },
    api: {
      path: "/api/v1/messages",
      method: "POST",
      docUrl: "/docs/message-send"
    },
    category: {
      id: "2",
      nameCn: "消息推送",
      path: "/2/",
      categoryPath: ["消息推送"]
    },
    approver: {
      userId: "user002",
      userName: "王五"
    },
    status: 0,
    authType: 1,
    approvalUrl: "https://approval.example.com/api/2",
    createTime: "2026-04-20T10:00:00.000Z"
  },
  {
    id: "302",
    appId: "10",
    permissionId: "202",
    permission: {
      nameCn: "创建日历事件",
      scope: "calendar.event.create"
    },
    api: {
      path: "/api/v1/calendar/events",
      method: "POST",
      docUrl: "/docs/calendar-create"
    },
    category: {
      id: "3",
      nameCn: "日历管理",
      path: "/3/",
      categoryPath: ["日历管理"]
    },
    approver: {
      userId: "user003",
      userName: "赵六"
    },
    status: 2,
    authType: 1,
    approvalUrl: "https://approval.example.com/api/3",
    createTime: "2026-04-20T10:00:00.000Z"
  },
  {
    id: "303",
    appId: "10",
    permissionId: "203",
    permission: {
      nameCn: "更新通讯录",
      scope: "contact.update"
    },
    api: {
      path: "/api/v1/contacts",
      method: "PUT",
      docUrl: "/docs/contact-update"
    },
    category: {
      id: "4",
      nameCn: "通讯录",
      path: "/4/",
      categoryPath: ["通讯录"]
    },
    approver: {
      userId: "user004",
      userName: "孙七"
    },
    status: 0,
    authType: 0,
    approvalUrl: "https://approval.example.com/api/4",
    createTime: "2026-04-20T10:00:00.000Z"
  },
];

export const mockFeatureFlag = {
  enableIdentityPermission: false,
};

export const identityPermissionApis = {
  BUSINESS_IDENTITY_SOA: {
    modules: [
      { key: 'all', name: '所有' },
      { key: 'employee', name: '员工管理' },
      { key: 'department', name: '部门管理' },
      { key: 'organization', name: '组织架构' },
    ],
    apis: [
      {
        id: 401,
        name: '获取员工详情',
        scope: 'soa.employee.get',
        category: '员工管理',
        needReview: false,
        docUrl: '/docs/soa-employee-get',
      },
      {
        id: 402,
        name: '批量查询员工',
        scope: 'soa.employee.list',
        category: '员工管理',
        needReview: false,
        docUrl: '/docs/soa-employee-list',
      },
      {
        id: 403,
        name: '同步员工数据',
        scope: 'soa.employee.sync',
        category: '员工管理',
        needReview: true,
        docUrl: '/docs/soa-employee-sync',
      },
      {
        id: 404,
        name: '获取部门列表',
        scope: 'soa.department.list',
        category: '部门管理',
        needReview: false,
        docUrl: '/docs/soa-department-list',
      },
      {
        id: 405,
        name: '获取部门详情',
        scope: 'soa.department.get',
        category: '部门管理',
        needReview: false,
        docUrl: '/docs/soa-department-get',
      },
      {
        id: 406,
        name: '创建部门',
        scope: 'soa.department.create',
        category: '部门管理',
        needReview: true,
        docUrl: '/docs/soa-department-create',
      },
      {
        id: 407,
        name: '获取组织架构',
        scope: 'soa.organization.tree',
        category: '组织架构',
        needReview: false,
        docUrl: '/docs/soa-organization-tree',
      },
    ],
  },
  BUSINESS_IDENTITY_APIG: {
    modules: [
      { key: 'all', name: '所有' },
      { key: 'gateway', name: '网关配置' },
      { key: 'monitor', name: '监控告警' },
      { key: 'log', name: '日志分析' },
    ],
    apis: [
      {
        id: 501,
        name: '创建API网关',
        scope: 'apigateway.create',
        category: '网关配置',
        needReview: true,
        docUrl: '/docs/apigateway-create',
      },
      {
        id: 502,
        name: '查询网关列表',
        scope: 'apigateway.list',
        category: '网关配置',
        needReview: false,
        docUrl: '/docs/apigateway-list',
      },
      {
        id: 503,
        name: '配置网关路由',
        scope: 'apigateway.route',
        category: '网关配置',
        needReview: true,
        docUrl: '/docs/apigateway-route',
      },
      {
        id: 504,
        name: '获取监控数据',
        scope: 'monitor.metrics',
        category: '监控告警',
        needReview: false,
        docUrl: '/docs/monitor-metrics',
      },
      {
        id: 505,
        name: '设置告警规则',
        scope: 'monitor.alert',
        category: '监控告警',
        needReview: true,
        docUrl: '/docs/monitor-alert',
      },
      {
        id: 506,
        name: '查询调用日志',
        scope: 'log.query',
        category: '日志分析',
        needReview: false,
        docUrl: '/docs/log-query',
      },
    ],
  },
  PERSONAL_IDENTITY_SOA: {
    modules: [
      { key: 'all', name: '所有' },
      { key: 'basic', name: '基本信息' },
      { key: 'contact', name: '联系方式' },
    ],
    apis: [
      {
        id: 601,
        name: '获取个人基本信息',
        scope: 'soa.personal.basic',
        category: '基本信息',
        needReview: false,
        docUrl: '/docs/soa-personal-basic',
      },
      {
        id: 602,
        name: '更新个人基本信息',
        scope: 'soa.personal.update',
        category: '基本信息',
        needReview: false,
        docUrl: '/docs/soa-personal-update',
      },
      {
        id: 603,
        name: '获取联系方式',
        scope: 'soa.contact.get',
        category: '联系方式',
        needReview: false,
        docUrl: '/docs/soa-contact-get',
      },
      {
        id: 604,
        name: '更新联系方式',
        scope: 'soa.contact.update',
        category: '联系方式',
        needReview: false,
        docUrl: '/docs/soa-contact-update',
      },
    ],
  },
  PERSONAL_IDENTITY_APIG: {
    modules: [
      { key: 'all', name: '所有' },
      { key: 'message', name: '消息通知' },
      { key: 'calendar', name: '日程管理' },
    ],
    apis: [
      {
        id: 701,
        name: '发送消息',
        scope: 'apig.message.send',
        category: '消息通知',
        needReview: false,
        docUrl: '/docs/apig-message-send',
      },
      {
        id: 702,
        name: '查询消息列表',
        scope: 'apig.message.list',
        category: '消息通知',
        needReview: false,
        docUrl: '/docs/apig-message-list',
      },
      {
        id: 703,
        name: '创建日程',
        scope: 'apig.calendar.create',
        category: '日程管理',
        needReview: false,
        docUrl: '/docs/apig-calendar-create',
      },
      {
        id: 704,
        name: '查询日程列表',
        scope: 'apig.calendar.list',
        category: '日程管理',
        needReview: false,
        docUrl: '/docs/apig-calendar-list',
      },
      {
        id: 705,
        name: '更新日程',
        scope: 'apig.calendar.update',
        category: '日程管理',
        needReview: false,
        docUrl: '/docs/apig-calendar-update',
      },
    ],
  },
  PERSONAL_IDENTITY_AKSK: {
    modules: [
      { key: 'all', name: '所有' },
      { key: 'credential', name: '凭证管理' },
      { key: 'signature', name: '签名密钥' },
    ],
    apis: [
      {
        id: 801,
        name: '获取凭证信息',
        scope: 'akskey.credential.get',
        category: '凭证管理',
        needReview: false,
        docUrl: '/docs/akskey-credential-get',
      },
      {
        id: 802,
        name: '创建凭证',
        scope: 'akskey.credential.create',
        category: '凭证管理',
        needReview: true,
        docUrl: '/docs/akskey-credential-create',
      },
      {
        id: 803,
        name: '删除凭证',
        scope: 'akskey.credential.delete',
        category: '凭证管理',
        needReview: true,
        docUrl: '/docs/akskey-credential-delete',
      },
      {
        id: 804,
        name: '获取签名密钥',
        scope: 'akskey.signature.get',
        category: '签名密钥',
        needReview: false,
        docUrl: '/docs/akskey-signature-get',
      },
      {
        id: 805,
        name: '更新签名密钥',
        scope: 'akskey.signature.update',
        category: '签名密钥',
        needReview: true,
        docUrl: '/docs/akskey-signature-update',
      },
    ],
  },
};