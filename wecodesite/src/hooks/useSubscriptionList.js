import { useState, useCallback } from 'react';
import { message } from 'antd';
import { INIT_PAGECONFIG } from '../utils/constants';

export const useSubscriptionList = (appId, options) => {
  const [data, setData] = useState([]);
  const [loading, setLoading] = useState(false);
  const [pagination, setPagination] = useState(INIT_PAGECONFIG);
  const [drawerOpen, setDrawerOpen] = useState(false);
  const [subscribeLoading, setSubscribeLoading] = useState(false);
  const [approvalModalOpen, setApprovalModalOpen] = useState(false);
  const [currentApprovalInfo, setCurrentApprovalInfo] = useState({});
  const [deleteModalOpen, setDeleteModalOpen] = useState(false);
  const [currentDeleteId, setCurrentDeleteId] = useState(null);
  const [deleteLoading, setDeleteLoading] = useState(false);

  const loadData = useCallback(async (page = 1, size = pagination.pageSize) => {
    if (!appId) return;
    setLoading(true);
    try {
      const result = await options.fetchList(appId, { curPage: page, pageSize: size });
      if (result && result.code === '200') {
        setData(result.data || []);
        setPagination(prev => ({
          ...prev,
          total: result.page?.total || 0,
          curPage: page,
          pageSize: size
        }));
      } else {
        message.error(result?.message || '加载列表失败');
      }
    } catch (error) {
      message.error('加载列表失败');
    } finally {
      setLoading(false);
    }
  }, [appId, pagination.pageSize, options.fetchList]);

  const handlePageChange = useCallback((page, size) => {
    loadData(page, size);
  }, [loadData]);

  const openDrawer = useCallback(() => setDrawerOpen(true), []);

  const closeDrawer = useCallback(() => setDrawerOpen(false), []);

  const handleSubscribe = useCallback(async (selectedItems) => {
    if (!appId) return;
    const permissionIds = selectedItems.filter(item => item.id).map(item => item.id);
    if (permissionIds.length === 0) {
      message.warning('没有可订阅的权限');
      return;
    }
    setSubscribeLoading(true);
    try {
      const res = await options.subscribe(appId, { permissionIds });
      if (res && res.code === '200') {
        message.success('申请已提交');
        loadData(1, INIT_PAGECONFIG.pageSize);
        setDrawerOpen(false);
      } else {
        message.error(res?.message || '申请失败');
      }
    } catch (error) {
      message.error('申请失败');
    } finally {
      setSubscribeLoading(false);
    }
  }, [appId, loadData, options.subscribe]);

  const handleCopyApprovalAddress = useCallback((record) => {
    setCurrentApprovalInfo({
      id: record.id,
      approver: record.approver?.userName || '待分配',
      approvalUrl: record.approvalUrl || ''
    });
    setApprovalModalOpen(true);
  }, []);

  const handleWithdraw = useCallback(async (record) => {
    try {
      const res = await options.withdraw(appId, record.id);
      if (res && res.code === '200') {
        message.success('已撤回');
        loadData();
      } else {
        message.error(res?.message || '撤回失败');
      }
    } catch (error) {
      message.error('撤回失败');
    }
  }, [appId, loadData, options.withdraw]);

  const handleDeleteClick = useCallback((id) => {
    setCurrentDeleteId(id);
    setDeleteModalOpen(true);
  }, []);

  const handleConfirmDelete = useCallback(async () => {
    setDeleteLoading(true);
    try {
      const res = await options.deleteItem(currentDeleteId);
      if (res && res.code === '200') {
        message.success('删除成功');
        setDeleteModalOpen(false);
        loadData();
      } else {
        message.error(res?.message || '删除失败');
      }
    } catch (error) {
      message.error('删除失败');
    } finally {
      setDeleteLoading(false);
    }
  }, [currentDeleteId, loadData, options.deleteItem]);

  return {
    data,
    loading,
    pagination,
    drawerOpen,
    subscribeLoading,
    approvalModalOpen,
    currentApprovalInfo,
    deleteModalOpen,
    deleteLoading,
    loadData,
    handlePageChange,
    openDrawer,
    closeDrawer,
    handleSubscribe,
    handleCopyApprovalAddress,
    handleWithdraw,
    handleDeleteClick,
    handleConfirmDelete,
    closeApprovalModal: () => setApprovalModalOpen(false),
    closeDeleteModal: () => setDeleteModalOpen(false),
  };
};
