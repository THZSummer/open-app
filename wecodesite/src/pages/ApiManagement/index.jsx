import React, { useState, useEffect } from 'react';
import { Table, Pagination, Button, message } from 'antd';
import { fetchAppApis, subscribeApis, withdrawApiApplication, remindApproval } from './thunk';
import { fetchAppInfo } from '../BasicInfo/thunk';
import ApiPermissionDrawer from './ApiPermissionDrawer';
import ApprovalAddressModal from '../../components/ApprovalAddressModal/ApprovalAddressModal';
import { PAGE_SIZE_OPTIONS, INIT_PAGECONFIG } from '../../utils/constants';
import { getApiManagementColumns } from './constants';
import { openUrl, queryParams } from '../../utils/common';
import './ApiManagement.m.less';

function ApiManagement() {
  const appId = queryParams('appId');

  const [apis, setApis] = useState([]);
  const [loading, setLoading] = useState(false);
  const [drawerOpen, setDrawerOpen] = useState(false);
  const [appType, setAppType] = useState('business');
  const [approvalModalOpen, setApprovalModalOpen] = useState(false);
  const [currentApprovalInfo, setCurrentApprovalInfo] = useState({});
  const [pagination, setPagination] = useState(INIT_PAGECONFIG);

  const loadAppInfo = async () => {
    const appInfo = await fetchAppInfo(appId);
    if (appInfo && appInfo.eamap) {
      setAppType('business');
    } else {
      setAppType('personal');
    }
  };

  const loadApis = async (page = 1, size = pagination.pageSize) => {
    setLoading(true);
    try {
      const result = await fetchAppApis(appId, { curPage: page, pageSize: size });
      if (result && result.code === '200') {
        setApis(result.data || []);
        setPagination(prev => ({ ...prev, total: result.page?.total || 0, curPage: page, pageSize: size }));
      } else {
        message.error(result?.message || '加载API列表失败');
      }
    } catch (error) {
      message.error('加载API列表失败');
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    if (appId) {
      loadAppInfo();
      loadApis();
    }
  }, [appId]);

  const handleAddApi = () => {
    setDrawerOpen(true);
  };

  const handleConfirmPermission = async (selectedApis) => {
    const permissionIds = selectedApis
      .filter(api => api.id)
      .map(api => api.id);

    if (permissionIds.length === 0) {
      message.warning('没有可订阅的权限');
      return;
    }

    const result = await subscribeApis(appId, { permissionIds });
    if (result && result.code === '200') {
      message.success('申请已提交');
      setDrawerOpen(false);
      loadApis(1, INIT_PAGECONFIG.pageSize);
    } else {
      message.error(result?.message || '订阅失败');
    }
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
    const res = await withdrawApiApplication(appId, record.id);
    if (res && res.code === '200') {
      message.success('已撤回申请');
      loadApis();
    } else {
      message.error(res?.message || '撤回失败');
    }
  };

  const handleOpenDoc = (url) => {
    openUrl(url);
  };

  const handlePageChange = (page, size) => {
    const newSize = size || pagination.pageSize;
    loadApis(page, newSize);
  };

  const columns = getApiManagementColumns({
    handleOpenDoc,
    handleCopyApprovalAddress,
    handleWithdraw,
  });

  return (
    <div className="api-management">
      <div className="page-header">
        <div className="page-header-left">
          <h4 className="page-title">API管理</h4>
          <span className="page-desc">管理应用接口，配置API权限和调用参数</span>
        </div>
        {/* 添加API按钮，打开权限开通抽屉 */}
        <Button type="primary" onClick={handleAddApi} style={{ justifyContent: 'center', borderRadius: 6 }}>添加API</Button>
      </div>

      <Table
        columns={columns}
        dataSource={apis}
        rowKey="id"
        pagination={false}
        loading={loading}
      />

      {pagination.total > 0 && (
        <div style={{ marginTop: 16, textAlign: 'right' }}>
          <Pagination
            current={pagination.curPage}
            pageSize={pagination.pageSize}
            total={pagination.total}
            onChange={handlePageChange}
            showSizeChanger
            pageSizeOptions={PAGE_SIZE_OPTIONS}
            showQuickJumper
            showTotal={(total) => `共 ${total} 条`}
          />
        </div>
      )}

      <ApiPermissionDrawer
        open={drawerOpen}
        onClose={() => setDrawerOpen(false)}
        onConfirm={handleConfirmPermission}
        appType={appType}
        appId={appId}
      />

      <ApprovalAddressModal
        open={approvalModalOpen}
        onClose={() => setApprovalModalOpen(false)}
        approver={currentApprovalInfo.approver}
        approvalUrl={currentApprovalInfo.approvalUrl}
        onRemind={async () => {
          const res = await remindApproval(currentApprovalInfo.id);
          if (res && res.code === '200') {
            message.success('已催办');
          } else {
            message.error(res?.message || '催办失败');
          }
        }}
      />
    </div>
  );
}

export default ApiManagement;
