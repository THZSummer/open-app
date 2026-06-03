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
  const [revokeVisible, setRevokeVisible] = useState(false);
  const [currentWithdrawId, setCurrentWithdrawId] = useState(null);
  const [revokePending, setRevokePending] = useState(false);

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
    revokeVisible, setRevokeVisible,
    currentWithdrawId, setCurrentWithdrawId,
    revokePending, setRevokePending,
  };
};

const createListOperations = (state, appId, options) => {
  const { setLoading, setData, setPagination, pagination } = state;

  const loadData = useCallback(async (page = pagination.curPage, size = pagination.pageSize) => {
    if (!appId) return;
    setLoading(true);
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
      message.error(result?.messageZh || result?.message || '加载列表失败');
    }
    setLoading(false);
  }, [appId, pagination.curPage, pagination.pageSize, options.fetchList]);

  const handlePageChange = useCallback((page, size) => {
    setPagination({
      curPage: page,
      pageSize: size
    })
  }, [loadData]);

  return {
    loadData,
    handlePageChange,
  };
};

const createDrawerOperations = (state) => {
  const { setDrawerOpen } = state;
  let loadDataRef = null;

  const openDrawer = useCallback(() => setDrawerOpen(true), []);

  const closeDrawer = useCallback(() => setDrawerOpen(false), []);

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
    const res = await options.subscribe(appId, { permissionIds });
    if (res && res.code === '200') {
      message.success('申请已提交');
      loadDataRef?.(1, INIT_PAGECONFIG.pageSize);
      setDrawerOpen(false);
    } else {
      message.error(res?.messageZh || res?.message || '申请失败');
    }
    setSubscribeLoading(false);
  }, [appId, options.subscribe]);

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
      approver: `${record.approver?.userName} ${record.approver?.userId}` || '待分配',
      approvalUrl: record.approvalUrl || '',
      approvalUser: record.applicantId,
    });
    setApprovalModalOpen(true);
  }, []);

  const closeApprovalModal = useCallback(() => setApprovalModalOpen(false), []);

  return {
    handleCopyApprovalAddress,
    closeApprovalModal,
  };
};

const createDeleteOperations = (state, appId, options) => {
  const { setDeleteModalOpen, setCurrentDeleteId, setDeleteLoading, pagination } = state;
  let loadDataRef = null;
  const deleteIdRef = useRef(null);

  const handleDelete = useCallback((id) => {
    deleteIdRef.current = id;
    setCurrentDeleteId(id);
    setDeleteModalOpen(true);
  }, []);

  const handleConfirmDelete = useCallback(async () => {
    if (!deleteIdRef.current) return;
    setDeleteLoading(true);
    const res = await options.deleteItem(appId, deleteIdRef.current);
    if (res && res.code === '200') {
      message.success('删除成功');
      setDeleteModalOpen(false);
      deleteIdRef.current = null;
      loadDataRef?.(INIT_PAGECONFIG);
    } else {
      message.error(res?.messageZh || res?.message || '删除失败');
    }
    setDeleteLoading(false);
  }, [appId, options.deleteItem]);

  const closeDeleteModal = useCallback(() => setDeleteModalOpen(false), []);

  const setLoadData = useCallback((fn) => {
    loadDataRef = fn;
  }, []);

  return {
    handleDelete,
    handleConfirmDelete,
    closeDeleteModal,
    setLoadData,
  };
};

const createWithdrawOperations = (state, appId, options) => {
  const { setCurrentWithdrawId, setRevokePending, setRevokeVisible, pagination } = state;
  let loadDataRef = null;
  const withdrawIdRef = useRef(null);

  const handleWithdraw = useCallback((id) => {
    withdrawIdRef.current = id;
    setCurrentWithdrawId(id);
    setRevokeVisible(true);
  }, []);

  const handleConfirmWithdraw = useCallback(async () => {
    if (!withdrawIdRef.current) return;
    setRevokePending(true);
    const res = await options.withdraw(appId, withdrawIdRef.current);
    if (res && res.code === '200') {
      message.success('已撤回');
      setRevokeVisible(false);
      withdrawIdRef.current = null;
      loadDataRef?.();
    } else {
      message.error(res?.messageZh || res?.message || '撤回失败');
    }
    setRevokePending(false);
  }, [appId, options.withdraw]);

  const setLoadData = useCallback((fn) => {
    loadDataRef = fn;
  }, []);
  
  const closeWithdrawModal = useCallback(() => setRevokeVisible(false), []);

  return {
    handleConfirmWithdraw,
    handleWithdraw,
    closeWithdrawModal,
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
