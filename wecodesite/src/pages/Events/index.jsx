import React, { useEffect, useState } from 'react';
import { Button } from 'antd';
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

function Events() {
  const appId = queryParams('appId');
  const [subscriptionDrawerOpen, setSubscriptionDrawerOpen] = useState(false);
  const [editingEvent, setEditingEvent] = useState(null);

  const {
    data: events,
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

  const handleEdit = (record) => {
    setEditingEvent(record);
    setSubscriptionDrawerOpen(true);
  };

  const handleSaveSubscription = async () => {
    loadData();
  };

  const handleCloseSubscriptionDrawer = () => {
    setSubscriptionDrawerOpen(false);
    setEditingEvent(null);
  };

  const handleOpenDoc = (url) => {
    openUrl(url);
  };

  const columns = getEventColumns({
    handleOpenDoc,
    handleEdit,
    handleCopyApprovalAddress,
    handleWithdraw,
    handleDeleteClick,
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
        dataSource={events}
        loading={loading}
        pagination={pagination}
        onPageChange={handlePageChange}
      />

      <EventDrawer
        open={drawerOpen}
        onClose={closeDrawer}
        onConfirm={handleSubscribe}
        selectedEvents={events}
        subscribeLoading={subscribeLoading}
        appId={appId}
      />

      <EventSubscriptionDrawer
        open={subscriptionDrawerOpen}
        onClose={handleCloseSubscriptionDrawer}
        onSave={handleSaveSubscription}
        event={editingEvent}
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

export default Events;
