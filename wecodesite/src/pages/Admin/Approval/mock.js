export const mockApprovals = [
  {
    id: '1',
    applicationNo: 'APP-001',
    applicant: '张三',
    applicantId: 'user001',
    type: 'API权限',
    content: '发送消息API',
    status: 0,
    createTime: '2026-04-01 10:00:00',
  },
  {
    id: '2',
    applicationNo: 'APP-002',
    applicant: '李四',
    applicantId: 'user002',
    type: '事件订阅',
    content: '用户状态变更',
    status: 0,
    createTime: '2026-04-02 10:00:00',
  },
  {
    id: '3',
    applicationNo: 'APP-003',
    applicant: '王五',
    applicantId: 'user003',
    type: 'API权限',
    content: '用户信息API',
    status: 1,
    createTime: '2026-04-03 10:00:00',
  },
];

export const mockMyApprovals = [
  {
    id: '4',
    applicationNo: 'APP-004',
    type: 'API权限',
    content: '我的申请-发送消息API',
    status: 1,
    createTime: '2026-04-01 10:00:00',
  },
];