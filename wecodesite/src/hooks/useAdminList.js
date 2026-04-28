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
    currentItem, setCurrentItem,
    mode, setMode,
  };
};

const createListOperations = (state, options) => {
  const { setLoading, setData, setPagination, keyword, categoryId, status, pagination, categories, setCategories } = state;

  const loadCategories = useCallback(async () => {
    if (!options.fetchCategories) return;
    const result = await options.fetchCategories();
    if (result.code === '200') {
      setCategories(result.data || []);
    }
  }, [options.fetchCategories, setCategories]);

  const loadData = useCallback(async (params = {}) => {
    setLoading(true);
    try {
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
      handleListResponse(result, setData, setPagination, finalPage, finalSize);
    } catch (error) {
      message.error('加载数据失败');
    } finally {
      setLoading(false);
    }
  }, [keyword, categoryId, status, pagination.curPage, pagination.pageSize, options.fetchList, setLoading, setData, setPagination]);

  const handleSearch = useCallback(() => {
    loadData({ curPage: 1 });
  }, [loadData]);

  const handlePageChange = useCallback((page, size) => {
    loadData({ curPage: page, pageSize: size });
  }, [loadData]);

  const handleCategoryChange = useCallback((value) => {
    state.setCategoryId(value);
    loadData({ categoryId: value });
  }, [loadData, state]);

  const handleStatusChange = useCallback((value) => {
    state.setStatus(value);
    loadData({ status: value });
  }, [loadData, state]);

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
  const { setCurrentItem, setMode, setModalVisible } = state;
  let loadDataRef = null;

  const handleAdd = useCallback(() => {
    setCurrentItem(null);
    setMode('create');
    setModalVisible(true);
  }, [setCurrentItem, setMode, setModalVisible]);

  const handleEdit = useCallback((record) => {
    setCurrentItem({ id: record.id });
    setMode('edit');
    setModalVisible(true);
  }, [setCurrentItem, setMode, setModalVisible]);

  const handleView = useCallback((record) => {
    setCurrentItem({ id: record.id });
    setMode('view');
    setModalVisible(true);
  }, [setCurrentItem, setMode, setModalVisible]);

  const handleDelete = useCallback(async (id) => {
    if (!options.deleteItem) return;
    const res = await options.deleteItem(id);
    if (res && res.code === '200') {
      message.success('删除成功');
      loadDataRef?.();
    } else {
      message.error(res?.message || '删除失败');
    }
  }, [options.deleteItem]);

  const setLoadData = useCallback((fn) => {
    loadDataRef = fn;
  }, []);

  return {
    handleAdd,
    handleEdit,
    handleView,
    handleDelete,
    setLoadData,
  };
};

const createModalOperations = (state) => {
  const { setModalVisible, setCurrentItem, setMode } = state;
  let loadDataRef = null;

  const handleSuccess = useCallback(() => {
    setModalVisible(false);
    loadDataRef?.();
  }, [setModalVisible]);

  const openModal = useCallback((item, editMode) => {
    setCurrentItem(item);
    setMode(editMode ? 'edit' : 'create');
    setModalVisible(true);
  }, [setCurrentItem, setModalVisible]);

  const closeModal = useCallback(() => {
    setModalVisible(false);
    setCurrentItem(null);
  }, [setModalVisible, setCurrentItem]);

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
    convertToTreeData,
  };
};

const getFinalParam = (params, key, defaultValue) => {
  return key in params ? params[key] : defaultValue;
};

const buildListRequestParams = ({ keyword, categoryId, status, curPage, pageSize }) => {
  const requestParams = { keyword, categoryId, status, curPage, pageSize };
  return filterUndefinedParams(requestParams);
};

const filterUndefinedParams = (params) => {
  return Object.fromEntries(
    Object.entries(params).filter(([_, value]) => value !== undefined)
  );
};

const handleListResponse = (result, setData, setPagination, curPage, pageSize) => {
  if (result.code === '200') {
    setData(result.data);
    setPagination(prev => ({ ...prev, total: result.page?.total || 0, curPage, pageSize }));
  }
};

const convertToTreeData = (categoryList) => {
  if (!categoryList) return [];
  return categoryList.map(cat => ({
    value: cat.id,
    title: cat.nameCn,
    key: cat.id,
    children: cat.children ? convertToTreeData(cat.children) : undefined
  }));
};
