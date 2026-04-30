import React, { useState, useEffect } from 'react';
import { Drawer, Table, Button, Pagination, Input, Select, message } from 'antd';
import { PAGE_SIZE_OPTIONS, INIT_PAGECONFIG } from '../../utils/constants';
import { NEED_REVIEW_OPTIONS } from './constants';

function ResourceDrawer({
  open,
  onClose,
  onConfirm,
  selectedItems = [],
  subscribeLoading = false,
  appId,
  title = '添加资源',
  className = 'resource-drawer',
  placeholder = '资源名称/标识',
  fetchCategories,
  fetchData,
  getColumns,
}) {
  const [selectedRowKeys, setSelectedRowKeys] = useState(
    selectedItems.map(item => item.id)
  );
  const [pagination, setPagination] = useState(INIT_PAGECONFIG);
  const [allItems, setAllItems] = useState([]);
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

    const result = await fetchData(defaultParams);
    if (result && result.code === '200') {
      const resultData = result.data || [];
      const resultTotal = result.total || resultData.length;
      setAllItems(resultData);
      setPagination(prev => ({ ...prev, total: resultTotal }));
    } else if (Array.isArray(result?.data)) {
      setAllItems(result.data);
      setPagination(prev => ({ ...prev, total: result.total || result.data.length }));
    } else {
      message.error(result?.message || result?.messageZh || '加载列表失败');
      setAllItems([]);
      setPagination(prev => ({ ...prev, total: 0 }));
    }
    setLoading(false);
  };

  useEffect(() => {
    if (!open) return;

    setFilterKeyword('');
    setFilterNeedReview('all');
    setPagination(INIT_PAGECONFIG);
    setSelectedRowKeys(selectedItems.map(item => item.id));

    const initData = async () => {
      const categoriesRes = await fetchCategories();
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
    const selected = allItems.filter(item =>
      selectedRowKeys.includes(item.id)
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

  const handleOpenDoc = (docUrl) => {
    if (docUrl) {
      window.open(docUrl, '_blank');
    }
  };

  const columns = getColumns({ handleOpenDoc });

  const rowSelection = {
    selectedRowKeys,
    onChange: handleSelectChange,
    getCheckboxProps: (record) => ({
      disabled: record.isSubscribed === 1,
    }),
  };

  return (
    <Drawer
      title={title}
      placement="right"
      width={700}
      onClose={onClose}
      open={open}
      className={className}
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
      <div className={`${className}-content`}>
        <div className={`${className}-filter`}>
          <Input
            placeholder={placeholder}
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
        <div className={`${className}-main`}>
          <div className={`${className}-table`}>
            <Table
              rowSelection={rowSelection}
              columns={columns}
              dataSource={allItems}
              rowKey="id"
              pagination={false}
              loading={loading}
            />
            <div className={`${className}-pagination`}>
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
        </div>
      </div>
    </Drawer>
  );
}

export default ResourceDrawer;
