import { useState, useCallback } from 'react';
import { message } from 'antd';
import { INIT_PAGECONFIG } from '../utils/constants';

export const useAdminList = (options) => {
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

  const loadCategories = useCallback(async () => {
    if (!options.fetchCategories) return;
    const result = await options.fetchCategories();
    if (result.code === '200') {
      setCategories(result.data || []);
    }
  }, [options.fetchCategories]);

  const convertToTreeData = useCallback((categoryList) => {
    if (!categoryList) return [];
    return categoryList.map(cat => ({
      value: cat.id,
      title: cat.nameCn,
      key: cat.id,
      children: cat.children ? convertToTreeData(cat.children) : undefined
    }));
  }, []);

  const loadData = useCallback(async (params = {}) => {
    setLoading(true);
    try {
      const finalKeyword = 'keyword' in params ? params.keyword : keyword;
      const finalCategoryId = 'categoryId' in params ? params.categoryId : categoryId;
      const finalStatus = 'status' in params ? params.status : status;
      const finalPage = 'curPage' in params ? params.curPage : pagination.curPage;
      const finalSize = 'pageSize' in params ? params.pageSize : pagination.pageSize;

      const requestParams = {
        keyword: finalKeyword,
        categoryId: finalCategoryId,
        status: finalStatus,
        curPage: finalPage,
        pageSize: finalSize,
      };

      const filteredParams = Object.fromEntries(
        Object.entries(requestParams).filter(([_, value]) => value !== undefined)
      );

      const result = await options.fetchList(filteredParams);
      if (result.code === '200') {
        setData(result.data);
        setPagination(prev => ({ ...prev, total: result.page?.total || 0, curPage: finalPage, pageSize: finalSize }));
      }
    } catch (error) {
      message.error('加载数据失败');
    } finally {
      setLoading(false);
    }
  }, [keyword, categoryId, status, pagination.curPage, pagination.pageSize, options.fetchList]);

  const handleSearch = useCallback(() => {
    loadData({ curPage: 1 });
  }, [loadData]);

  const handlePageChange = useCallback((page, size) => {
    loadData({ curPage: page, pageSize: size });
  }, [loadData]);

  const handleCategoryChange = useCallback((value) => {
    setCategoryId(value);
    loadData({ categoryId: value });
  }, [loadData]);

  const handleStatusChange = useCallback((value) => {
    setStatus(value);
    loadData({ status: value });
  }, [loadData]);

  const handleAdd = useCallback(() => {
    setCurrentItem(null);
    setMode('create');
    setModalVisible(true);
  }, []);

  const handleEdit = useCallback((record) => {
    setCurrentItem({ id: record.id });
    setMode('edit');
    setModalVisible(true);
  }, []);

  const handleView = useCallback((record) => {
    setCurrentItem({ id: record.id });
    setMode('view');
    setModalVisible(true);
  }, []);

  const handleDelete = useCallback(async (id) => {
    if (!options.deleteItem) return;
    const res = await options.deleteItem(id);
    if (res && res.code === '200') {
      message.success('删除成功');
      loadData();
    } else {
      message.error(res?.message || '删除失败');
    }
  }, [loadData, options.deleteItem]);

  const handleSuccess = useCallback(() => {
    setModalVisible(false);
    loadData();
  }, [loadData]);

  const openModal = useCallback((item, editMode) => {
    setCurrentItem(item);
    setMode(editMode ? 'edit' : 'create');
    setModalVisible(true);
  }, []);

  const closeModal = useCallback(() => {
    setModalVisible(false);
    setCurrentItem(null);
  }, []);

  return {
    data,
    loading,
    pagination,
    keyword,
    categoryId,
    status,
    categories,
    modalVisible,
    currentItem,
    mode,
    setKeyword,
    setCategoryId,
    setStatus,
    loadData,
    loadCategories,
    convertToTreeData,
    handleSearch,
    handlePageChange,
    handleCategoryChange,
    handleStatusChange,
    handleAdd,
    handleEdit,
    handleView,
    handleDelete,
    handleSuccess,
    openModal,
    closeModal,
  };
};
