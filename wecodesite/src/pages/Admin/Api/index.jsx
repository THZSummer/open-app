import React, { useEffect, useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { Button, Table, Spin, Empty, Pagination, message } from 'antd';
import { fetchApiList, deleteApi } from './thunk';
import { fetchCategoryTree } from '../Category/thunk';
import { useAdminList } from '../../../hooks/useAdminList';
import AdminTableToolbar from '../../../components/AdminTableToolbar/AdminTableToolbar';
import { getApiListColumns } from './constants';
import { PAGE_SIZE_OPTIONS } from '../../../utils/constants';
import { isInAdminWhitelist } from '../../../utils/common';
import ApiRegister from './ApiRegister';
import SimpleSidebar from '../../../components/SimpleSidebar/SimpleSidebar';
import DeleteConfirmModal from '../../../components/DeleteConfirmModal/DeleteConfirmModal';
import './ApiList.m.less';

/**
 * API列表管理页面
 */
function ApiList() {
  const navigate = useNavigate();

  /**
   * 弹窗状态
   */
  const [removeConfirmVisible, setRemoveConfirmVisible] = useState(false);
  const [removeTargetId, setRemoveTargetId] = useState(null);
  const [removePending, setRemovePending] = useState(false);

  /**
   * 初始化权限校验
   */
  const validateAccess = async () => {
    const hasAccess = await isInAdminWhitelist();
    if (!hasAccess) {
      navigate('/apps');
    }
  };

  useEffect(() => {
    validateAccess();
  }, []);

  const {
    loadData,
    loadCategories,
    data: apiItems,
    categories,
    categoryId,
    status,
    keyword,
    loading,
    pagination,
    handleSearch,
    handlePageChange,
    handleCategoryChange,
    handleStatusChange,
    handleAdd,
    handleEdit,
    handleView,
    handleSuccess,
    modalVisible,
    currentItem,
    mode,
    closeModal,
    setKeyword,
  } = useAdminList({
    fetchList: fetchApiList,
    deleteItem: deleteApi,
    fetchCategories: fetchCategoryTree,
  });

  /**
   * 删除确认处理
   */
  const openRemoveConfirm = (id) => {
    setRemoveTargetId(id);
    setRemoveConfirmVisible(true);
  };

  const confirmRemove = async () => {
    if (!removeTargetId) return;

    setRemovePending(true);
    const result = await deleteApi(removeTargetId);

    if (result && result.code === '200') {
      message.success('删除成功');
      setRemoveConfirmVisible(false);
      setRemoveTargetId(null);
      loadData();
    } else {
      message.error(result?.messageZh || result?.message || '删除失败');
    }

    setRemovePending(false);
  };

  const cancelRemove = () => {
    setRemoveConfirmVisible(false);
    setRemoveTargetId(null);
  };

  useEffect(() => {
    loadCategories();
    loadData();
  }, [loadCategories, loadData]);

  const columns = getApiListColumns({
    onView: handleView,
    onEdit: handleEdit,
    onDelete: openRemoveConfirm,
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
            {apiItems.length > 0 ? (
              <>
                <div className="table-wrapper">
                  <Table
                    columns={columns}
                    dataSource={apiItems}
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
            onClose={cancelRemove}
            onConfirm={confirmRemove}
            type="delete"
            title="确认删除API"
            content="删除后无法恢复，确定要删除这个API吗？"
            loading={removePending}
          />
        </div>
      </div>
    </div>
  );
}

export default ApiList;
