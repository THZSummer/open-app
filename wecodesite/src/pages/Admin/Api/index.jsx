import React, { useEffect } from 'react';
import { Button, Table, Spin, Empty, Pagination } from 'antd';
import { fetchApiList, deleteApi } from './thunk';
import { fetchCategoryTree } from '../Category/thunk';
import { useAdminList } from '../../../hooks/useAdminList';
import AdminTableToolbar from '../../../components/AdminTableToolbar/AdminTableToolbar';
import { ADMIN_API_DELETE_SECOND_MODAL_INFO, getApiListColumns } from './constants';
import { PAGE_SIZE_OPTIONS } from '../../../utils/constants';
import { getSecondModalInfo } from '../../../utils/common';
import { useAdminAccessGuard } from '../../../hooks/useAdminAccessGuard';
import ApiRegister from './ApiRegister';
import SimpleSidebar from '../../../components/SimpleSidebar/SimpleSidebar';
import DeleteConfirmModal from '../../../components/DeleteConfirmModal/DeleteConfirmModal';
import './ApiList.m.less';

/**
 * API列表管理页面
 */
function ApiList() {
  useAdminAccessGuard();

  const {
    categories,
    closeModal,
    currentItem,
    data: apiList,
    handleAdd,
    handleCategoryChange,
    handleDelete,
    handleCloseDeleteConfirm,
    handleDeleteClick,
    handleEdit,
    handlePageChange,
    handleSearch,
    handleStatusChange,
    handleSuccess,
    handleView,
    keyword,
    loadCategories,
    loadData,
    loading,
    mode,
    modalVisible,
    removeConfirmVisible,
    pagination,
    setKeyword,
    status,
    categoryId,
  } = useAdminList({
    fetchCategories: fetchCategoryTree,
    deleteItem: deleteApi,
    fetchList: fetchApiList,
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
    <div style={{ display: 'flex', height: '100vh', overflow: 'hidden' }}>
      <SimpleSidebar />
      <div style={{ flex: 1, overflow: 'auto' }}>
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

          <DeleteConfirmModal
            open={removeConfirmVisible}
            onClose={handleCloseDeleteConfirm}
            onConfirm={handleDeleteClick}
            modalInfo={getSecondModalInfo({
              ...ADMIN_API_DELETE_SECOND_MODAL_INFO,
              objectName: currentItem?.nameCn || currentItem?.nameEn,
            })}
          />
        </div>
      </div>
    </div>
  );
}

export default ApiList;
