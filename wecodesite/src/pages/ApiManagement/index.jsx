import React, { useEffect } from 'react';
import { Button } from 'antd';
import { useSubscriptionList } from '../../hooks/useSubscriptionList';
import SubscriptionTable from '../../components/SubscriptionTable/SubscriptionTable';
import ApprovalAddressModal from '../../components/ApprovalAddressModal/ApprovalAddressModal';
import DeleteConfirmModal from '../../components/DeleteConfirmModal/DeleteConfirmModal';
import ApiPermissionDrawer from './ApiPermissionDrawer';
import { fetchAppApis, subscribeApis, withdrawApiApplication, deleteApiSubscription } from './thunk';
import { getApiManagementColumns } from './constants';
import { openUrl, queryParams } from '../../utils/common';
import './ApiManagement.m.less';

function ApiManagement() {
  const appId = queryParams('appId');

  const {
    data: apis,
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
    fetchList: fetchAppApis,
    subscribe: subscribeApis,
    withdraw: withdrawApiApplication,
    deleteItem: deleteApiSubscription,
  });

  useEffect(() => {
    if (appId) {
      loadData();
    }
  }, [appId, loadData]);

  const handleOpenDoc = (url) => {
    openUrl(url);
  };

  const columns = getApiManagementColumns({
    handleOpenDoc,
    handleCopyApprovalAddress,
    handleWithdraw,
    handleDelete: handleDeleteClick,
  });

  return (
    <div className="api-management">
      <div className="page-header">
        <div className="page-header-left">
          <h4 className="page-title">API管理</h4>
          <span className="page-desc">管理应用接口，配置API权限和调用参数</span>
        </div>
        <Button type="primary" onClick={openDrawer} style={{ justifyContent: 'center', borderRadius: 6 }}>添加API</Button>
      </div>

      <SubscriptionTable
        columns={columns}
        dataSource={apis}
        loading={loading}
        pagination={pagination}
        onPageChange={handlePageChange}
      />

      <ApiPermissionDrawer
        open={drawerOpen}
        onClose={closeDrawer}
        onConfirm={handleSubscribe}
        appId={appId}
      />

      <ApprovalAddressModal
        open={approvalModalOpen}
        onClose={closeApprovalModal}
        approver={currentApprovalInfo.approver}
        approvalUrl={currentApprovalInfo.approvalUrl}
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

export default ApiManagement;
