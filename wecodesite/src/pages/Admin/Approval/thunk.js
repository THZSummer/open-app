/**
 * Admin模块 - 审批管理相关API
 * 用于审批中心处理待审批事项
 */
import { useTrueFetch } from '../../../utils/constants';
import { API_CONFIG, buildApiUrl, fetchApi } from '../../../configs/web.config';
import { mockApprovals, mockMyApprovals, mockApprovalFlows } from './mock';

const delay = (ms) => new Promise((resolve) => setTimeout(resolve, ms));

/**
 * 获取待审批列表
 * @param {Object} params - 查询参数，包含 type（审批类型）、keyword（关键词）、curPage、pageSize
 * @returns {Promise<Object>} 包含 code、messageZh、data、page 的响应对象
 */
export const fetchApprovalList = async (params = {}) => {
  if (useTrueFetch) {
    try {
      // 如果指定了 status=0（待审），添加 approverId=current 参数
      const queryParams = { ...params };
      if (params.status === 0 && !params.approverId) {
        queryParams.approverId = 'current';
      }
      const result = await fetchApi(API_CONFIG.APPROVALS.PENDING, { method: 'GET', params: queryParams });
      return result || {};
    } catch (err) {
      return {};
    }
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
    try {
      const result = await fetchApi(buildApiUrl(API_CONFIG.APPROVALS.DETAIL, { id }));
      return result || {};
    } catch (err) {
      return {};
    }
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
    try {
      const result = await fetchApi(API_CONFIG.APPROVALS.PENDING, { method: 'GET', params: { ...params, applicantId: 'current' } });
      return result || {};
    } catch (err) {
      return {};
    }
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
    try {
      const result = await fetchApi(buildApiUrl(API_CONFIG.APPROVALS.APPROVE, { id }), {
        method: 'POST',
        body: JSON.stringify(data)
      });
      return result || {};
    } catch (err) {
      return {};
    }
  }
  await delay(300);
  return {
    code: '200',
    messageZh: '审批通过',
    data: {
      id,
      status: 1,
      // v2.8.0: 返回 combinedNodes（组合审批节点）
      combinedNodes: [
        { type: 'approver', userId: 'user001', userName: '张三', order: 1, level: 'scene', status: 1 },
        { type: 'approver', userId: 'user002', userName: '李四', order: 2, level: 'global', status: null }
      ]
    }
  };
};

/**
 * 驳回审批
 * @param {string} id - 审批记录ID
 * @param {Object} data - 驳回数据，包含 comment（审批意见）
 * @returns {Promise<Object>} 包含 code、messageZh、data 的响应对象
 */
export const rejectApplication = async (id, data = {}) => {
  if (useTrueFetch) {
    try {
      return await fetchApi(buildApiUrl(API_CONFIG.APPROVALS.REJECT, { id }), {
        method: 'POST',
        body: JSON.stringify(data)
      });
    } catch (err) {
      return {};
    }
  }
  await delay(300);
  return {
    code: '200',
    messageZh: '审批拒绝',
    data: {
      id,
      status: 2,
      // v2.8.0: 返回组合审批节点
      combinedNodes: [
        { type: 'approver', userId: 'user001', userName: '张三', order: 1, level: 'scene', status: 1 },
        { type: 'approver', userId: 'user002', userName: '李四', order: 2, level: 'global', status: 2 }
      ]
    }
  };
};

/**
 * 撤销审批
 * @param {string} id - 审批记录ID
 * @returns {Promise<Object>} 包含 code、messageZh、data 的响应对象
 */
