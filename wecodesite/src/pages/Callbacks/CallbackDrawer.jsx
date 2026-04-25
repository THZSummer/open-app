import React, { useState, useEffect } from 'react';
import { Drawer, Table, Button, Pagination, Input, Select, message } from 'antd';
import { fetchCallbackCategories, fetchCallbacks } from './thunk';
import { PAGE_SIZE_OPTIONS, INIT_PAGECONFIG } from '../../utils/constants';
import { getCallbackDrawerColumns, NEED_REVIEW_OPTIONS } from './constants';
import './CallbackDrawer.m.less';

function CallbackDrawer({ open, onClose, onConfirm, selectedCallbacks = [], subscribeLoading = false, appId }) {
  const [selectedRowKeys, setSelectedRowKeys] = useState(
    selectedCallbacks.map(c => c.id)
  );
  const [pagination, setPagination] = useState(INIT_PAGECONFIG);
  const [allCallbacks, setAllCallbacks] = useState([]);
  const [rootCategoryId, setRootCategoryId] = useState('');
  const [loading, setLoading] = useState(false);
  const [filterKeyword, setFilterKeyword] = useState('');
  const [filterNeedReview, setFilterNeedReview] = useState('all');

  const loadData = async (params = {}) => {
    const currentCategoryId = params.categoryId || rootCategoryId;
    if (!currentCategoryId) {
      setLoading(false);
      return;
    }
    
    setLoading(true);
    const defaultParams = {
      keyword: filterKeyword,
      needReview: filterNeedReview,
      categoryId: currentCategoryId,
      curPage: pagination.curPage,
      pageSize: pagination.pageSize,
      appId: appId,
      ...params
    };
    
    const result = await fetchCallbacks(defaultParams);
    if (result && result.code === '200') {
      const resultData = result.data || [];
      const resultTotal = result.total || resultData.length;
      setAllCallbacks(resultData);
      setPagination(prev => ({ ...prev, total: resultTotal }));
    } else if (Array.isArray(result?.data)) {
      setAllCallbacks(result.data);
      setPagination(prev => ({ ...prev, total: result.total || result.data.length }));
    } else {
      message.error(result?.message || result?.messageZh || '加载回调列表失败');
      setAllCallbacks([]);
      setPagination(prev => ({ ...prev, total: 0 }));
    }
    setLoading(false);
  };

  useEffect(() => {
    if (!open) return;
    
    setFilterKeyword('');
    setFilterNeedReview('all');
    setPagination(INIT_PAGECONFIG);
    setSelectedRowKeys(selectedCallbacks.map(c => c.id));
    
    const initData = async () => {
      const categoriesRes = await fetchCallbackCategories();
      if (categoriesRes && categoriesRes.code === '200') {
        const rootId = categoriesRes.data?.[0]?.id;
        if (rootId) {
          setRootCategoryId(rootId);
          await loadData({ categoryId: rootId });
        }
      } else if (Array.isArray(categoriesRes) && categoriesRes.length > 0) {
        const rootId = categoriesRes[0]?.id;
        if (rootId) {
          setRootCategoryId(rootId);
          await loadData({ categoryId: rootId });
        }
      } else {
        message.error(categoriesRes?.message || '加载分类失败');
      }
    };
    
    initData();
  }, [open]);

  const handlePageChange = async (page, size) => {
    setPagination(prev => ({ ...prev, curPage: page, pageSize: size }));
    await loadData({ curPage: page, pageSize: size });
  };

  const handleSelectChange = (keys) => {
    setSelectedRowKeys(keys);
  };

  const handleConfirm = () => {
    const selected = allCallbacks.filter(callback =>
      selectedRowKeys.includes(callback.id)
    );
    onConfirm(selected);
    setSelectedRowKeys([]);
    setPagination(INIT_PAGECONFIG);
    onClose();
  };

  const handleFilterChange = async (e) => {
    const keyword = e.target.value;
    setFilterKeyword(keyword);
    setPagination(INIT_PAGECONFIG);
    await loadData({ keyword, curPage: 1 });
  };

  const handleNeedReviewChange = async (value) => {
    setFilterNeedReview(value);
    setPagination(INIT_PAGECONFIG);
    await loadData({ needReview: value, curPage: 1 });
  };

  const columns = getCallbackDrawerColumns();

  const rowSelection = {
    selectedRowKeys,
    onChange: handleSelectChange,
    getCheckboxProps: (record) => ({
      disabled: record.isSubscribed === 1,
    }),
  };

  return (
    <Drawer
      title="添加回调"
      placement="right"
      width={700}
      onClose={onClose}
      open={open}
      className="callback-drawer"
      footer={
        <div className="drawer-footer">
          <Button onClick={onClose}>取消</Button>
          <Button
            type="primary"
            disabled={selectedRowKeys.length === 0 || subscribeLoading}
            loading={subscribeLoading}
            onClick={handleConfirm}
          >
            确认添加
          </Button>
        </div>
      }
    >
      <div className="drawer-filter">
        <Input
          placeholder="回调名称/Scope"
          value={filterKeyword}
          onChange={handleFilterChange}
          style={{ width: 200 }}
          allowClear
        />
        <Select
          placeholder="是否需要审核"
          value={filterNeedReview}
          onChange={handleNeedReviewChange}
          options={NEED_REVIEW_OPTIONS}
          style={{ width: 150 }}
        />
      </div>
      <div className="drawer-table">
        <Table
          rowSelection={rowSelection}
          columns={columns}
          dataSource={allCallbacks}
          rowKey="id"
          pagination={false}
          loading={loading}
        />
        <div className="drawer-pagination">
          <span className="pagination-total">共 {pagination.total} 条</span>
          <Pagination
            current={pagination.curPage}
            pageSize={pagination.pageSize}
            total={pagination.total}
            onChange={handlePageChange}
            showSizeChanger
            pageSizeOptions={PAGE_SIZE_OPTIONS}
            showQuickJumper
          />
        </div>
      </div>
    </Drawer>
  );
}

export default CallbackDrawer;
