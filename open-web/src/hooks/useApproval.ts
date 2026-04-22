import { useState, useCallback } from 'react';
import { message } from 'antd';
import {
  getPendingApprovals,
  getApprovalDetail,
  approveApproval,
  rejectApproval,
  cancelApproval,
  batchApproveApprovals,
  batchRejectApprovals,
  getApprovalFlowList,
  getApprovalFlowDetail,
  createApprovalFlow,
  updateApprovalFlow,
  Approval,
  ApprovalListParams,
  ApprovalFlow,
  ApprovalFlowListParams,
  CreateApprovalFlowParams,
  UpdateApprovalFlowParams,
} from '@/services/approval.service';

/**
 * 审批管理 Hook
 */
export const useApproval = () => {
  const [loading, setLoading] = useState(false);
  const [approvalList, setApprovalList] = useState<Approval[]>([]);
  const [currentApproval, setCurrentApproval] = useState<Approval | null>(null);
  const [total, setTotal] = useState(0);

  /**
   * 获取待审批列表
   */
  const fetchPendingApprovals = useCallback(async (params?: ApprovalListParams) => {
    setLoading(true);
    try {
      const response = await getPendingApprovals(params);
      setApprovalList(response.data.data || []);
      setTotal(response.data.page?.total || 0);
      return {
        list: response.data.data || [],
        total: response.data.page?.total || 0,
      };
    } catch (error) {
      console.error('获取待审批列表失败:', error);
      return { list: [], total: 0 };
    } finally {
      setLoading(false);
    }
  }, []);

  /**
   * 获取审批详情
   */
  const fetchApprovalDetail = useCallback(async (id: string) => {
    setLoading(true);
    try {
      const response = await getApprovalDetail(id);
      setCurrentApproval(response.data.data);
      return response.data.data;
    } catch (error) {
      console.error('获取审批详情失败:', error);
      return null;
    } finally {
      setLoading(false);
    }
  }, []);

  /**
   * 同意审批
   */
  const handleApprove = useCallback(async (id: string, comment?: string) => {
    setLoading(true);
    try {
      const response = await approveApproval(id, { comment });
      message.success('审批已同意');
      return response.data.data;
    } catch (error) {
      console.error('同意审批失败:', error);
      return null;
    } finally {
      setLoading(false);
    }
  }, []);

  /**
   * 驳回审批
   */
  const handleReject = useCallback(async (id: string, reason: string) => {
    setLoading(true);
    try {
      const response = await rejectApproval(id, { reason });
      message.success('审批已驳回');
      return response.data.data;
    } catch (error) {
      console.error('驳回审批失败:', error);
      return null;
    } finally {
      setLoading(false);
    }
  }, []);

  /**
   * 撤销审批
   */
  const handleCancel = useCallback(async (id: string) => {
    setLoading(true);
    try {
      const response = await cancelApproval(id);
      message.success('审批已撤销');
      return response.data.data;
    } catch (error) {
      console.error('撤销审批失败:', error);
      return null;
    } finally {
      setLoading(false);
    }
  }, []);

  /**
   * 批量同意审批
   */
  const handleBatchApprove = useCallback(async (approvalIds: string[], comment?: string) => {
    setLoading(true);
    try {
      const response = await batchApproveApprovals(approvalIds, comment);
      message.success(`批量审批完成，成功 ${response.data.data.successCount} 条`);
      return response.data.data;
    } catch (error) {
      console.error('批量同意审批失败:', error);
      return null;
    } finally {
      setLoading(false);
    }
  }, []);

  /**
   * 批量驳回审批
   */
  const handleBatchReject = useCallback(async (approvalIds: string[], reason: string) => {
    setLoading(true);
    try {
      const response = await batchRejectApprovals(approvalIds, reason);
      message.success(`批量驳回完成，成功 ${response.data.data.successCount} 条`);
      return response.data.data;
    } catch (error) {
      console.error('批量驳回审批失败:', error);
      return null;
    } finally {
      setLoading(false);
    }
  }, []);

  return {
    loading,
    approvalList,
    currentApproval,
    total,
    fetchPendingApprovals,
    fetchApprovalDetail,
    handleApprove,
    handleReject,
    handleCancel,
    handleBatchApprove,
    handleBatchReject,
  };
};

/**
 * 审批流程管理 Hook
 */
export const useApprovalFlow = () => {
  const [loading, setLoading] = useState(false);
  const [flowList, setFlowList] = useState<ApprovalFlow[]>([]);
  const [currentFlow, setCurrentFlow] = useState<ApprovalFlow | null>(null);
  const [total, setTotal] = useState(0);

  /**
   * 获取审批流程列表
   */
  const fetchFlowList = useCallback(async (params?: ApprovalFlowListParams) => {
    setLoading(true);
    try {
      const response = await getApprovalFlowList(params);
      setFlowList(response.data.data || []);
      setTotal(response.data.page?.total || 0);
      return {
        list: response.data.data || [],
        total: response.data.page?.total || 0,
      };
    } catch (error) {
      console.error('获取审批流程列表失败:', error);
      return { list: [], total: 0 };
    } finally {
      setLoading(false);
    }
  }, []);

  /**
   * 获取审批流程详情
   */
  const fetchFlowDetail = useCallback(async (id: string) => {
    setLoading(true);
    try {
      const response = await getApprovalFlowDetail(id);
      setCurrentFlow(response.data.data);
      return response.data.data;
    } catch (error) {
      console.error('获取审批流程详情失败:', error);
      return null;
    } finally {
      setLoading(false);
    }
  }, []);

  /**
   * 创建审批流程
   */
  const handleCreateFlow = useCallback(async (data: CreateApprovalFlowParams) => {
    setLoading(true);
    try {
      const response = await createApprovalFlow(data);
      message.success('审批流程创建成功');
      return response.data.data;
    } catch (error) {
      console.error('创建审批流程失败:', error);
      return null;
    } finally {
      setLoading(false);
    }
  }, []);

  /**
   * 更新审批流程
   */
  const handleUpdateFlow = useCallback(async (id: string, data: UpdateApprovalFlowParams) => {
    setLoading(true);
    try {
      const response = await updateApprovalFlow(id, data);
      message.success('审批流程更新成功');
      return response.data.data;
    } catch (error) {
      console.error('更新审批流程失败:', error);
      return null;
    } finally {
      setLoading(false);
    }
  }, []);

  return {
    loading,
    flowList,
    currentFlow,
    total,
    fetchFlowList,
    fetchFlowDetail,
    handleCreateFlow,
    handleUpdateFlow,
  };
};
