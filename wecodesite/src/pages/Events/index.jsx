import React, { useEffect, useState } from 'react';
import { Button, message } from 'antd';
import { useSubscriptionList } from '../../hooks/useSubscriptionList';
import SubscriptionTable from '../../components/SubscriptionTable/SubscriptionTable';
import ApprovalAddressModal from '../../components/ApprovalAddressModal/ApprovalAddressModal';
import DeleteConfirmModal from '../../components/DeleteConfirmModal/DeleteConfirmModal';
import EventDrawer from './EventDrawer';
import EventSubscriptionDrawer from './EventSubscriptionDrawer';
import { fetchAppEvents, deleteAppEventSubscription, withdrawApproval, subscribeEvents } from './thunk';
import { getEventColumns } from './constants';
import { queryParams, openUrl } from '../../utils/common';
import './Events.m.less';

/**
 * 事件订阅管理页面
 */
function Events() {
  const appId = queryParams('appId');

  /**
   * 订阅抽屉状态
   */
  const [subscriptionDrawerOpen, setSubscriptionDrawerOpen] = useState(false);
  const [subscriptionTarget, setSubscriptionTarget] = useState(null);

  /**
   * 撤回弹窗状态
   */
  const [cancelModalVisible, setCancelModalVisible] = useState(false);
  const [cancelItem, setCancelItem] = useState(null);
  const [cancelInProgress, setCancelInProgress] = useState(false);

  const {
    data: eventSubscriptions,
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
    fetchList: fetchAppEvents,
    subscribe: subscribeEvents,
    deleteItem: deleteAppEventSubscription,
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
  const requestCancel = (record) => {
    setCancelItem(record);
    setCancelModalVisible(true);
  };

  const executeCancel = async () => {
    if (!cancelItem) return;

    setCancelInProgress(true);
    const result = await withdrawApproval(appId, cancelItem.id);

    if (result && result.code === '200') {
      message.success('已撤回');
      setCancelModalVisible(false);
      setCancelItem(null);
      loadData();
    } else {
      message.error(result?.messageZh || result?.message || '撤回失败');
    }

    setCancelInProgress(false);
  };

  const dismissCancel = () => {
    setCancelModalVisible(false);
    setCancelItem(null);
  };

  /**
   * 编辑处理
   */
  const openSubscriptionDrawer = (record) => {
    setSubscriptionTarget(record);
    setSubscriptionDrawerOpen(true);
  };

  const reloadSubscriptions = async () => {
    loadData();
  };

  const closeSubscriptionDrawer = () => {
    setSubscriptionDrawerOpen(false);
    setSubscriptionTarget(null);
  };

  const openDocLink = (url) => {
    openUrl(url);
  };

  const columns = getEventColumns({
    openDoc: openDocLink,
    modifyItem: openSubscriptionDrawer,
    copyApproval: handleCopyApprovalAddress,
    cancelSubscription: requestCancel,
    removeItem: handleDeleteClick,
  });

  return (
    <div className="events">
      <div className="page-header">
        <div className="page-header-left">
          <h4 className="page-title">事件配置</h4>
          <span className="page-desc">
            配置事件订阅和回调地址
            <a onClick={() => navigate('/events-docs')} style={{ marginLeft: 4, cursor: 'pointer', color: '#1677ff' }}>了解更多</a>
          </span>
        </div>
        <Button type="primary" onClick={openDrawer} style={{ justifyContent: 'center', borderRadius: 6 }}>添加事件</Button>
      </div>

      <SubscriptionTable
        columns={columns}
        dataSource={eventSubscriptions}
        loading={loading}
        pagination={pagination}
        onPageChange={handlePageChange}
      />

      <EventDrawer
        open={drawerOpen}
        onClose={closeDrawer}
        onConfirm={handleSubscribe}
        selectedEvents={eventSubscriptions}
        subscribeLoading={subscribeLoading}
        appId={appId}
      />

      <EventSubscriptionDrawer
        open={subscriptionDrawerOpen}
        onClose={closeSubscriptionDrawer}
        onSave={reloadSubscriptions}
        event={subscriptionTarget}
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
        open={cancelModalVisible}
        onClose={dismissCancel}
        onConfirm={executeCancel}
        type="withdraw"
        title="确认撤回订阅"
        content="撤回后将无法恢复，确定要撤回这个事件订阅吗？"
        loading={cancelInProgress}
      />
    </div>
  );
}

export default Events;
