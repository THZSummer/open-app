import { useState } from 'react';
import { message } from 'antd';
import {
  getCallbackList,
  getCallbackDetail,
  createCallback,
  updateCallback,
  deleteCallback,
  withdrawCallback,
  Callback,
  CallbackListParams,
  CreateCallbackParams,
  UpdateCallbackParams,
} from '@/services/callback.service';

/**
 * 回调管理 Hook
 * 注意：导出时重命名为 useCallbackManager 避免与 React Hook 冲突
 */
export const useCallbackManager = () => {
  const [loading, setLoading] = useState(false);
  const [callbackList, setCallbackList] = useState<Callback[]>([]);
  const [currentCallback, setCurrentCallback] = useState<Callback | null>(null);
  const [total, setTotal] = useState(0);

  /**
   * 获取回调列表
   */
  const fetchCallbackList = async (params: CallbackListParams) => {
    setLoading(true);
    try {
      const response = await getCallbackList(params);
      setCallbackList(response.data.data || []);
      setTotal(response.data.page?.total || 0);
      return {
        list: response.data.data || [],
        total: response.data.page?.total || 0,
      };
    } catch (error) {
      console.error('获取回调列表失败:', error);
      return { list: [], total: 0 };
    } finally {
      setLoading(false);
    }
  };

  /**
   * 获取回调详情
   */
  const fetchCallbackDetail = async (id: string) => {
    setLoading(true);
    try {
      const response = await getCallbackDetail(id);
      setCurrentCallback(response.data.data);
      return response.data.data;
    } catch (error) {
      console.error('获取回调详情失败:', error);
      return null;
    } finally {
      setLoading(false);
    }
  };

  /**
   * 注册回调
   */
  const handleCreateCallback = async (data: CreateCallbackParams) => {
    setLoading(true);
    try {
      const response = await createCallback(data);
      message.success('回调注册成功，等待审批');
      return response.data.data;
    } catch (error) {
      console.error('注册回调失败:', error);
      return null;
    } finally {
      setLoading(false);
    }
  };

  /**
   * 更新回调
   */
  const handleUpdateCallback = async (id: string, data: UpdateCallbackParams) => {
    setLoading(true);
    try {
      const response = await updateCallback(id, data);
      message.success('回调更新成功');
      return response.data.data;
    } catch (error) {
      console.error('更新回调失败:', error);
      return null;
    } finally {
      setLoading(false);
    }
  };

  /**
   * 删除回调
   */
  const handleDeleteCallback = async (id: string) => {
    setLoading(true);
    try {
      await deleteCallback(id);
      message.success('回调删除成功');
      return true;
    } catch (error) {
      console.error('删除回调失败:', error);
      return false;
    } finally {
      setLoading(false);
    }
  };

  /**
   * 撤回回调
   */
  const handleWithdrawCallback = async (id: string) => {
    setLoading(true);
    try {
      const response = await withdrawCallback(id);
      message.success('回调已撤回');
      return response.data.data;
    } catch (error) {
      console.error('撤回回调失败:', error);
      return null;
    } finally {
      setLoading(false);
    }
  };

  return {
    loading,
    callbackList,
    currentCallback,
    total,
    fetchCallbackList,
    fetchCallbackDetail,
    handleCreateCallback,
    handleUpdateCallback,
    handleDeleteCallback,
    handleWithdrawCallback,
  };
};
