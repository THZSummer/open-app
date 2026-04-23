import React, { useState, useEffect, useCallback } from 'react';
import { useNavigate } from 'react-router-dom';
import { Button, Table, Pagination, Tag, message } from 'antd';
import { fetchAppCallbacks, remindApproval, deleteCallback, withdrawApproval, subscribeCallbacks } from './thunk';
import CallbackDrawer from './CallbackDrawer';
import CallbackConfigDrawer from './CallbackConfigDrawer';
import ApprovalAddressModal from '../../components/ApprovalAddressModal/ApprovalAddressModal';
import DeleteConfirmModal from '../../components/DeleteConfirmModal/DeleteConfirmModal';
import { SUBSCRIPTION_STATUS, CALLBACK_CHANNEL_TYPE } from '../../utils/constants';
import './Callbacks.m.less';

function getStatusTag(status) {
  const { text, color } = SUBSCRIPTION_STATUS[status] || { text: '未知', color: 'default' };
  return <Tag color={color}>{text}</Tag>;
}

function Callbacks() {
  const navigate = useNavigate();
  const [callbacks, setCallbacks] = useState([]);
  const [total, setTotal] = useState(0);
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
  const [subscribeLoading, setSubscribeLoading] = useState(false);

  const loadCallbacks = useCallback(async (page = currentPage, size = pageSize) => {
    setLoading(true);
    try {
      const result = await fetchAppCallbacks('10', { curPage: page, pageSize: size });
      setCallbacks(result.data || []);
      setTotal(result.page?.total || 0);
    } catch (error) {
      message.error('加载回调列表失败');
    } finally {
      setLoading(false);
    }
  }, [currentPage, pageSize]);

  useEffect(() => {
    loadCallbacks();
  }, [loadCallbacks]);

  const handleAddCallback = async (selectedCallbacks) => {
    setSubscribeLoading(true);
    try {
      const permissionIds = selectedCallbacks.map(c => c.id);
      await subscribeCallbacks('10', { permissionIds });
      message.success('申请已提交');
      loadCallbacks(1, pageSize);
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
    setEditingCallback(record);
    setConfigDrawerOpen(true);
  };

  const handleSaveCallback = async (updatedCallback) => {
    loadCallbacks();
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
    try {
      await withdrawApproval(record.id);
      message.success('已撤回');
      loadCallbacks();
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
      await deleteCallback(currentDeleteId);
      message.success('删除成功');
      setDeleteModalOpen(false);
      loadCallbacks();
    } catch (error) {
      message.error('删除失败');
    } finally {
      setDeleteLoading(false);
    }
  };

  const handlePageChange = (page, size) => {
    setCurrentPage(page);
    setPageSize(size);
    loadCallbacks(page, size);
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
        <Button type="primary" onClick={handleOpenDrawer} style={{ justifyContent: 'center', borderRadius: 6 }}>添加回调</Button>
      </div>
      <Table
        columns={columns}
        dataSource={callbacks}
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

      <CallbackDrawer
        open={drawerOpen}
        onClose={handleCloseDrawer}
        onConfirm={handleAddCallback}
        selectedCallbacks={callbacks}
        subscribeLoading={subscribeLoading}
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
