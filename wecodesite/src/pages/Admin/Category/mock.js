export const mockCategories = [
  {
    id: '1',
    categoryAlias: 'api',
    nameCn: 'API分类',
    nameEn: 'API Category',
    sortOrder: 0,
    children: [
      {
        id: '1-1',
        nameCn: '用户服务',
        nameEn: 'User Service',
        sortOrder: 0,
        children: [
          { id: '1-1-1', nameCn: '发送消息', nameEn: 'Send Message', sortOrder: 0 },
          { id: '1-1-2', nameCn: '用户信息', nameEn: 'User Info', sortOrder: 1 }
        ]
      },
      {
        id: '1-2',
        nameCn: '消息推送',
        nameEn: 'Message Push',
        sortOrder: 1
      },
      {
        id: '1-3',
        nameCn: '订单服务',
        nameEn: 'Order Service',
        sortOrder: 2,
        children: [
          { id: '1-3-1', nameCn: '订单查询', nameEn: 'Order Query', sortOrder: 0 },
          { id: '1-3-2', nameCn: '订单创建', nameEn: 'Order Create', sortOrder: 1 }
        ]
      }
    ]
  },
  {
    id: '2',
    categoryAlias: 'event',
    nameCn: '事件分类',
    nameEn: 'Event Category',
    sortOrder: 1,
    children: [
      { id: '2-1', nameCn: '用户状态变更', nameEn: 'User Status Change', sortOrder: 0 },
      { id: '2-2', nameCn: '订单事件', nameEn: 'Order Events', sortOrder: 1 }
    ]
  },
  {
    id: '3',
    categoryAlias: 'callback',
    nameCn: '回调分类',
    nameEn: 'Callback Category',
    sortOrder: 2,
    children: [
      { id: '3-1', nameCn: '交易服务', nameEn: 'Transaction Service', sortOrder: 0 },
      { id: '3-2', nameCn: '通知服务', nameEn: 'Notification Service', sortOrder: 1 }
    ]
  }
];

export const mockOwners = [
  { id: '1', userId: 'user001', userName: '张三' },
  { id: '2', userId: 'user002', userName: '李四' },
  { id: '3', userId: 'admin001', userName: '王五' },
  { id: '4', userId: 'admin002', userName: '赵六' }
];