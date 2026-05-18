import React, { useEffect, useState } from 'react';
import { Button, Table, Spin, Empty, Pagination, message } from 'antd';
import { useNavigate } from 'react-router-dom';
import { fetchCallbackList, deleteCallback } from './thunk';
import CallbackRegister from './CallbackRegister';
import { getCallbackListColumns } from './constants';
import { useAdminList } from '../../../hooks/useAdminList';
import AdminTableToolbar from '../../../components/AdminTableToolbar/AdminTableToolbar';
import { fetchCategoryTree } from '../Category/thunk';
import { PAGE_SIZE_OPTIONS } from '../../../utils/constants';
import { isInAdminWhitelist } from '../../../utils/common';
import SimpleSidebar from '../../../components/SimpleSidebar/SimpleSidebar';
import DeleteConfirmModal from '../../../components/DeleteConfirmModal/DeleteConfirmModal';
import './CallbackList.m.less';

/**
 * 回调管理列表页面
 */
function CallbackList() {
  const navigate = useNavigate();

  /**
   * 删除弹窗相关状态
   */
  const [delDialogVisible, setDelDialogVisible] = useState(false);
  const [delTargetId, setDelTargetId] = useState(null);
  const [delProcessing, setDelProcessing] = useState(false);

  /**
   * 页面初始化
   */
  const pageInit = async () => {
    const hasPermission = await isInAdminWhitelist();
    if (!hasPermission) {
      navigate('/apps');
    }
  };

  useEffect(() => {
    pageInit();
  }, []);

  const {
    data: callbackData,
    loading,
    handleAdd,
    handleView,
    handleEdit,
    handleSuccess,
    setKeyword,
    handleSearch,
    loadData,
    loadCategories,
    pagination,
    handlePageChange,
    modalVisible,
    currentItem,
    mode,
    closeModal,
    keyword,
    categoryId,
    categories,
    status,
    handleCategoryChange,
    handleStatusChange,
  } = useAdminList({
    deleteItem: deleteCallback,
    fetchList: fetchCallbackList,
    fetchCategories: fetchCategoryTree,
  });

  /**
   * 删除相关操作
   */
  const triggerDelete = (id) => {
    setDelTargetId(id);
    setDelDialogVisible(true);
  };

  const executeDelete = async () => {
    if (!delTargetId) return;

    setDelProcessing(true);
    const result = await deleteCallback(delTargetId);

    if (result && result.code === '200') {
      message.success('删除成功');
      setDelDialogVisible(false);
      setDelTargetId(null);
      loadData();
    } else {
      message.error(result?.messageZh || result?.message || '删除失败');
    }

    setDelProcessing(false);
  };

  const dismissDelete = () => {
    setDelDialogVisible(false);
    setDelTargetId(null);
  };

  useEffect(() => {
    loadCategories();
    loadData();
  }, [loadCategories, loadData]);

  const columns = getCallbackListColumns({
    onView: handleView,
    onEdit: handleEdit,
    onDelete: triggerDelete,
  });

  return (
    <div style={{ display: 'flex', height: '100vh', overflow: 'hidden' }}>
      <SimpleSidebar />
      <div style={{ flex: 1, overflow: 'auto' }}>
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
            status={status}
            onStatusChange={handleStatusChange}
          />

          <Spin spinning={loading}>
            {callbackData.length > 0 ? (
              <>
                <div className="table-wrapper">
                  <Table
                    columns={columns}
                    dataSource={callbackData}
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

          <DeleteConfirmModal
            open={delDialogVisible}
            onClose={dismissDelete}
            onConfirm={executeDelete}
            type="delete"
            title="确认删除回调"
            content="删除后无法恢复，确定要删除这个回调吗？"
            loading={delProcessing}
          />
        </div>
      </div>
    </div>
  );
}

export default CallbackList;
