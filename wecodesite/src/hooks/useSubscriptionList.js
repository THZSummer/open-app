import { useState, useCallback, useRef } from 'react';
import { message } from 'antd';
import { INIT_PAGECONFIG } from '../utils/constants';

const createState = () => {
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

  return {
    data, setData,
    loading, setLoading,
    pagination, setPagination,
    drawerOpen, setDrawerOpen,
    subscribeLoading, setSubscribeLoading,
    approvalModalOpen, setApprovalModalOpen,
    currentApprovalInfo, setCurrentApprovalInfo,
    deleteModalOpen, setDeleteModalOpen,
    currentDeleteId, setCurrentDeleteId,
    deleteLoading, setDeleteLoading,
  };
};

const createListOperations = (state, appId, options) => {
  const { setLoading, setData, setPagination, pagination } = state;

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
  }, [appId, pagination.pageSize, options.fetchList, setLoading, setData, setPagination]);

  const handlePageChange = useCallback((page, size) => {
    loadData(page, size);
  }, [loadData]);

  return {
    loadData,
    handlePageChange,
  };
};

const createDrawerOperations = (state) => {
  const { setDrawerOpen } = state;
  let loadDataRef = null;

  const openDrawer = useCallback(() => setDrawerOpen(true), [setDrawerOpen]);

  const closeDrawer = useCallback(() => setDrawerOpen(false), [setDrawerOpen]);

  const setLoadData = useCallback((fn) => {
    loadDataRef = fn;
  }, []);

  return {
    openDrawer,
    closeDrawer,
    setLoadData,
  };
};

const createSubscribeOperations = (state, appId, options) => {
  const { setSubscribeLoading, setDrawerOpen } = state;
  let loadDataRef = null;

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
        loadDataRef?.(1, INIT_PAGECONFIG.pageSize);
        setDrawerOpen(false);
      } else {
        message.error(res?.message || '申请失败');
      }
    } catch (error) {
      message.error('申请失败');
    } finally {
      setSubscribeLoading(false);
    }
  }, [appId, setSubscribeLoading, setDrawerOpen, options.subscribe]);

  const setLoadData = useCallback((fn) => {
    loadDataRef = fn;
  }, []);

  return {
    handleSubscribe,
    setLoadData,
  };
};

const createApprovalOperations = (state) => {
  const { setApprovalModalOpen, setCurrentApprovalInfo } = state;

  const handleCopyApprovalAddress = useCallback((record) => {
    setCurrentApprovalInfo({
      id: record.id,
      approver: record.approver?.userName || '待分配',
      approvalUrl: record.approvalUrl || ''
    });
    setApprovalModalOpen(true);
  }, [setApprovalModalOpen, setCurrentApprovalInfo]);

  const closeApprovalModal = useCallback(() => setApprovalModalOpen(false), [setApprovalModalOpen]);

  return {
    handleCopyApprovalAddress,
    closeApprovalModal,
  };
};

const createDeleteOperations = (state, appId, options) => {
  const { setDeleteModalOpen, setCurrentDeleteId, setDeleteLoading } = state;
  let loadDataRef = null;
  const deleteIdRef = useRef(null);

  const handleDeleteClick = useCallback((id) => {
    deleteIdRef.current = id;
    setCurrentDeleteId(id);
    setDeleteModalOpen(true);
  }, [setDeleteModalOpen, setCurrentDeleteId]);

  const handleConfirmDelete = useCallback(async () => {
    if (!deleteIdRef.current) return;
    setDeleteLoading(true);
    try {
      const res = await options.deleteItem(appId, deleteIdRef.current);
      if (res && res.code === '200') {
        message.success('删除成功');
        setDeleteModalOpen(false);
        deleteIdRef.current = null;
        loadDataRef?.(1);
      } else {
        message.error(res?.message || '删除失败');
      }
    } catch (error) {
      message.error('删除失败');
    } finally {
      setDeleteLoading(false);
    }
  }, [appId, setDeleteLoading, setDeleteModalOpen, options.deleteItem]);

  const closeDeleteModal = useCallback(() => setDeleteModalOpen(false), [setDeleteModalOpen]);

  const setLoadData = useCallback((fn) => {
    loadDataRef = fn;
  }, []);

  return {
    handleDeleteClick,
    handleConfirmDelete,
    closeDeleteModal,
    setLoadData,
  };
};

const createWithdrawOperations = (state, appId, options) => {
  let loadDataRef = null;

  const handleWithdraw = useCallback(async (record) => {
    try {
      const res = await options.withdraw(appId, record.id);
      if (res && res.code === '200') {
        message.success('已撤回');
        loadDataRef?.();
      } else {
        message.error(res?.message || '撤回失败');
      }
    } catch (error) {
      message.error('撤回失败');
    }
  }, [appId, options.withdraw]);

  const setLoadData = useCallback((fn) => {
    loadDataRef = fn;
  }, []);

  return {
    handleWithdraw,
    setLoadData,
  };
};

export const useSubscriptionList = (appId, options) => {
  const state = createState();
  const listOps = createListOperations(state, appId, options);
  const drawerOps = createDrawerOperations(state);
  const subscribeOps = createSubscribeOperations(state, appId, options);
  const approvalOps = createApprovalOperations(state);
  const deleteOps = createDeleteOperations(state, appId, options);
  const withdrawOps = createWithdrawOperations(state, appId, options);

  drawerOps.setLoadData(listOps.loadData);
  subscribeOps.setLoadData(listOps.loadData);
  deleteOps.setLoadData(listOps.loadData);
  withdrawOps.setLoadData(listOps.loadData);

  return {
    ...state,
    ...listOps,
    ...drawerOps,
    ...subscribeOps,
    ...approvalOps,
    ...deleteOps,
    ...withdrawOps,
  };
};
