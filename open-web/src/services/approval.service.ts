import { get, post } from '@/utils/request';

/**
 * 审批管理相关接口
 */

// ==================== 类型定义 ====================

export interface ApprovalFlowNode {
  type: string; // approver=审批人
  userId: string;
  userName?: string;
  order: number;
}

export interface ApprovalFlow {
  id: string;
  nameCn: string;
  nameEn: string;
  code: string;
  isDefault: number; // 0=否, 1=是
  status: number;
  nodes?: ApprovalFlowNode[];
  createTime?: string;
  createBy?: string;
  lastUpdateTime?: string;
  lastUpdateBy?: string;
}

export interface ApprovalFlowListParams {
  keyword?: string;
  curPage?: number;
  pageSize?: number;
}

export interface CreateApprovalFlowParams {
  nameCn: string;
  nameEn: string;
  code: string;
  isDefault?: number;
  nodes: ApprovalFlowNode[];
}

export interface UpdateApprovalFlowParams {
  nameCn?: string;
  nameEn?: string;
  code?: string;
  isDefault?: number;
  nodes?: ApprovalFlowNode[];
}

export interface ApprovalLog {
  id: string;
  approvalId: string;
  operatorId: string;
  operatorName: string;
  action: string; // approve, reject, cancel
  comment?: string;
  createTime: string;
}

export interface Approval {
  id: string;
  approvalNo: string;
  type: string; // api, event, callback
  applicantId: string;
  applicantName: string;
  permissionId: string;
  permissionName: string;
  permissionScope: string;
  appId: string;
  appName: string;
  status: number; // 0=待审, 1=已通过, 2=已拒绝, 3=已撤销
  currentNode: number;
  approver?: {
    userId: string;
    userName: string;
  };
  logs?: ApprovalLog[];
  createTime: string;
  updateTime?: string;
}

export interface ApprovalListParams {
  status?: number;
  type?: string;
  keyword?: string;
  curPage?: number;
  pageSize?: number;
}

export interface ApproveParams {
  comment?: string;
}

export interface RejectParams {
  reason: string;
}

// ==================== 审批流程管理 ====================

/**
 * 获取审批流程模板列表
 */
export const getApprovalFlowList = (params?: ApprovalFlowListParams) => {
  return get<ApprovalFlow[]>('/approval-flows', params);
};

/**
 * 获取审批流程模板详情
 */
export const getApprovalFlowDetail = (id: string) => {
  return get<ApprovalFlow>(`/approval-flows/${id}`);
};

/**
 * 创建审批流程模板
 */
export const createApprovalFlow = (data: CreateApprovalFlowParams) => {
  return post<ApprovalFlow>('/approval-flows', data);
};

/**
 * 更新审批流程模板
 */
export const updateApprovalFlow = (id: string, data: UpdateApprovalFlowParams) => {
  return post<ApprovalFlow>(`/approval-flows/${id}`, data);
};

// ==================== 审批执行 ====================

/**
 * 获取待审批列表
 */
export const getPendingApprovals = (params?: ApprovalListParams) => {
  return get<Approval[]>('/approvals/pending', params);
};

/**
 * 获取审批详情
 */
export const getApprovalDetail = (id: string) => {
  return get<Approval>(`/approvals/${id}`);
};

/**
 * 同意审批
 */
export const approveApproval = (id: string, data: ApproveParams) => {
  return post<Approval>(`/approvals/${id}/approve`, data);
};

/**
 * 驳回审批
 */
export const rejectApproval = (id: string, data: RejectParams) => {
  return post<Approval>(`/approvals/${id}/reject`, data);
};

/**
 * 撤销审批
 */
export const cancelApproval = (id: string) => {
  return post<Approval>(`/approvals/${id}/cancel`);
};

/**
 * 批量同意审批
 */
export const batchApproveApprovals = (approvalIds: string[], comment?: string) => {
  return post<{ successCount: number; failedCount: number }>('/approvals/batch-approve', {
    approvalIds,
    comment,
  });
};

/**
 * 批量驳回审批
 */
export const batchRejectApprovals = (approvalIds: string[], reason: string) => {
  return post<{ successCount: number; failedCount: number }>('/approvals/batch-reject', {
    approvalIds,
    reason,
  });
};
