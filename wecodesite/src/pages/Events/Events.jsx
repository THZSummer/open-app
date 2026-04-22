import React, { useState, useEffect } from 'react';
import { useNavigate } from 'react-router-dom';
import { Button, Table, Pagination, Tag } from 'antd';
import { fetchEventList } from './thunk';
import EventDrawer from './EventDrawer';
import EventSubscriptionDrawer from './EventSubscriptionDrawer';
import ApprovalAddressModal from '../../components/ApprovalAddressModal/ApprovalAddressModal';
import DeleteConfirmModal from '../../components/DeleteConfirmModal/DeleteConfirmModal';
import { remindApproval, deleteEvent, withdrawApproval } from './thunk';
import { SUBSCRIPTION_STATUS, EVENT_CHANNEL_TYPE } from '../../utils/constants';
import './Events.m.less';

function getStatusTag(status) {
  const { text, color } = SUBSCRIPTION_STATUS[status] || { text: '未知', color: 'default' };
  return <Tag color={color}>{text}</Tag>;
}

function Events() {
  const navigate = useNavigate();
  const [events, setEvents] = useState([]);
  const [loading, setLoading] = useState(false);
  const [currentPage, setCurrentPage] = useState(1);
  const [pageSize, setPageSize] = useState(10);
  const [drawerOpen, setDrawerOpen] = useState(false);
  const [subscriptionDrawerOpen, setSubscriptionDrawerOpen] = useState(false);
  const [editingEvent, setEditingEvent] = useState(null);
  const [approvalModalOpen, setApprovalModalOpen] = useState(false);
  const [currentApprovalInfo, setCurrentApprovalInfo] = useState({});
  const [deleteModalOpen, setDeleteModalOpen] = useState(false);
  const [currentDeleteId, setCurrentDeleteId] = useState(null);
  const [deleteLoading, setDeleteLoading] = useState(false);

  useEffect(() => {
    const loadData = async () => {
      setLoading(true);
      const eventsData = await fetchEventList();
      setEvents(eventsData);
      setLoading(false);
    };
    loadData();
  }, []);

  const handleAddEvent = (selectedEvents) => {
    const newEvents = selectedEvents.map((event, index) => ({
      ...event,
      id: String(Date.now() + index),
      status: event.needReview ? 0 : 1,
      approver: event.needReview ? {
        userId: 'pending',
        userName: '待分配'
      } : null,
      approvalUrl: event.needReview ? 'https://approval.example.com/event/' + (Date.now() + index) : '',
      channelType: 0,
      channelAddress: '',
      authType: 0
    }));
    setEvents([...events, ...newEvents]);
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

  const handleSaveSubscription = (updatedEvent) => {
    setEvents(events.map(e => e.id === updatedEvent.id ? updatedEvent : e));
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
    await withdrawApproval(record.id);
    const data = await fetchEventList();
    setEvents(data);
  };

  const handleOpenDoc = (url) => {
    window.open(url, '_blank');
  };

  const handleDeleteClick = (id) => {
    setCurrentDeleteId(id);
    setDeleteModalOpen(true);
  };

  const handleConfirmDelete = async () => {
    setDeleteLoading(true);
    try {
      await deleteEvent(currentDeleteId);
      setEvents(events.filter(e => e.id !== currentDeleteId));
      const data = await fetchEventList();
      setEvents(data);
      setDeleteModalOpen(false);
    } finally {
      setDeleteLoading(false);
    }
  };

  const handlePageChange = (page, size) => {
    setCurrentPage(page);
    setPageSize(size);
  };

  const columns = [
    {
      title: '事件名称',
      dataIndex: ['permission', 'nameCn'],
      key: 'nameCn',
      render: (text, record) => (
        <div>
          <div>{text}</div>
          <span style={{ fontSize: 12, color: '#8c8c8c' }}>{record.event?.topic}</span>
        </div>
      ),
    },
    {
      title: '分类',
      dataIndex: ['category', 'nameCn'],
      key: 'category',
    },
    {
      title: '所需权限',
      dataIndex: ['permission', 'nameCn'],
      key: 'permissionName',
    },
    {
      title: '订阅方式',
      dataIndex: 'channelType',
      key: 'channelType',
      render: (type) => EVENT_CHANNEL_TYPE[type] || '-',
    },
    {
      title: '状态',
      dataIndex: 'status',
      key: 'status',
      render: getStatusTag,
    },
    {
      title: '操作',
      key: 'action',
      render: (_, record) => (
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
      ),
    },
  ];

  const paginatedData = events.slice((currentPage - 1) * pageSize, currentPage * pageSize);

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
        dataSource={paginatedData}
        rowKey="id"
        pagination={false}
        loading={loading}
      />
      <div style={{ marginTop: 16, textAlign: 'right' }}>
        <Pagination
          total={events.length}
          current={currentPage}
          pageSize={pageSize}
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