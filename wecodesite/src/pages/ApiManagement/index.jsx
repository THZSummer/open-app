import React, { useEffect, useState } from 'react';
import { Button } from 'antd';
import { useSubscriptionList } from '../../hooks/useSubscriptionList';
import SubscriptionTable from '../../components/SubscriptionTable/SubscriptionTable';
import ApprovalAddressModal from '../../components/ApprovalAddressModal/ApprovalAddressModal';
import DeleteConfirmModal from '../../components/DeleteConfirmModal/DeleteConfirmModal';
import ApiPermissionDrawer from './ApiPermissionDrawer';
import { fetchAppApis, subscribeApis, withdrawApiApplication, deleteApiSubscription } from './thunk';
import { getApiManagementColumns } from './constants';
import { getSecondModalInfo, openUrl, queryParams } from '../../utils/common';
import './ApiManagement.m.less';

/**
 * API权限管理页面
 */
function ApiManagement() {
  const appId = queryParams('appId');

  const {
    data: apis,
    loadData,
    openDrawer,
    closeDrawer,
    handleWithdraw,
    handlePageChange,
    handleDelete,
    handleConfirmDelete,
    handleCopyApprovalAddress,
    closeApprovalModal,
    closeDeleteModal,
    handleSubscribe,
    handleConfirmWithdraw,
    closeWithdrawModal,
    revokeVisible,
    revokePending,
    loading,
    pagination,
    drawerOpen,
    deleteModalOpen,
    deleteLoading,
    subscribeLoading,
    approvalModalOpen,
    currentApprovalInfo,
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
    handleWithdraw,
    handleCopyApprovalAddress,
    handleDelete,
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

      <ApiPermissionDrawer
        open={drawerOpen}
        onClose={closeDrawer}
        onConfirm={handleSubscribe}
        appId={appId}
      />

      <DeleteConfirmModal
        open={deleteModalOpen}
        onClose={closeDeleteModal}
        onConfirm={handleConfirmDelete}
        loading={deleteLoading}
        modalInfo={getSecondModalInfo('API', 'delete', false)}
      />

      <ApprovalAddressModal
        open={approvalModalOpen}
        onClose={closeApprovalModal}
        approver={currentApprovalInfo.approver}
        approvalUrl={currentApprovalInfo.approvalUrl}
      />

      <SubscriptionTable
        columns={columns}
        dataSource={apis}
        loading={loading}
        pagination={pagination}
        onPageChange={handlePageChange}
      />

      <DeleteConfirmModal
        open={revokeVisible}
        onClose={closeWithdrawModal}
        onConfirm={handleConfirmWithdraw}
        modalInfo={getSecondModalInfo('API', 'withdraw', false)}
        loading={revokePending}
      />
    </div>
  );
}

export default ApiManagement;
