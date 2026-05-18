import React, { useEffect, useState } from 'react';
import { Button, message } from 'antd';
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

/**
 * 回调配置管理页面
 */
function Callbacks() {
  const appId = queryParams('appId');

  /**
   * 编辑抽屉状态
   */
  const [editDrawerVisible, setEditDrawerVisible] = useState(false);
  const [editTarget, setEditTarget] = useState(null);

  /**
   * 撤回弹窗状态
   */
  const [withdrawVisible, setWithdrawVisible] = useState(false);
  const [withdrawData, setWithdrawData] = useState(null);
  const [withdrawPending, setWithdrawPending] = useState(false);

  const {
    data: callbackRecords,
    loading,
    pagination,
    drawerOpen,
    deleteLoading,
    deleteModalOpen,
    subscribeLoading,
    approvalModalOpen,
    currentApprovalInfo,
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

  /**
   * 撤回处理
   */
  const showWithdrawModal = (record) => {
    setWithdrawData(record);
    setWithdrawVisible(true);
  };

  const processWithdraw = async () => {
    if (!withdrawData) return;

    setWithdrawPending(true);
    const result = await withdrawApproval(appId, withdrawData.id);

    if (result && result.code === '200') {
      message.success('已撤回');
      setWithdrawVisible(false);
      setWithdrawData(null);
      loadData();
    } else {
      message.error(result?.messageZh || result?.message || '撤回失败');
    }

    setWithdrawPending(false);
  };

  const hideWithdrawModal = () => {
    setWithdrawVisible(false);
    setWithdrawData(null);
  };

  /**
   * 编辑处理
   */
  const showEditDrawer = (record) => {
    setEditTarget(record);
    setEditDrawerVisible(true);
  };

  const refreshData = async () => {
    loadData();
  };

  const hideEditDrawer = () => {
    setEditDrawerVisible(false);
    setEditTarget(null);
  };

  const viewDocument = (url) => {
    openUrl(url);
  };

  const columns = getCallbackColumns({
    onDocLink: viewDocument,
    onEditItem: showEditDrawer,
    onCopyAddr: handleCopyApprovalAddress,
    onWithdraw: showWithdrawModal,
    onDeleteItem: handleDeleteClick,
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
        dataSource={callbackRecords}
        loading={loading}
        pagination={pagination}
        onPageChange={handlePageChange}
      />

      <CallbackDrawer
        open={drawerOpen}
        onClose={closeDrawer}
        onConfirm={handleSubscribe}
        selectedCallbacks={callbackRecords}
        subscribeLoading={subscribeLoading}
      />

      <CallbackConfigDrawer
        open={editDrawerVisible}
        onClose={hideEditDrawer}
        onSave={refreshData}
        callback={editTarget}
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

      <DeleteConfirmModal
        open={withdrawVisible}
        onClose={hideWithdrawModal}
        onConfirm={processWithdraw}
        type="withdraw"
        title="确认撤回订阅"
        content="撤回后将无法恢复，确定要撤回这个回调订阅吗？"
        loading={withdrawPending}
      />
    </div>
  );
}

export default Callbacks;
