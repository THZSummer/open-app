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
      { id: '2-1', nameCn: '用户状态变更', nameEn: 'User Status Change', sortOrder: 0 }
    ]
  },
  {
    id: '3',
    categoryAlias: 'callback',
    nameCn: '回调分类',
    nameEn: 'Callback Category',
    sortOrder: 2
  }
];

export const mockOwners = [
  { id: '1', userId: 'user001', userName: '张三' },
  { id: '2', userId: 'user002', userName: '李四' }
];