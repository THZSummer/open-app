import type { Member, User } from '../../types';

export const mockMembers: Member[] = [
  { id: 1, name: '张三', employeeId: 'EMP001', role: '管理员', status: 'active' },
  { id: 2, name: '李四', employeeId: 'EMP002', role: '开发者', status: 'active' },
  { id: 3, name: '王五', employeeId: 'EMP003', role: '开发者', status: 'active' },
];

export const mockUsers: User[] = [
  { id: 1, name: '张三', employeeId: 'EMP001', email: 'zhangsan@company.com' },
  { id: 2, name: '李四', employeeId: 'EMP002', email: 'lisi@company.com' },
  { id: 3, name: '王五', employeeId: 'EMP003', email: 'wangwu@company.com' },
  { id: 4, name: '赵六', employeeId: 'EMP004', email: 'zhaoliu@company.com' },
  { id: 5, name: '孙七', employeeId: 'EMP005', email: 'sunqi@company.com' },
];

export const rolePermissions: Record<string, string[]> = {
  '管理员': ['查看应用', '编辑应用', '管理成员', '发布版本', '配置API'],
  '开发者': ['查看应用', '编辑应用', '配置API'],
};
