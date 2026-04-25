import React, { useState, useEffect } from 'react';
import { Table, Button, Tag, Pagination, message } from 'antd';
import { fetchAppApis, subscribeApis, withdrawApiApplication, remindApproval } from './thunk';
import { fetchAppInfo } from '../BasicInfo/thunk';
import ApiPermissionDrawer from './ApiPermissionDrawer';
import ApprovalAddressModal from '../../components/ApprovalAddressModal/ApprovalAddressModal';
import { SUBSCRIPTION_STATUS, AUTH_TYPE, PAGE_SIZE_OPTIONS, INIT_PAGECONFIG } from '../../utils/constants';
import { getApiManagementColumns } from './constants';
import { openUrl, queryParams } from '../../utils/common';
import './ApiManagement.m.less';

/**
 * API管理页面组件
 * 用于管理应用已订阅的API权限列表，包括查看、添加、撤回、删除等操作
 */
function ApiManagement() {
  // 当前应用ID
  const appId = queryParams('appId');

  // 已订阅API列表数据
  const [apis, setApis] = useState([]);
  // 数据加载状态
  const [loading, setLoading] = useState(false);
  // 权限抽屉弹窗显示状态
  const [drawerOpen, setDrawerOpen] = useState(false);
  // 应用类型：'business'(企业应用) 或 'personal'(个人应用)
  const [appType, setAppType] = useState('business');
  // 审批地址弹窗显示状态
  const [approvalModalOpen, setApprovalModalOpen] = useState(false);
  // 当前待审批记录信息
  const [currentApprovalInfo, setCurrentApprovalInfo] = useState({});
  // 分页配置对象
  const [pagination, setPagination] = useState(INIT_PAGECONFIG);

  /**
   * 加载应用信息
   */
  const loadAppInfo = async () => {
    const appInfo = await fetchAppInfo(appId);
    if (appInfo && appInfo.eamap) {
      setAppType('business');
    } else {
      setAppType('personal');
    }
  };

  /**
   * 加载API列表
   */
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

  /**
   * 组件挂载和 appId 变化时加载数据
   */
  useEffect(() => {
    if (appId) {
      loadAppInfo();
      loadApis();
    }
  }, [appId]);

  /**
   * 打开权限开通抽屉弹窗
   */
  const handleAddApi = () => {
    setDrawerOpen(true);
  };

  /**
   * 确认开通权限后的回调处理
   */
  const handleConfirmPermission = async (selectedApis) => {
    // 权限ID直接是 api.id，不再嵌套在 permission 对象中
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

  /**
   * 打开审批地址复制弹窗
   */
  const handleCopyApprovalAddress = (record) => {
    setCurrentApprovalInfo({
      id: record.id,
      approver: record.approver?.userName || '待分配',
      approvalUrl: record.approvalUrl || ''
    });
    setApprovalModalOpen(true);
  };

  /**
   * 撤回待审核的API申请
   */
  const handleWithdraw = async (record) => {
    const res = await withdrawApiApplication(appId, record.id);
    if (res && res.code === '200') {
      message.success('已撤回申请');
      loadApis();
    } else {
      message.error(res?.message || '撤回失败');
    }
  };

  /**
   * 在新窗口打开API文档
   */
  const handleOpenDoc = (url) => {
    openUrl(url);
  };

  /**
   * 处理分页变化
   */
  const handlePageChange = (page, size) => {
    const newSize = size || pagination.pageSize;
    loadApis(page, newSize);
  };

  // 表格列配置
  const renderScope = (code) => <code>{code}</code>;
  const renderAuthType = (type) => AUTH_TYPE[type] || '-';
  const renderStatus = (status) => {
    const { text, color } = SUBSCRIPTION_STATUS[status] || { text: '未知', color: 'default' };
    return <Tag color={color}>{text}</Tag>;
  };
  const renderAction = (_, record) => (
    <div style={{ display: 'flex', gap: 8 }}>
      <Button type="link" size="small" onClick={() => handleOpenDoc(record.api?.docUrl)}>查看文档</Button>
      {record.status === 0 && (
        <>
          <Button type="link" size="small" onClick={() => handleCopyApprovalAddress(record)}>复制审批地址</Button>
          <Button type="link" size="small" onClick={() => handleWithdraw(record)}>撤回审核</Button>
        </>
      )}
    </div>
  );

  const columns = getApiManagementColumns({
    renderScope,
    renderAuthType,
    renderStatus,
    renderAction,
  });

  return (
    <div className="api-management">
      {/* 页面头部 */}
      <div className="page-header">
        <div className="page-header-left">
          <h4 className="page-title">API管理</h4>
          <span className="page-desc">管理应用接口，配置API权限和调用参数</span>
        </div>
        {/* 添加API按钮，打开权限开通抽屉 */}
        <Button type="primary" onClick={handleAddApi} style={{ justifyContent: 'center', borderRadius: 6 }}>添加API</Button>
      </div>

      {/* API列表表格 */}
      <Table
        columns={columns}
        dataSource={apis}
        rowKey="id"
        pagination={false}
        loading={loading}
      />

      {/* 分页器 */}
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

      {/* 权限开通抽屉弹窗组件 */}
      <ApiPermissionDrawer
        open={drawerOpen}
        onClose={() => setDrawerOpen(false)}
        onConfirm={handleConfirmPermission}
        appType={appType}
        appId={appId}
      />

      {/* 审批地址弹窗组件 */}
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
