import React, { useEffect, useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { Button, Table, Spin, Empty, Pagination, message } from 'antd';
import { fetchEventList, deleteEvent } from './thunk';
import { getEventListColumns } from './constants';
import EventRegister from './EventRegister';
import { useAdminList } from '../../../hooks/useAdminList';
import AdminTableToolbar from '../../../components/AdminTableToolbar/AdminTableToolbar';
import { fetchCategoryTree } from '../Category/thunk';
import SimpleSidebar from '../../../components/SimpleSidebar/SimpleSidebar';
import { PAGE_SIZE_OPTIONS } from '../../../utils/constants';
import { isInAdminWhitelist } from '../../../utils/common';
import DeleteConfirmModal from '../../../components/DeleteConfirmModal/DeleteConfirmModal';
import './EventList.m.less';

/**
 * 事件列表管理页
 */
function EventList() {
  const navigate = useNavigate();

  /**
   * 删除弹窗状态管理
   */
  const [showDeleteDialog, setShowDeleteDialog] = useState(false);
  const [deletingId, setDeletingId] = useState(null);
  const [isDeleting, setIsDeleting] = useState(false);

  useEffect(() => {
    checkPermission();
  }, []);

  /**
   * 权限校验
   */
  const checkPermission = async () => {
    const allowed = await isInAdminWhitelist();
    if (!allowed) {
      navigate('/apps');
    }
  };

  const {
    loadData,
    loadCategories,
    data: eventData,
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
    handleDelete,
    handleSuccess,
    modalVisible,
    currentItem,
    mode,
    closeModal,
    setKeyword,
  } = useAdminList({
    fetchList: fetchEventList,
    deleteItem: deleteEvent,
    fetchCategories: fetchCategoryTree,
  });

  /**
   * 删除操作处理
   */
  const initiateDelete = (id) => {
    setDeletingId(id);
    setShowDeleteDialog(true);
  };

  const proceedDelete = async () => {
    if (!deletingId) return;

    setIsDeleting(true);
    const response = await deleteEvent(deletingId);

    if (response && response.code === '200') {
      message.success('删除成功');
      setShowDeleteDialog(false);
      setDeletingId(null);
      loadData();
    } else {
      message.error(response?.message || '删除失败');
    }

    setIsDeleting(false);
  };

  const abortDelete = () => {
    setShowDeleteDialog(false);
    setDeletingId(null);
  };

  useEffect(() => {
    loadCategories();
    loadData();
  }, [loadCategories, loadData]);

  const columns = getEventListColumns({
    handleView,
    handleEdit,
    handleDelete: initiateDelete,
  });

  return (
    <div style={{ display: 'flex', height: '100vh', overflow: 'hidden' }}>
      <SimpleSidebar />
      <div style={{ flex: 1, overflow: 'auto' }}>
        <div className="event-list">
          <div className="page-header">
            <div className="page-header-left">
              <h4 className="page-title">事件管理</h4>
              <span className="page-desc">管理事件定义，配置事件订阅</span>
            </div>
            <Button type="primary" onClick={handleAdd} style={{ justifyContent: 'center', borderRadius: 6 }}>
              注册事件
            </Button>
          </div>

          <AdminTableToolbar
            keyword={keyword}
            onKeywordChange={setKeyword}
            onSearch={handleSearch}
            placeholder="搜索事件名称"
            categoryId={categoryId}
            categories={categories}
            onCategoryChange={handleCategoryChange}
            status={status}
            onStatusChange={handleStatusChange}
          />

          <Spin spinning={loading}>
            {eventData.length > 0 ? (
              <>
                <div className="table-wrapper">
                  <Table
                    columns={columns}
                    dataSource={eventData}
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
              <Empty description="暂无事件数据" />
            )}
          </Spin>

          <EventRegister
            visible={modalVisible}
            event={currentItem}
            mode={mode}
            onSuccess={handleSuccess}
            onCancel={closeModal}
          />

          <DeleteConfirmModal
            open={showDeleteDialog}
            onClose={abortDelete}
            onConfirm={proceedDelete}
            type="delete"
            title="确认删除事件"
            content="删除后无法恢复，确定要删除这个事件吗？"
            loading={isDeleting}
          />
        </div>
      </div>
    </div>
  );
}

export default EventList;
