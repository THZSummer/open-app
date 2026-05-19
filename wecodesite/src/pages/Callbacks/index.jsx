import React, { useEffect, useState } from 'react';
import { Button, message } from 'antd';
import CallbackDrawer from './CallbackDrawer';
import './Callbacks.m.less';
import { getCallbackColumns } from './constants';
import { queryParams, openUrl, getSecondModalInfo } from '../../utils/common';
import CallbackConfigDrawer from './CallbackConfigDrawer';
import { useSubscriptionList } from '../../hooks/useSubscriptionList';
import SubscriptionTable from '../../components/SubscriptionTable/SubscriptionTable';
import ApprovalAddressModal from '../../components/ApprovalAddressModal/ApprovalAddressModal';
import DeleteConfirmModal from '../../components/DeleteConfirmModal/DeleteConfirmModal';
import { fetchAppCallbacks, deleteAppCallbackSubscription, withdrawApproval, subscribeCallbacks } from './thunk';

/**
 * 回调配置管理页面
 */
function Callbacks() {
  const appId = queryParams('appId');

  /**
   * 编辑抽屉状态
   */
  const [configDrawerOpen, setConfigDrawerOpen] = useState(false);
  const [editingCallback, setEditingCallback] = useState(null);

  const [pageDocUrl, setPageDocUlr] = useState('');

  const {
    data: callbacks,
    loading,
    pagination,
    drawerOpen,
    deleteLoading,
    deleteModalOpen,
    subscribeLoading,
    approvalModalOpen,
    currentApprovalInfo,
    revokePending,
    revokeVisible,
    loadData,
    openDrawer,
    closeDrawer,
    handleWithdraw,
    handleSubscribe,
    handlePageChange,
    closeDeleteModal,
    handleDelete,
    handleConfirmWithdraw,
    closeWithdrawModal,
    closeApprovalModal,
    handleConfirmDelete,
    handleCopyApprovalAddress,
  } = useSubscriptionList(appId, {
    fetchList: fetchAppCallbacks,
    subscribe: subscribeCallbacks,
    deleteItem: deleteAppCallbackSubscription,
    withdraw: withdrawApproval,
  });

  useEffect(() => {
    if (appId) {
      loadData();
    }
  }, [appId, loadData]);

  const handleSaveCallback = async () => {
    loadData();
  };

  const handleEdit = (record) => {
    setEditingCallback(record);
    setConfigDrawerOpen(true);
  };

  const handleOpenDoc = (url) => {
    openUrl(url);
  };

  const handleCloseConfigDrawer = () => {
    setConfigDrawerOpen(false);
    setEditingCallback(null);
  }

  const columns = getCallbackColumns({
    handleOpenDoc,
    handleEdit,
    handleCopyApprovalAddress,
    handleWithdraw,
    handleDelete,
  });

  return (
    <div className="callbacks">
      <div className="page-header">
        <div className="page-header-left">
          <h4 className="page-title">回调配置</h4>
          <span className="page-desc">
            配置API回调地址
            <a onClick={() => navigate('/callbacks-docs')} style={{ marginLeft: 4, cursor: 'pointer', color: '#1677ff' }}>了解更多</a>
          </span>
        </div>
        <Button type="primary" onClick={openDrawer} style={{ justifyContent: 'center', borderRadius: 6 }}>添加回调</Button>
      </div>

      <CallbackDrawer
        appId={appId}
        open={drawerOpen}
        onClose={closeDrawer}
        onConfirm={handleSubscribe}
        selectedCallbacks={callbacks}
        subscribeLoading={subscribeLoading}
      />

      <SubscriptionTable
        columns={columns}
        dataSource={callbacks}
        loading={loading}
        pagination={pagination}
        onPageChange={handlePageChange}
      />

      <ApprovalAddressModal
        open={approvalModalOpen}
        onClose={closeApprovalModal}
        approver={currentApprovalInfo.approver}
        approvalUrl={currentApprovalInfo.approvalUrl}
        appId={appId}
      />

      <CallbackConfigDrawer
        open={configDrawerOpen}
        onClose={handleCloseConfigDrawer}
        onSave={handleSaveCallback}
        callback={editingCallback}
      />

      <DeleteConfirmModal
        open={deleteModalOpen}
        onClose={closeDeleteModal}
        onConfirm={handleConfirmDelete}
        loading={deleteLoading}
        modalInfo={getSecondModalInfo('回调', 'delete', false)}
      />

      <DeleteConfirmModal
        open={revokeVisible}
        onClose={closeWithdrawModal}
        onConfirm={handleConfirmWithdraw}
        modalInfo={getSecondModalInfo('回调', 'withdraw', false)}
        loading={revokePending}
      />
    </div>
  );
}

export default Callbacks;
