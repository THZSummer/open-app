/**
 * Admin模块 - 审批管理相关API
 * 用于审批中心处理待审批事项
 */
import { API_CONFIG, buildApiUrl, fetchApi } from '../../../configs/web.config';

/**
 * 获取待审批列表
 * @param {Object} params - 查询参数，包含 type（审批类型）、keyword（关键词）、curPage、pageSize
 * @returns {Promise<Object>} 包含 code、messageZh、data、page 的响应对象
 */
export const fetchApprovalList = async (params = {}) => {
  try {
    const queryParams = { ...params };
    if (params.status === 0 && !params.approverId) {
      queryParams.approverId = 'current';
    }
    const result = await fetchApi(API_CONFIG.APPROVALS.PENDING, { method: 'GET', params: queryParams });
    return result || {};
  } catch (err) {
    return {};
  }
};

/**
 * 获取审批详情
 * @param {string} id - 审批记录ID
 * @returns {Promise<Object>} 包含 code、messageZh、data 的响应对象
 */
export const fetchApprovalDetail = async (id) => {
  try {
    const result = await fetchApi(buildApiUrl(API_CONFIG.APPROVALS.DETAIL, { id }));
    return result || {};
  } catch (err) {
    return {};
  }
};

/**
 * 获取我发起的审批列表
 * @param {Object} params - 查询参数
 * @returns {Promise<Object>} 包含 code、messageZh、data、page 的响应对象
 */
export const fetchMyApprovals = async (params = {}) => {
  try {
    const result = await fetchApi(API_CONFIG.APPROVALS.PENDING, { method: 'GET', params: { ...params, applicantId: 'current' } });
    return result || {};
  } catch (err) {
    return {};
  }
};

/**
 * 同意审批
 * @param {string} id - 审批记录ID
 * @param {Object} data - 审批数据，包含 comment（审批意见）
 * @returns {Promise<Object>} 包含 code、messageZh、data 的响应对象
 */
export const approveApplication = async (id, data = {}) => {
  try {
    const result = await fetchApi(buildApiUrl(API_CONFIG.APPROVALS.APPROVE, { id }), {
      method: 'POST',
      body: JSON.stringify(data)
    });
    return result || {};
  } catch (err) {
    return {};
  }
};

/**
 * 驳回审批
 * @param {string} id - 审批记录ID
 * @param {Object} data - 驳回数据，包含 comment（审批意见）
 * @returns {Promise<Object>} 包含 code、messageZh、data 的响应对象
 */
export const rejectApplication = async (id, data = {}) => {
  try {
    const result = await fetchApi(buildApiUrl(API_CONFIG.APPROVALS.REJECT, { id }), {
      method: 'POST',
      body: JSON.stringify(data)
    });
    return result || {};
  } catch (err) {
    return {};
  }
};

/**
 * 撤销审批
 * @param {string} id - 审批记录ID
 * @returns {Promise<Object>} 包含 code、messageZh、data 的响应对象
 */
export const cancelApproval = async (id) => {
  try {
    const result = await fetchApi(buildApiUrl(API_CONFIG.APPROVALS.CANCEL, { id }), { method: 'POST' });
    return result || {};
  } catch (err) {
    return {};
  }
};

/**
 * 批量同意审批
 * @param {Object} data - 批量审批数据，包含 approvalIds（审批ID列表）、comment（审批意见）
 * @returns {Promise<Object>} 包含 code、messageZh、data 的响应对象
 */
export const batchApprove = async (data) => {
  try {
    const result = await fetchApi(API_CONFIG.APPROVALS.BATCH_APPROVE, {
      method: 'POST',
      body: JSON.stringify(data)
    });
    return result || {};
  } catch (err) {
    return {};
  }
};

/**
 * 批量驳回审批
 * @param {Object} data - 批量驳回数据，包含 approvalIds（审批ID列表）、comment（审批意见）
 * @returns {Promise<Object>} 包含 code、messageZh、data 的响应对象
 */
export const batchReject = async (data) => {
  try {
    const result = await fetchApi(API_CONFIG.APPROVALS.BATCH_REJECT, {
      method: 'POST',
      body: JSON.stringify(data)
    });
    return result || {};
  } catch (err) {
    return {};
  }
};

/**
 * 获取审批流程模板列表
 * @param {Object} params - 查询参数，包含 keyword、curPage、pageSize
 * @returns {Promise<Object>} 包含 code、messageZh、data、page 的响应对象
 */
export const fetchApprovalFlowList = async (params = {}) => {
  try {
    const result = await fetchApi(API_CONFIG.APPROVAL_FLOWS.LIST, { method: 'GET', params });
    return result || {};
  } catch (err) {
    return {};
  }
};

/**
 * 获取审批流程模板详情
 * @param {string} id - 流程ID
 * @returns {Promise<Object>} 包含 code、messageZh、data 的响应对象
 */
export const fetchApprovalFlowDetail = async (id) => {
  try {
    const result = await fetchApi(buildApiUrl(API_CONFIG.APPROVAL_FLOWS.DETAIL, { id }));
    return result || {};
  } catch (err) {
    return {};
  }
};

/**
 * 创建审批流程模板
 * @param {Object} data - 创建数据，包含 nameCn、nameEn、code、nodes
 * @returns {Promise<Object>} 包含 code、messageZh、data 的响应对象
 */
export const createApprovalFlow = async (data) => {
  try {
    const result = await fetchApi(API_CONFIG.APPROVAL_FLOWS.CREATE, {
      method: 'POST',
      body: JSON.stringify(data)
    });
    return result || {};
  } catch (err) {
    return {};
  }
};

/**
 * 更新审批流程模板
 * @param {string} id - 流程ID
 * @param {Object} data - 更新数据，包含 nameCn、nameEn、nodes
 * @returns {Promise<Object>} 包含 code、messageZh、data 的响应对象
 */
export const updateApprovalFlow = async (id, data) => {
  try {
    const result = await fetchApi(buildApiUrl(API_CONFIG.APPROVAL_FLOWS.UPDATE, { id }), {
      method: 'PUT',
      body: JSON.stringify(data)
    });
    return result || {};
  } catch (err) {
    return {};
  }
};

/**
 * 删除审批流程模板
 * @param {string} id - 流程ID
 * @returns {Promise<Object>} 包含 code、messageZh 的响应对象
 */
export const deleteApprovalFlow = async (id) => {
  try {
    const result = await fetchApi(buildApiUrl(API_CONFIG.APPROVAL_FLOWS.DELETE, { id }), {
      method: 'DELETE'
    });
    return result || {};
  } catch (err) {
    return {};
  }
};
