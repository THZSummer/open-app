/**
 * Admin模块 - 审批管理相关API
 * 用于审批中心处理待审批事项
 */
import { useTrueFetch } from '@/utils/constants';
import { API_CONFIG, buildApiUrl, fetchApi } from '@/configs/web.config';
import { mockApprovals, mockMyApprovals } from './mock';

const delay = (ms) => new Promise((resolve) => setTimeout(resolve, ms));

/**
 * 获取待审批列表
 * @param {Object} params - 查询参数，包含 type（审批类型）、keyword（关键词）、curPage、pageSize
 * @returns {Promise<Object>} 包含 code、messageZh、data、page 的响应对象
 */
export const fetchApprovalList = async (params = {}) => {
  if (useTrueFetch) {
    return fetchApi(API_CONFIG.APPROVALS.PENDING, { method: 'GET', params });
  }
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

/**
 * 获取审批详情
 * @param {string} id - 审批记录ID
 * @returns {Promise<Object>} 包含 code、messageZh、data 的响应对象
 */
export const fetchApprovalDetail = async (id) => {
  if (useTrueFetch) {
    return fetchApi(buildApiUrl(API_CONFIG.APPROVALS.DETAIL, { id }));
  }
  await delay(300);
  const approval = mockApprovals.find(item => item.id === id);
  return {
    code: '200',
    messageZh: '查询成功',
    data: approval
  };
};

/**
 * 获取我发起的审批列表
 * @param {Object} params - 查询参数
 * @returns {Promise<Object>} 包含 code、messageZh、data、page 的响应对象
 */
export const fetchMyApprovals = async (params = {}) => {
  if (useTrueFetch) {
    // 使用待审批列表接口，通过参数筛选
    return fetchApi(API_CONFIG.APPROVALS.PENDING, { method: 'GET', params: { ...params, applicantId: 'current' } });
  }
  await delay(300);
  return {
    code: '200',
    messageZh: '查询成功',
    data: mockMyApprovals,
  };
};

/**
 * 同意审批
 * @param {string} id - 审批记录ID
 * @param {Object} data - 审批数据，包含 comment（审批意见）
 * @returns {Promise<Object>} 包含 code、messageZh、data 的响应对象
 */
export const approveApplication = async (id, data = {}) => {
  if (useTrueFetch) {
    return fetchApi(buildApiUrl(API_CONFIG.APPROVALS.APPROVE, { id }), { 
      method: 'POST', 
      body: JSON.stringify(data) 
    });
  }
  await delay(300);
  return {
    code: '200',
    messageZh: '审批通过',
    data: { id, status: 1 }
  };
};

/**
 * 驳回审批
 * @param {string} id - 审批记录ID
 * @param {Object} data - 驳回数据，包含 reason（驳回原因）
 * @returns {Promise<Object>} 包含 code、messageZh、data 的响应对象
 */
export const rejectApplication = async (id, data = {}) => {
  if (useTrueFetch) {
    return fetchApi(buildApiUrl(API_CONFIG.APPROVALS.REJECT, { id }), { 
      method: 'POST', 
      body: JSON.stringify(data) 
    });
  }
  await delay(300);
  return {
    code: '200',
    messageZh: '审批拒绝',
    data: { id, status: 2 }
  };
};

/**
 * 撤销审批
 * @param {string} id - 审批记录ID
 * @returns {Promise<Object>} 包含 code、messageZh、data 的响应对象
 */
export const cancelApproval = async (id) => {
  if (useTrueFetch) {
    return fetchApi(buildApiUrl(API_CONFIG.APPROVALS.CANCEL, { id }), { method: 'POST' });
  }
  await delay(300);
  return {
    code: '200',
    messageZh: '审批已撤销',
    data: { id, status: 3 }
  };
};

/**
 * 批量同意审批
 * @param {Object} data - 批量审批数据，包含 approvalIds（审批ID列表）、comment（审批意见）
 * @returns {Promise<Object>} 包含 code、messageZh、data 的响应对象
 */
export const batchApprove = async (data) => {
  if (useTrueFetch) {
    return fetchApi(API_CONFIG.APPROVALS.BATCH_APPROVE, { 
      method: 'POST', 
      body: JSON.stringify(data) 
    });
  }
  await delay(300);
  return {
    code: '200',
    messageZh: '批量审批成功',
    data: { successCount: data.approvalIds?.length || 0, failedCount: 0 }
  };
};

/**
 * 批量驳回审批
 * @param {Object} data - 批量驳回数据，包含 approvalIds（审批ID列表）、reason（驳回原因）
 * @returns {Promise<Object>} 包含 code、messageZh、data 的响应对象
 */
export const batchReject = async (data) => {
  if (useTrueFetch) {
    return fetchApi(API_CONFIG.APPROVALS.BATCH_REJECT, { 
      method: 'POST', 
      body: JSON.stringify(data) 
    });
  }
  await delay(300);
  return {
    code: '200',
    messageZh: '批量驳回成功',
    data: { successCount: data.approvalIds?.length || 0, failedCount: 0 }
  };
};
