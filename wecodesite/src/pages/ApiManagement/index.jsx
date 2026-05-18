import React, { useEffect, useState } from 'react';
import { Button, message } from 'antd';
import { useSubscriptionList } from '../../hooks/useSubscriptionList';
import SubscriptionTable from '../../components/SubscriptionTable/SubscriptionTable';
import ApprovalAddressModal from '../../components/ApprovalAddressModal/ApprovalAddressModal';
import DeleteConfirmModal from '../../components/DeleteConfirmModal/DeleteConfirmModal';
import ApiPermissionDrawer from './ApiPermissionDrawer';
import { fetchAppApis, subscribeApis, withdrawApiApplication, deleteApiSubscription } from './thunk';
import { getApiManagementColumns } from './constants';
import { openUrl, queryParams } from '../../utils/common';
import './ApiManagement.m.less';

/**
 * API权限管理页面
 */
function ApiManagement() {
  const appId = queryParams('appId');

  /**
   * 撤回弹窗状态
   */
  const [revokeVisible, setRevokeVisible] = useState(false);
  const [revokeRecord, setRevokeRecord] = useState(null);
  const [revokePending, setRevokePending] = useState(false);

  const {
    data: apiSubscriptions,
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

  /**
   * 撤回申请处理
   */
  const openRevokeConfirm = (record) => {
    setRevokeRecord(record);
    setRevokeVisible(true);
  };

  const confirmRevoke = async () => {
    if (!revokeRecord) return;

    setRevokePending(true);
    const result = await withdrawApiApplication(appId, revokeRecord.id);

    if (result && result.code === '200') {
      message.success('已撤回');
      setRevokeVisible(false);
      setRevokeRecord(null);
      loadData();
    } else {
      message.error(result?.messageZh || result?.message || '撤回失败');
    }

    setRevokePending(false);
  };

  const closeRevokeDialog = () => {
    setRevokeVisible(false);
    setRevokeRecord(null);
  };

  const openDoc = (url) => {
    openUrl(url);
  };

  const columns = getApiManagementColumns({
    onViewDoc: openDoc,
    onCopyUrl: handleCopyApprovalAddress,
    onRevoke: openRevokeConfirm,
    onRemove: handleDeleteClick,
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
        dataSource={apiSubscriptions}
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

      <DeleteConfirmModal
        open={revokeVisible}
        onClose={closeRevokeDialog}
        onConfirm={confirmRevoke}
        type="withdraw"
        title="确认撤回申请"
        content="撤回后将无法恢复，确定要撤回这个API申请吗？"
        loading={revokePending}
      />
    </div>
  );
}

export default ApiManagement;
