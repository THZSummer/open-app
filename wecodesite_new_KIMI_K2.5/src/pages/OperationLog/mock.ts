export const mockLogs = [
  { id: '1', account: 'admin', operationType: '创建应用', object: '应用', description: '创建应用: 智能客服机器人', ip: '10.0.1.100', result: '成功', time: '2024-01-15 10:30:00' },
  { id: '2', account: 'zhangsan', operationType: '更新配置', object: '基础信息', description: '修改应用名称', ip: '10.0.1.101', result: '成功', time: '2024-01-15 11:15:00' },
  { id: '3', account: 'lisi', operationType: '添加成员', object: '成员', description: '添加成员: 王五', ip: '10.0.1.102', result: '成功', time: '2024-01-15 14:20:00' },
  { id: '4', account: 'admin', operationType: '删除成员', object: '成员', description: '删除成员: 赵六', ip: '10.0.1.100', result: '失败', time: '2024-01-15 16:45:00' },
  { id: '5', account: 'wangwu', operationType: '发布版本', object: '版本', description: '发布版本: 1.0.0', ip: '10.0.1.103', result: '成功', time: '2024-01-16 09:00:00' },
];

export const mockOperationTypes = ['全部', '创建应用', '更新配置', '添加成员', '删除成员', '发布版本'];
export const mockOperationObjects = ['全部', '应用', '基础信息', '成员', '版本', 'API', '事件'];
