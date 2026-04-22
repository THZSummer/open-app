import { mockApprovals, mockMyApprovals } from './mock';

const delay = (ms) => new Promise((resolve) => setTimeout(resolve, ms));

export const fetchApprovalList = async (params = {}) => {
  await delay(300);
  let data = mockApprovals;
  if (params.status !== undefined) {
    data = data.filter(item => item.status === params.status);
  }
  return {
    code: '200',
    messageZh: '查询成功',
    data: data,
    page: { curPage: 1, pageSize: 20, total: data.length }
  };
};

export const fetchMyApprovals = async () => {
  await delay(300);
  return {
    code: '200',
    messageZh: '查询成功',
    data: mockMyApprovals,
  };
};

export const approveApplication = async (id) => {
  await delay(300);
  return {
    code: '200',
    messageZh: '审批通过',
    data: { id, status: 1 }
  };
};

export const rejectApplication = async (id) => {
  await delay(300);
  return {
    code: '200',
    messageZh: '审批拒绝',
    data: { id, status: 2 }
  };
};