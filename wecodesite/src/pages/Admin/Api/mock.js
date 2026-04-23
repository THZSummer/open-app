export const mockApis = [
  {
    id: '1',
    nameCn: '发送消息',
    nameEn: 'Send Message',
    path: '/im/send',
    method: 'POST',
    authType: 1,
    categoryId: '1-1-1',
    categoryName: '发送消息',
    status: 2,
    permission: {
      nameCn: '发送消息权限',
      nameEn: 'Send Message Permission',
      scope: 'api:im:send-message',
    },
    createTime: '2026-04-01 10:00:00',
  },
  {
    id: '2',
    nameCn: '获取用户信息',
    nameEn: 'Get User Info',
    path: '/user/info',
    method: 'GET',
    authType: 5,
    categoryId: '1-1-2',
    categoryName: '用户信息',
    status: 2,
    permission: {
      nameCn: '用户信息权限',
      nameEn: 'User Info Permission',
      scope: 'api:user:info',
    },
    createTime: '2026-04-02 10:00:00',
  },
  {
    id: '3',
    nameCn: '用户登录',
    nameEn: 'User Login',
    path: '/user/login',
    method: 'POST',
    authType: 0,
    categoryId: '1-1-2',
    categoryName: '用户信息',
    status: 0,
    permission: {
      nameCn: '登录权限',
      nameEn: 'Login Permission',
      scope: 'api:user:login',
    },
    createTime: '2026-04-03 10:00:00',
  },
];

export const mockCategories = [
  { id: '1-1-1', nameCn: '发送消息' },
  { id: '1-1-2', nameCn: '用户信息' },
];