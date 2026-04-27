import React, { useEffect } from 'react';
import { Button, Table, Spin, Empty, Pagination } from 'antd';
import { useAdminList } from '../../../hooks/useAdminList';
import AdminTableToolbar from '../../../components/AdminTableToolbar/AdminTableToolbar';
import { fetchApiList, deleteApi } from './thunk';
import { fetchCategoryTree } from '../Category/thunk';
import ApiRegister from './ApiRegister';
import { getApiListColumns } from './constants';
import { PAGE_SIZE_OPTIONS } from '../../../utils/constants';
import './ApiList.m.less';

function ApiList() {
  const {
    data: apiList,
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
    fetchList: fetchApiList,
    fetchCategories: fetchCategoryTree,
    deleteItem: deleteApi,
  });

  useEffect(() => {
    loadCategories();
    loadData();
  }, [loadCategories, loadData]);

  const columns = getApiListColumns({
    handleView,
    handleEdit,
    handleDelete,
  });

  return (
    <div className="api-list">
      <div className="page-header">
        <div className="page-header-left">
          <h4 className="page-title">API管理</h4>
          <span className="page-desc">管理API接口，配置API权限</span>
        </div>
        <Button type="primary" onClick={handleAdd} style={{ justifyContent: 'center', borderRadius: 6 }}>
          注册API
        </Button>
      </div>

      <AdminTableToolbar
        keyword={keyword}
        onKeywordChange={setKeyword}
        onSearch={handleSearch}
        placeholder="搜索API名称"
        categoryId={categoryId}
        categories={categories}
        onCategoryChange={handleCategoryChange}
        convertToTreeData={convertToTreeData}
        status={status}
        onStatusChange={handleStatusChange}
      />

      <Spin spinning={loading}>
        {apiList.length > 0 ? (
          <>
            <div className="table-wrapper">
              <Table
                columns={columns}
                dataSource={apiList}
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
          <Empty description="暂无API数据" />
        )}
      </Spin>

      <ApiRegister
        visible={modalVisible}
        api={currentItem}
        mode={mode}
        onSuccess={handleSuccess}
        onCancel={closeModal}
      />
    </div>
  );
}

export default ApiList;
