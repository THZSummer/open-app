import { useState, useCallback } from 'react';
import { message } from 'antd';
import {
  getApiList,
  getApiDetail,
  createApi,
  updateApi,
  deleteApi,
  withdrawApi,
  Api,
  ApiListParams,
  CreateApiParams,
  UpdateApiParams,
} from '@/services/api.service';

/**
 * API 管理 Hook
 */
export const useApi = () => {
  const [loading, setLoading] = useState(false);
  const [apiList, setApiList] = useState<Api[]>([]);
  const [currentApi, setCurrentApi] = useState<Api | null>(null);
  const [total, setTotal] = useState(0);

  /**
   * 获取 API 列表
   */
  const fetchApiList = useCallback(async (params: ApiListParams) => {
    setLoading(true);
    try {
      const response = await getApiList(params);
      setApiList(response.data.data || []);
      setTotal(response.data.page?.total || 0);
      return {
        list: response.data.data || [],
        total: response.data.page?.total || 0,
      };
    } catch (error) {
      console.error('获取 API 列表失败:', error);
      return { list: [], total: 0 };
    } finally {
      setLoading(false);
    }
  }, []);

  /**
   * 获取 API 详情
   */
  const fetchApiDetail = useCallback(async (id: string) => {
    setLoading(true);
    try {
      const response = await getApiDetail(id);
      setCurrentApi(response.data.data);
      return response.data.data;
    } catch (error) {
      console.error('获取 API 详情失败:', error);
      return null;
    } finally {
      setLoading(false);
    }
  }, []);

  /**
   * 注册 API
   */
  const handleCreateApi = useCallback(async (data: CreateApiParams) => {
    setLoading(true);
    try {
      const response = await createApi(data);
      message.success('API 注册成功，等待审批');
      return response.data.data;
    } catch (error) {
      console.error('注册 API 失败:', error);
      return null;
    } finally {
      setLoading(false);
    }
  }, []);

  /**
   * 更新 API
   */
  const handleUpdateApi = useCallback(async (id: string, data: UpdateApiParams) => {
    setLoading(true);
    try {
      const response = await updateApi(id, data);
      message.success('API 更新成功');
      return response.data.data;
    } catch (error) {
      console.error('更新 API 失败:', error);
      return null;
    } finally {
      setLoading(false);
    }
  }, []);

  /**
   * 删除 API
   */
  const handleDeleteApi = useCallback(async (id: string) => {
    setLoading(true);
    try {
      await deleteApi(id);
      message.success('API 删除成功');
      return true;
    } catch (error) {
      console.error('删除 API 失败:', error);
      return false;
    } finally {
      setLoading(false);
    }
  }, []);

  /**
   * 撤回 API
   */
  const handleWithdrawApi = useCallback(async (id: string) => {
    setLoading(true);
    try {
      const response = await withdrawApi(id);
      message.success('API 已撤回');
      return response.data.data;
    } catch (error) {
      console.error('撤回 API 失败:', error);
      return null;
    } finally {
      setLoading(false);
    }
  }, []);

  return {
    loading,
    apiList,
    currentApi,
    total,
    fetchApiList,
    fetchApiDetail,
    handleCreateApi,
    handleUpdateApi,
    handleDeleteApi,
    handleWithdrawApi,
  };
};
