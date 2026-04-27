import React, { useEffect } from 'react';
import { Button, Table, Spin, Empty, Pagination } from 'antd';
import { useAdminList } from '../../../hooks/useAdminList';
import AdminTableToolbar from '../../../components/AdminTableToolbar/AdminTableToolbar';
import { fetchCallbackList, deleteCallback } from './thunk';
import { fetchCategoryTree } from '../Category/thunk';
import CallbackRegister from './CallbackRegister';
import { getCallbackListColumns } from './constants';
import { PAGE_SIZE_OPTIONS } from '../../../utils/constants';
import './CallbackList.m.less';

function CallbackList() {
  const {
    data: callbackList,
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
    closeModal,
  } = useAdminList({
    fetchList: fetchCallbackList,
    fetchCategories: fetchCategoryTree,
    deleteItem: deleteCallback,
  });

  useEffect(() => {
    loadCategories();
    loadData();
  }, [loadCategories, loadData]);

  const columns = getCallbackListColumns({
    handleView,
    handleEdit,
    handleDelete,
  });

  return (
    <div className="callback-list">
      <div className="page-header">
        <div className="page-header-left">
          <h4 className="page-title">回调管理</h4>
          <span className="page-desc">管理回调接口，配置回调地址</span>
        </div>
        <Button type="primary" onClick={handleAdd} style={{ justifyContent: 'center', borderRadius: 6 }}>
          注册回调
        </Button>
      </div>

      <AdminTableToolbar
        keyword={keyword}
        onKeywordChange={setKeyword}
        onSearch={handleSearch}
        placeholder="搜索回调名称"
        categoryId={categoryId}
        categories={categories}
        onCategoryChange={handleCategoryChange}
        convertToTreeData={convertToTreeData}
        status={status}
        onStatusChange={handleStatusChange}
      />

      <Spin spinning={loading}>
        {callbackList.length > 0 ? (
          <>
            <div className="table-wrapper">
              <Table
                columns={columns}
                dataSource={callbackList}
                rowKey="id"
                pagination={false}
              />
            </div>
            <div style={{ marginTop: 16, textAlign: 'right' }}>
              <Pagination
                total={pagination.total}
                current={pagination.curPage}
                pageSize={pagination.pageSize}
                pageSizeOptions={PAGE_SIZE_OPTIONS}
                showSizeChanger
                showQuickJumper
                showTotal={(total) => `共 ${total} 条`}
                onChange={handlePageChange}
              />
            </div>
          </>
        ) : (
          <Empty description="暂无回调数据" />
        )}
      </Spin>

      <CallbackRegister
        visible={modalVisible}
        callback={currentItem}
        mode={mode}
        onSuccess={handleSuccess}
        onCancel={closeModal}
      />
    </div>
  );
}

export default CallbackList;
