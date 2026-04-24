import React, { useState, useEffect, useCallback } from 'react';
import { useNavigate, useSearchParams } from 'react-router-dom';
import { Button, Table, Pagination, Tag, message } from 'antd';
import { fetchAppEvents, remindApproval, deleteEvent, withdrawApproval, subscribeEvents } from './thunk';
import EventDrawer from './EventDrawer';
import EventSubscriptionDrawer from './EventSubscriptionDrawer';
import ApprovalAddressModal from '../../components/ApprovalAddressModal/ApprovalAddressModal';
import DeleteConfirmModal from '../../components/DeleteConfirmModal/DeleteConfirmModal';
import { SUBSCRIPTION_STATUS, EVENT_CHANNEL_TYPE } from '../../utils/constants';
import './Events.m.less';

function getStatusTag(status) {
  const { text, color } = SUBSCRIPTION_STATUS[status] || { text: '未知', color: 'default' };
  return <Tag color={color}>{text}</Tag>;
}

function Events() {
  const navigate = useNavigate();
  const [searchParams] = useSearchParams();
  const appId = searchParams.get('appId');
  const [events, setEvents] = useState([]);
  const [total, setTotal] = useState(0);
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
  const [subscribeLoading, setSubscribeLoading] = useState(false);

  const loadEvents = useCallback(async (page = currentPage, size = pageSize) => {
    if (!appId) return;
    
    setLoading(true);
    try {
      const result = await fetchAppEvents(appId, { curPage: page, pageSize: size });
      setEvents(result.data || []);
      setTotal(result.page?.total || 0);
    } catch (error) {
      message.error('加载事件列表失败');
    } finally {
      setLoading(false);
    }
  }, [appId, currentPage, pageSize]);

  useEffect(() => {
    loadEvents();
  }, [loadEvents]);

  const handleAddEvent = async (selectedEvents) => {
    if (!appId) return;
    
    setSubscribeLoading(true);
    try {
      // 使用 permission.id 作为权限ID（而不是事件ID e.id）
      const permissionIds = selectedEvents
        .filter(e => e.permission?.id)  // 过滤出有权限ID的事件
        .map(e => e.permission.id);
      
      if (permissionIds.length === 0) {
        message.warning('没有可订阅的权限');
        setSubscribeLoading(false);
        return;
      }
      
      await subscribeEvents(appId, { permissionIds });
      message.success('申请已提交');
      loadEvents(1, pageSize);
      setDrawerOpen(false);
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
      await withdrawApproval(record.id);
      message.success('已撤回');
      loadEvents();
    } catch (error) {
      message.error('撤回失败');
    }
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
      message.success('删除成功');
      setDeleteModalOpen(false);
      loadEvents();
    } catch (error) {
      message.error('删除失败');
    } finally {
      setDeleteLoading(false);
    }
  };

  const handlePageChange = (page, size) => {
    setCurrentPage(page);
    setPageSize(size);
    loadEvents(page, size);
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
          total={total}
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
