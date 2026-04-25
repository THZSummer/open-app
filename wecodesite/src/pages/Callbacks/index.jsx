import React, { useState, useEffect } from 'react';
import { Button, Table, Pagination, Tag, message } from 'antd';
import { fetchAppCallbacks, remindApproval, deleteCallback, withdrawApproval, subscribeCallbacks } from './thunk';
import CallbackDrawer from './CallbackDrawer';
import CallbackConfigDrawer from './CallbackConfigDrawer';
import ApprovalAddressModal from '../../components/ApprovalAddressModal/ApprovalAddressModal';
import DeleteConfirmModal from '../../components/DeleteConfirmModal/DeleteConfirmModal';
import { SUBSCRIPTION_STATUS, PAGE_SIZE_OPTIONS, INIT_PAGECONFIG } from '../../utils/constants';
import { queryParams, openUrl } from '../../utils/common';
import { getCallbackColumns } from './constants';
import './Callbacks.m.less';

function Callbacks() {
  const appId = queryParams('appId');
  const [callbacks, setCallbacks] = useState([]);
  const [loading, setLoading] = useState(false);
  const [pagination, setPagination] = useState(INIT_PAGECONFIG);
  const [drawerOpen, setDrawerOpen] = useState(false);
  const [configDrawerOpen, setConfigDrawerOpen] = useState(false);
  const [editingCallback, setEditingCallback] = useState(null);
  const [approvalModalOpen, setApprovalModalOpen] = useState(false);
  const [currentApprovalInfo, setCurrentApprovalInfo] = useState({});
  const [deleteModalOpen, setDeleteModalOpen] = useState(false);
  const [currentDeleteId, setCurrentDeleteId] = useState(null);
  const [deleteLoading, setDeleteLoading] = useState(false);
  const [subscribeLoading, setSubscribeLoading] = useState(false);

  /**
   * 加载回调列表
   */
  const loadCallbacks = async (page = 1, size = pagination.pageSize) => {
    if (!appId) return;
    
    setLoading(true);
    try {
      const result = await fetchAppCallbacks(appId, { curPage: page, pageSize: size });
      if (result && result.code === '200') {
        setCallbacks(result.data || []);
        setPagination(prev => ({ ...prev, total: result.page?.total || 0, curPage: page, pageSize: size }));
      } else {
        message.error(result?.message || '加载回调列表失败');
      }
    } catch (error) {
      message.error('加载回调列表失败');
    } finally {
      setLoading(false);
    }
  };

  /**
   * 组件挂载和 appId 变化时加载数据
   */
  useEffect(() => {
    if (appId) {
      loadCallbacks();
    }
  }, [appId]);

  const handleAddCallback = async (selectedCallbacks) => {
    if (!appId) return;
    
    setSubscribeLoading(true);
    try {
      const permissionIds = selectedCallbacks
        .filter(c => c.id)
        .map(c => c.id);
      
      if (permissionIds.length === 0) {
        message.warning('没有可订阅的权限');
        setSubscribeLoading(false);
        return;
      }
      
      const res = await subscribeCallbacks(appId, { permissionIds });
      if (res && res.code === '200') {
        message.success('申请已提交');
        loadCallbacks(1, INIT_PAGECONFIG.pageSize);
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
      const res = await withdrawApproval(record.id);
      if (res && res.code === '200') {
        message.success('已撤回');
        loadCallbacks();
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
      const res = await deleteCallback(currentDeleteId);
      if (res && res.code === '200') {
        message.success('删除成功');
        setDeleteModalOpen(false);
        loadCallbacks();
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
    loadCallbacks(page, size);
  };

  const columns = getCallbackColumns({
    handleOpenDoc,
    handleEdit,
    handleCopyApprovalAddress,
    handleWithdraw,
    handleDeleteClick,
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
        appId={appId}
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
