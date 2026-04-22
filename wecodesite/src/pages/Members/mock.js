export const mockMembers = [
  { id: 1, name: '张三', employeeId: 'E001', role: '管理员', status: '正常' },
  { id: 2, name: '李四', employeeId: 'E002', role: '开发者', status: '正常' },
  { id: 3, name: '王五', employeeId: 'E003', role: '开发者', status: '正常' },
];

export const mockUsers = [
  { id: 1, name: '张三', employeeId: 'E001', email: 'zhangsan@company.com' },
  { id: 2, name: '李四', employeeId: 'E002', email: 'lisi@company.com' },
  { id: 3, name: '王五', employeeId: 'E003', email: 'wangwu@company.com' },
  { id: 4, name: '赵六', employeeId: 'E004', email: 'zhaoliu@company.com' },
  { id: 5, name: '钱七', employeeId: 'E005', email: 'qianqi@company.com' },
];

export const rolePermissions = {
  '管理员': [
    '应用基础信息管理',
    '成员管理',
    'API管理',
    '事件配置',
    '版本发布',
  ],
  '开发者': [
    '查看应用信息',
    'API调用',
    '事件订阅',
  ],
};