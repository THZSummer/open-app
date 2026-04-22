import React, { useState, useEffect } from 'react';
import { useNavigate } from 'react-router-dom';
import { Button, Table, Pagination, Tag } from 'antd';
import { PlusOutlined } from '@ant-design/icons';
import { fetchCallbackList } from './thunk';
import CallbackDrawer from './CallbackDrawer';
import CallbackConfigDrawer from './CallbackConfigDrawer';
import ApprovalAddressModal from '../../components/ApprovalAddressModal/ApprovalAddressModal';
import DeleteConfirmModal from '../../components/DeleteConfirmModal/DeleteConfirmModal';
import { remindApproval, deleteCallback, withdrawApproval } from './thunk';
import { SUBSCRIPTION_STATUS, CALLBACK_CHANNEL_TYPE } from '../../utils/constants';
import './Callbacks.m.less';

function getStatusTag(status) {
  const { text, color } = SUBSCRIPTION_STATUS[status] || { text: '未知', color: 'default' };
  return <Tag color={color}>{text}</Tag>;
}

function Callbacks() {
  const navigate = useNavigate();
  const [callbacks, setCallbacks] = useState([]);
  const [loading, setLoading] = useState(false);
  const [currentPage, setCurrentPage] = useState(1);
  const [pageSize, setPageSize] = useState(10);
  const [drawerOpen, setDrawerOpen] = useState(false);
  const [configDrawerOpen, setConfigDrawerOpen] = useState(false);
  const [editingCallback, setEditingCallback] = useState(null);
  const [approvalModalOpen, setApprovalModalOpen] = useState(false);
  const [currentApprovalInfo, setCurrentApprovalInfo] = useState({});
  const [deleteModalOpen, setDeleteModalOpen] = useState(false);
  const [currentDeleteId, setCurrentDeleteId] = useState(null);
  const [deleteLoading, setDeleteLoading] = useState(false);

  useEffect(() => {
    const loadData = async () => {
      setLoading(true);
      const callbacksData = await fetchCallbackList();
      setCallbacks(callbacksData);
      setLoading(false);
    };
    loadData();
  }, []);

  const handleAddCallback = (selectedCallbacks) => {
    const newCallbacks = selectedCallbacks.map((callback, index) => ({
      ...callback,
      id: String(Date.now() + index),
      status: callback.needReview ? 0 : 1,
      approver: callback.needReview ? {
        userId: 'pending',
        userName: '待分配'
      } : null,
      approvalUrl: callback.needReview ? 'https://approval.example.com/callback/' + (Date.now() + index) : '',
      channelType: 0,
      channelAddress: '',
      authType: 0
    }));
    setCallbacks([...callbacks, ...newCallbacks]);
  };

  const handleOpenDrawer = () => {
    setDrawerOpen(true);
  };

  const handleCloseDrawer = () => {
    setDrawerOpen(false);
  };

  const handleEdit = (record) => {
    setEditingCallback(record);
    setConfigDrawerOpen(true);
  };

  const handleSaveCallback = (updatedCallback) => {
    setCallbacks(callbacks.map(c => c.id === updatedCallback.id ? updatedCallback : c));
  };

  const handleCloseConfigDrawer = () => {
    setConfigDrawerOpen(false);
    setEditingCallback(null);
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
    const data = await fetchCallbackList();
    setCallbacks(data);
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
      await deleteCallback(currentDeleteId);
      setCallbacks(callbacks.filter(c => c.id !== currentDeleteId));
      const data = await fetchCallbackList();
      setCallbacks(data);
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
      title: '回调名称',
      dataIndex: ['permission', 'nameCn'],
      key: 'nameCn',
      render: (text, record) => (
        <div>
          <div>{text}</div>
          <span style={{ fontSize: 12, color: '#8c8c8c' }}>{record.permission?.scope}</span>
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
      title: '通道类型',
      dataIndex: 'channelType',
      key: 'channelType',
      render: (type) => CALLBACK_CHANNEL_TYPE[type] || '-',
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
          <Button type="link" onClick={() => handleOpenDoc(record.callback?.docUrl || record.docUrl)}>查看文档</Button>
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

  const paginatedData = callbacks.slice((currentPage - 1) * pageSize, currentPage * pageSize);

  return (
    <div className="callbacks">
      <h4 className="page-title">回调配置</h4>
      <span className="page-desc">
        配置API回调地址
        <a onClick={() => navigate('/callbacks-docs')} style={{ marginLeft: 4, cursor: 'pointer', color: '#1677ff' }}>了解更多</a>
      </span>

      <div style={{ marginTop: 16 }}>
        <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: 16 }}>
          <strong style={{ fontSize: 16 }}>已添加回调</strong>
          <Button icon={<PlusOutlined />} onClick={handleOpenDrawer}>添加回调</Button>
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
            total={callbacks.length}
            current={currentPage}
            pageSize={pageSize}
            pageSizeOptions={[10, 20, 50]}
            showSizeChanger
            showQuickJumper
            showTotal={(total) => `共 ${total} 条`}
            onChange={handlePageChange}
          />
        </div>
      </div>

      <CallbackDrawer
        open={drawerOpen}
        onClose={handleCloseDrawer}
        onConfirm={handleAddCallback}
        selectedCallbacks={callbacks}
      />

      <CallbackConfigDrawer
        open={configDrawerOpen}
        onClose={handleCloseConfigDrawer}
        onSave={handleSaveCallback}
        callback={editingCallback}
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

export default Callbacks;