import React, { useState, useEffect } from 'react';
import { Button, Table, Pagination, Tag, message } from 'antd';
import { fetchAppEvents, remindApproval, deleteEvent, withdrawApproval, subscribeEvents } from './thunk';
import EventDrawer from './EventDrawer';
import EventSubscriptionDrawer from './EventSubscriptionDrawer';
import ApprovalAddressModal from '../../components/ApprovalAddressModal/ApprovalAddressModal';
import DeleteConfirmModal from '../../components/DeleteConfirmModal/DeleteConfirmModal';
import { SUBSCRIPTION_STATUS, EVENT_CHANNEL_TYPE, PAGE_SIZE_OPTIONS, INIT_PAGECONFIG } from '../../utils/constants';
import { queryParams, openUrl } from '../../utils/common';
import { getEventColumns } from './constants';
import './Events.m.less';

function Events() {
  const appId = queryParams('appId');
  const [events, setEvents] = useState([]);
  const [loading, setLoading] = useState(false);
  const [pagination, setPagination] = useState(INIT_PAGECONFIG);
  const [drawerOpen, setDrawerOpen] = useState(false);
  const [subscriptionDrawerOpen, setSubscriptionDrawerOpen] = useState(false);
  const [editingEvent, setEditingEvent] = useState(null);
  const [approvalModalOpen, setApprovalModalOpen] = useState(false);
  const [currentApprovalInfo, setCurrentApprovalInfo] = useState({});
  const [deleteModalOpen, setDeleteModalOpen] = useState(false);
  const [currentDeleteId, setCurrentDeleteId] = useState(null);
  const [deleteLoading, setDeleteLoading] = useState(false);
  const [subscribeLoading, setSubscribeLoading] = useState(false);

  /**
   * 加载事件列表
   */
  const loadEvents = async (page = 1, size = pagination.pageSize) => {
    if (!appId) return;
    
    setLoading(true);
    try {
      const result = await fetchAppEvents(appId, { curPage: page, pageSize: size });
      if (result && result.code === '200') {
        setEvents(result.data || []);
        setPagination(prev => ({ ...prev, total: result.page?.total || 0, curPage: page, pageSize: size }));
      } else {
        message.error(result?.message || '加载事件列表失败');
      }
    } catch (error) {
      message.error('加载事件列表失败');
    } finally {
      setLoading(false);
    }
  };

  /**
   * 组件挂载和 appId 变化时加载数据
   */
  useEffect(() => {
    if (appId) {
      loadEvents();
    }
  }, [appId]);

  const handleAddEvent = async (selectedEvents) => {
    if (!appId) return;
    
    setSubscribeLoading(true);
    try {
      const permissionIds = selectedEvents
        .filter(e => e.permission?.id)
        .map(e => e.permission.id);
      
      if (permissionIds.length === 0) {
        message.warning('没有可订阅的权限');
        setSubscribeLoading(false);
        return;
      }
      
      const res = await subscribeEvents(appId, { permissionIds });
      if (res && res.code === '200') {
        message.success('申请已提交');
        loadEvents(1, INIT_PAGECONFIG.pageSize);
        setDrawerOpen(false);
      } else {
        message.error(res?.message || '申请失败');
      }
    } catch (error) {
      message.error('申请失败');
    } finally {
      setSubscribeLoading(false);
    }
  };

  const handleOpenDrawer = () => {
    setDrawerOpen(true);
  };

  const handleCloseDrawer = () => {
    setDrawerOpen(false);
  };

  const handleEdit = (record) => {
    setEditingEvent(record);
    setSubscriptionDrawerOpen(true);
  };

  const handleSaveSubscription = async (updatedEvent) => {
    loadEvents();
  };

  const handleCloseSubscriptionDrawer = () => {
    setSubscriptionDrawerOpen(false);
    setEditingEvent(null);
  };

  const handleCopyApprovalAddress = (record) => {
    setCurrentApprovalInfo({
      id: record.id,
      approver: record.approver?.userName || '待分配',
      approvalUrl: record.approvalUrl || ''
    });
    setApprovalModalOpen(true);
  };

  const handleWithdraw = async (record) => {
    try {
      const res = await withdrawApproval(record.id);
      if (res && res.code === '200') {
        message.success('已撤回');
        loadEvents();
      } else {
        message.error(res?.message || '撤回失败');
      }
    } catch (error) {
      message.error('撤回失败');
    }
  };

  const handleOpenDoc = (url) => {
    openUrl(url);
  };

  const handleDeleteClick = (id) => {
    setCurrentDeleteId(id);
    setDeleteModalOpen(true);
  };

  const handleConfirmDelete = async () => {
    setDeleteLoading(true);
    try {
      const res = await deleteEvent(currentDeleteId);
      if (res && res.code === '200') {
        message.success('删除成功');
        setDeleteModalOpen(false);
        loadEvents();
      } else {
        message.error(res?.message || '删除失败');
      }
    } catch (error) {
      message.error('删除失败');
    } finally {
      setDeleteLoading(false);
    }
  };

  const handlePageChange = (page, size) => {
    loadEvents(page, size);
  };

  const renderEventName = (text, record) => (
    <div>
      <div>{text}</div>
      <span style={{ fontSize: 12, color: '#8c8c8c' }}>{record.event?.topic}</span>
    </div>
  );

  const renderChannelType = (type) => EVENT_CHANNEL_TYPE[type] || '-';

  const renderStatus = (status) => {
    const { text, color } = SUBSCRIPTION_STATUS[status] || { text: '未知', color: 'default' };
    return <Tag color={color}>{text}</Tag>;
  };

  const renderAction = (_, record) => (
    <div>
      <Button type="link" onClick={() => handleOpenDoc(record.event?.docUrl || record.docUrl)}>查看文档</Button>
      {record.status === 1 && (
        <Button type="link" onClick={() => handleEdit(record)}>编辑</Button>
      )}
      {record.status === 0 && (
        <>
          <Button type="link" onClick={() => handleCopyApprovalAddress(record)}>复制审批地址</Button>
          <Button type="link" onClick={() => handleWithdraw(record)}>撤回审核</Button>
        </>
      )}
      {record.status !== 0 && (
        <Button type="link" danger onClick={() => handleDeleteClick(record.id)}>删除</Button>
      )}
    </div>
  );

  const columns = getEventColumns({
    renderEventName,
    renderChannelType,
    renderStatus,
    renderAction,
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
        <Button type="primary" onClick={handleOpenDrawer} style={{ justifyContent: 'center', borderRadius: 6 }}>添加事件</Button>
      </div>
      <Table
        columns={columns}
        dataSource={events}
        rowKey="id"
        pagination={false}
        loading={loading}
      />
      <div style={{ marginTop: 16, textAlign: 'right' }}>
        <Pagination
          total={pagination.total}
          current={pagination.curPage}
          pageSize={pagination.pageSize}
          pageSizeOptions={[10, 20, 50]}
          showSizeChanger
          showQuickJumper
          showTotal={(total) => `共 ${total} 条`}
          onChange={handlePageChange}
        />
      </div>

      <EventDrawer
        open={drawerOpen}
        onClose={handleCloseDrawer}
        onConfirm={handleAddEvent}
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
        onClose={() => setApprovalModalOpen(false)}
        approver={currentApprovalInfo.approver}
        approvalUrl={currentApprovalInfo.approvalUrl}
        onRemind={() => remindApproval(currentApprovalInfo.id)}
      />

      <DeleteConfirmModal
        open={deleteModalOpen}
        onClose={() => setDeleteModalOpen(false)}
        onConfirm={handleConfirmDelete}
        loading={deleteLoading}
      />
    </div>
  );
}

export default Events;