export const cancelApproval = async (id) => {
  if (useTrueFetch) {
    try {
      const result = await fetchApi(buildApiUrl(API_CONFIG.APPROVALS.CANCEL, { id }), { method: 'POST' });
      return result || {};
    } catch (err) {
      return {};
    }
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
    try {
      const result = await fetchApi(API_CONFIG.APPROVALS.BATCH_APPROVE, {
        method: 'POST',
        body: JSON.stringify(data)
      });
      return result || {};
    } catch (err) {
      return {};
    }
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
 * @param {Object} data - 批量驳回数据，包含 approvalIds（审批ID列表）、comment（审批意见）
 * @returns {Promise<Object>} 包含 code、messageZh、data 的响应对象
 */
export const batchReject = async (data) => {
  if (useTrueFetch) {
    try {
      const result = await fetchApi(API_CONFIG.APPROVALS.BATCH_REJECT, {
        method: 'POST',
        body: JSON.stringify(data)
      });
      return result || {};
    } catch (err) {
      return {};
    }
  }
  await delay(300);
  return {
    code: '200',
    messageZh: '批量驳回成功',
    data: { successCount: data.approvalIds?.length || 0, failedCount: 0 }
  };
};
// ==================== 审批流程模板管理 ====================

/**
 * 获取审批流程模板列表
 * @param {Object} params - 查询参数，包含 keyword、curPage、pageSize
 * @returns {Promise<Object>} 包含 code、messageZh、data、page 的响应对象
 */
export const fetchApprovalFlowList = async (params = {}) => {
  if (useTrueFetch) {
    return fetchApi(API_CONFIG.APPROVAL_FLOWS.LIST, { method: 'GET', params });
  }
  await delay(300);
  let data = mockApprovalFlows;
  if (params.keyword) {
    data = data.filter(item =>
      item.nameCn.includes(params.keyword) ||
      item.code.includes(params.keyword)
    );
  }
  return {
    code: '200',
    messageZh: '查询成功',
    data: data,
    page: { curPage: params.curPage || 1, pageSize: params.pageSize || 20, total: data.length }
  };
};

/**
 * 获取审批流程模板详情
 * @param {string} id - 流程ID
 * @returns {Promise<Object>} 包含 code、messageZh、data 的响应对象
 */
export const fetchApprovalFlowDetail = async (id) => {
  if (useTrueFetch) {
    return fetchApi(buildApiUrl(API_CONFIG.APPROVAL_FLOWS.DETAIL, { id }));
  }
  await delay(300);
  const flow = mockApprovalFlows.find(item => item.id === id);
  return {
    code: '200',
    messageZh: '查询成功',
    data: flow
  };
};

/**
 * 创建审批流程模板
 * @param {Object} data - 创建数据，包含 nameCn、nameEn、code、nodes
 * @returns {Promise<Object>} 包含 code、messageZh、data 的响应对象
 */
export const createApprovalFlow = async (data) => {
  if (useTrueFetch) {
    return fetchApi(API_CONFIG.APPROVAL_FLOWS.CREATE, {
      method: 'POST',
      body: JSON.stringify(data)
    });
  }
  await delay(300);
  const newFlow = {
    id: String(mockApprovalFlows.length + 1),
    ...data,
    status: 1
  };
  return {
    code: '200',
    messageZh: '创建成功',
    data: newFlow
  };
};

/**
 * 更新审批流程模板
 * @param {string} id - 流程ID
 * @param {Object} data - 更新数据，包含 nameCn、nameEn、nodes
 * @returns {Promise<Object>} 包含 code、messageZh、data 的响应对象
 */
export const updateApprovalFlow = async (id, data) => {
  if (useTrueFetch) {
    return fetchApi(buildApiUrl(API_CONFIG.APPROVAL_FLOWS.UPDATE, { id }), {
      method: 'PUT',
      body: JSON.stringify(data)
    });
  }
  await delay(300);
  return {
    code: '200',
    messageZh: '更新成功',
    data: { id, ...data }
  };
};

/**
 * 删除审批流程模板
 * @param {string} id - 流程ID
 * @returns {Promise<Object>} 包含 code、messageZh 的响应对象
 */
export const deleteApprovalFlow = async (id) => {
  if (useTrueFetch) {
    return fetchApi(buildApiUrl(API_CONFIG.APPROVAL_FLOWS.DELETE, { id }), {
      method: 'DELETE'
    });
  }
  await delay(300);
  return {
    code: '200',
    messageZh: '删除成功',
  };
};