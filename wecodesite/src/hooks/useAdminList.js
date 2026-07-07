import { useState, useCallback } from 'react';
import { message } from 'antd';
import { INIT_PAGECONFIG } from '../utils/constants';

const createState = () => {
  const [data, setData] = useState([]);
  const [loading, setLoading] = useState(false);
  const [pagination, setPagination] = useState(INIT_PAGECONFIG);
  const [keyword, setKeyword] = useState('');
  const [categoryId, setCategoryId] = useState(undefined);
  const [status, setStatus] = useState(undefined);
  const [categories, setCategories] = useState([]);
  const [modalVisible, setModalVisible] = useState(false);
  const [removeConfirmVisible, setRemoveConfirmVisible] = useState(false);
  const [currentItem, setCurrentItem] = useState(null);
  const [mode, setMode] = useState('create');

  return {
    data, setData,
    loading, setLoading,
    pagination, setPagination,
    keyword, setKeyword,
    categoryId, setCategoryId,
    status, setStatus,
    categories, setCategories,
    modalVisible, setModalVisible,
    removeConfirmVisible, setRemoveConfirmVisible,
    currentItem, setCurrentItem,
    mode, setMode,
  };
};

const createListOperations = (state, options) => {
  const { setLoading, setData, setPagination, keyword, categoryId, setCategoryId, status, setStatus, pagination, setCategories, setKeyword } = state;

  const loadCategories = useCallback(async () => {
    if (!options.fetchCategories) return;
    const result = await options.fetchCategories();
    if (result && result.code === '200') {
      setCategories(result.data || []);
    } else {
      message.error(result?.messageZh || result?.message || '加载分类失败');
    }
  }, [options.fetchCategories]);

  const loadData = useCallback(async (params = {}) => {
    setLoading(true);
    const finalKeyword = getFinalParam(params, 'keyword', keyword);
    const finalCategoryId = getFinalParam(params, 'categoryId', categoryId);
    const finalStatus = getFinalParam(params, 'status', status);
    const finalPage = getFinalParam(params, 'curPage', pagination.curPage);
    const finalSize = getFinalParam(params, 'pageSize', pagination.pageSize);

    const requestParams = buildListRequestParams({
      keyword: finalKeyword,
      categoryId: finalCategoryId,
      status: finalStatus,
      curPage: finalPage,
      pageSize: finalSize,
    });

    const result = await options.fetchList(requestParams);
    if (result && result.code === '200') {
      handleListResponse(result, setData, setPagination, finalPage, finalSize);
    } else {
      message.error(result?.messageZh || result.message || '加载数据失败');
    }
    setLoading(false);
  }, [keyword, categoryId, status, pagination.curPage, pagination.pageSize]);

  const handleSearch = useCallback((searchKey) => {
    setKeyword(searchKey);
    setPagination(INIT_PAGECONFIG);
  }, []);

  const handlePageChange = useCallback((page, size) => {
    setPagination({
      curPage: page,
      pageSize: size
    })
  }, []);

  const handleCategoryChange = useCallback((value) => {
    setCategoryId(value);
    setPagination(INIT_PAGECONFIG);
  }, []);

  const handleStatusChange = useCallback((value) => {
    setStatus(value);
    setPagination(INIT_PAGECONFIG);
  }, []);

  return {
    loadData,
    loadCategories,
    handleSearch,
    handlePageChange,
    handleCategoryChange,
    handleStatusChange,
  };
};

const createCrudOperations = (state, options) => {
  const { currentItem, setCurrentItem, setMode, setModalVisible, setRemoveConfirmVisible, pagination } = state;
  let loadDataRef = null;

  const handleAdd = useCallback(() => {
    setCurrentItem(null);
    setMode('create');
    setModalVisible(true);
  }, []);

  const handleEdit = useCallback((id) => {
    setCurrentItem({ id });
    setMode('edit');
    setModalVisible(true);
  }, []);

  const handleView = useCallback((id) => {
    setCurrentItem({ id });
    setMode('view');
    setModalVisible(true);
  }, []);

  const handleDelete = useCallback((record) => {
    setCurrentItem(record);
    setRemoveConfirmVisible(true);
  }, []);

  const handleCloseDeleteConfirm = useCallback(() => {
    setCurrentItem(null);
    setRemoveConfirmVisible(false);
  }, []);

  const handleDeleteClick = useCallback(async () => {
    if (!options.deleteItem) return;
    const res = await options.deleteItem(currentItem.id);
    if (res && res.code === '200') {
      message.success('删除成功');
      setRemoveConfirmVisible(false);
      setCurrentItem(null);
      loadDataRef?.(INIT_PAGECONFIG);
    } else {
      message.error(res?.messageZh || res?.message || '删除失败');
    }
  }, [currentItem, options.deleteItem]);

  const setLoadData = useCallback((fn) => {
    loadDataRef = fn;
  }, [currentItem, options.deleteItem]);

  return {
    handleAdd,
    handleEdit,
    handleView,
    handleDelete,
    handleCloseDeleteConfirm,
    handleDeleteClick,
    setLoadData,
  };
};

const createModalOperations = (state) => {
  const { setModalVisible, setCurrentItem, setMode } = state;
  let loadDataRef = null;

  const handleSuccess = useCallback(() => {
    setModalVisible(false);
    loadDataRef?.();
  }, []);

  const openModal = useCallback((item, editMode) => {
    setCurrentItem(item);
    setMode(editMode ? 'edit' : 'create');
    setModalVisible(true);
  }, []);

  const closeModal = useCallback(() => {
    setModalVisible(false);
    setCurrentItem(null);
  }, []);

  const setLoadData = useCallback((fn) => {
    loadDataRef = fn;
  }, []);

  return {
    handleSuccess,
    openModal,
    closeModal,
    setLoadData,
  };
};

export const useAdminList = (options) => {
  const state = createState();
  const listOps = createListOperations(state, options);
  const crudOps = createCrudOperations(state, options);
  const modalOps = createModalOperations(state);

  crudOps.setLoadData(listOps.loadData);
  modalOps.setLoadData(listOps.loadData);

  return {
    ...state,
    ...listOps,
    ...crudOps,
    ...modalOps,
  };
};

const getFinalParam = (params, key, defaultValue) => {
  return key in params ? params[key] : defaultValue;
};

const buildListRequestParams = (params) => {
  return Object.fromEntries(
    Object.entries(params)
      .filter(([_, value]) => value !== undefined)
  );
};

const handleListResponse = (result, setData, setPagination, curPage, pageSize) => {
  if (result && result.code === '200') {
    setData(result.data);
    setPagination(prev => ({ ...prev, total: result.page?.total || 0, curPage, pageSize }));
  } else {
    message.error(result?.messageZh || result?.message || '加载数据失败');
  }
};
