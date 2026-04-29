import React, { useEffect, useState } from 'react';
import { Button } from 'antd';
import { useSubscriptionList } from '../../hooks/useSubscriptionList';
import SubscriptionTable from '../../components/SubscriptionTable/SubscriptionTable';
import ApprovalAddressModal from '../../components/ApprovalAddressModal/ApprovalAddressModal';
import DeleteConfirmModal from '../../components/DeleteConfirmModal/DeleteConfirmModal';
import CallbackDrawer from './CallbackDrawer';
import CallbackConfigDrawer from './CallbackConfigDrawer';
import { fetchAppCallbacks, deleteAppCallbackSubscription, withdrawApproval, subscribeCallbacks } from './thunk';
import { getCallbackColumns } from './constants';
import { queryParams, openUrl } from '../../utils/common';
import './Callbacks.m.less';

function Callbacks() {
  const appId = queryParams('appId');
  const [configDrawerOpen, setConfigDrawerOpen] = useState(false);
  const [editingCallback, setEditingCallback] = useState(null);

  const {
    data: callbacks,
    loading,
    pagination,
    drawerOpen,
    subscribeLoading,
    approvalModalOpen,
    currentApprovalInfo,
    deleteModalOpen,
    deleteLoading,
    loadData,
    handlePageChange,
    openDrawer,
    closeDrawer,
    handleSubscribe,
    handleCopyApprovalAddress,
    handleWithdraw,
    handleDeleteClick,
    handleConfirmDelete,
    closeApprovalModal,
    closeDeleteModal,
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

  const handleEdit = (record) => {
    setEditingCallback(record);
    setConfigDrawerOpen(true);
  };

  const handleSaveCallback = async () => {
    loadData();
  };

  const handleCloseConfigDrawer = () => {
    setConfigDrawerOpen(false);
    setEditingCallback(null);
  };

  const handleOpenDoc = (url) => {
    openUrl(url);
  };

  const columns = getCallbackColumns({
    handleOpenDoc,
    handleEdit,
    handleCopyApprovalAddress,
    handleWithdraw,
    handleDeleteClick,
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

      <SubscriptionTable
        columns={columns}
        dataSource={callbacks}
        loading={loading}
        pagination={pagination}
        onPageChange={handlePageChange}
      />

      <CallbackDrawer
        open={drawerOpen}
        onClose={closeDrawer}
        onConfirm={handleSubscribe}
        selectedCallbacks={callbacks}
        subscribeLoading={subscribeLoading}
      />

      <CallbackConfigDrawer
        open={configDrawerOpen}
        onClose={handleCloseConfigDrawer}
        onSave={handleSaveCallback}
        callback={editingCallback}
      />

      <ApprovalAddressModal
        open={approvalModalOpen}
        onClose={closeApprovalModal}
        approver={currentApprovalInfo.approver}
        approvalUrl={currentApprovalInfo.approvalUrl}
        appId={appId}
      />

      <DeleteConfirmModal
        open={deleteModalOpen}
        onClose={closeDeleteModal}
        onConfirm={handleConfirmDelete}
        loading={deleteLoading}
      />
    </div>
  );
}

export default Callbacks;
