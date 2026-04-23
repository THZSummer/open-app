import React, { useState, useEffect } from 'react';
import { useSearchParams } from 'react-router-dom';
import { Table, Button, Tag } from 'antd';
import { fetchAppApis, subscribeApis, withdrawApiApplication, remindApproval } from './thunk';
import { fetchAppInfo } from '../BasicInfo/thunk';
import ApiPermissionDrawer from './ApiPermissionDrawer';
import ApprovalAddressModal from '../../components/ApprovalAddressModal/ApprovalAddressModal';
import { SUBSCRIPTION_STATUS, AUTH_TYPE } from '../../utils/constants';
import './ApiManagement.m.less';

/**
 * 获取订阅状态的标签显示
 * @param {number} status - 订阅状态码
 * @returns {React.ReactNode} 状态标签组件
 */
function getStatusTag(status) {
  const { text, color } = SUBSCRIPTION_STATUS[status] || { text: '未知', color: 'default' };
  return <Tag color={color}>{text}</Tag>;
}

/**
 * API管理页面组件
 * 用于管理应用已订阅的API权限列表，包括查看、添加、撤回、删除等操作
 */
function ApiManagement() {
  // URL查询参数，用于获取当前应用ID
  const [searchParams] = useSearchParams();

  // 已订阅API列表数据
  const [apis, setApis] = useState([]);
  // 数据加载状态
  const [loading, setLoading] = useState(false);
  // 权限抽屉弹窗显示状态
  const [drawerOpen, setDrawerOpen] = useState(false);
  // 应用类型：'business'(企业应用) 或 'personal'(个人应用)
  const [appType, setAppType] = useState('business');
  // 当前应用ID
  const [appId, setAppId] = useState('1');
  // 审批地址弹窗显示状态
  const [approvalModalOpen, setApprovalModalOpen] = useState(false);
  // 当前待审批记录信息
  const [currentApprovalInfo, setCurrentApprovalInfo] = useState({});

  /**
   * 组件挂载时加载已订阅的API列表
   */
  useEffect(() => {
    const loadData = async () => {
      setLoading(true);
      const result = await fetchAppApis(appId);
      setApis(result.data || []);
      setLoading(false);
    };
    loadData();
  }, [appId]);

  /**
   * 根据URL参数获取应用信息，确定应用类型
   * 企业应用(eamap)或个人应用
   */
  useEffect(() => {
    const loadAppInfo = async () => {
      // 从URL参数获取应用ID，默认为'1'
      const currentAppId = searchParams.get('appId') || '1';
      setAppId(currentAppId);
      const appInfo = await fetchAppInfo(currentAppId);
      // 根据应用是否包含eamap字段判断应用类型
      if (appInfo && appInfo.eamap) {
        setAppType('business');
      } else {
        setAppType('personal');
      }
    };
    loadAppInfo();
  }, [searchParams]);

  /**
   * 打开权限开通抽屉弹窗
   */
  const handleAddApi = () => {
    setDrawerOpen(true);
  };

  /**
   * 确认开通权限后的回调处理
   * 调用后端接口订阅API权限
   * @param {Array} selectedApis - 选中的API列表
   */
  const handleConfirmPermission = async (selectedApis) => {
    // 提取权限ID列表
    const permissionIds = selectedApis.map(api => api.id);
    // 调用后端接口订阅API权限
    const result = await subscribeApis(appId, { permissionIds });
    // 重新获取最新列表
    const listResult = await fetchAppApis(appId);
    setApis(listResult.data || []);
  };

  /**
   * 打开审批地址复制弹窗
   * @param {Object} record - API订阅记录
   */
  const handleCopyApprovalAddress = (record) => {
    setCurrentApprovalInfo({
      id: record.id,
      // 审批人名称，默认'待分配'
      approver: record.approver?.userName || '待分配',
      // 审批URL
      approvalUrl: record.approvalUrl || ''
    });
    setApprovalModalOpen(true);
  };

  /**
   * 撤回待审核的API申请
   * @param {Object} record - 待撤回的API订阅记录
   */
  const handleWithdraw = async (record) => {
    // 调用后端撤回接口
    await withdrawApiApplication(appId, record.id);
    // 重新获取最新列表
    const result = await fetchAppApis(appId);
    setApis(result.data || []);
  };

  /**
   * 在新窗口打开API文档
   * @param {string} url - 文档URL
   */
  const handleOpenDoc = (url) => {
    window.open(url, '_blank');
  };

  // 表格列配置
  const columns = [
    {
      // 权限名称列
      title: '权限名称',
      dataIndex: ['permission', 'nameCn'],
      key: 'nameCn',
    },
    {
      // Scope标识列，使用code样式展示
      title: 'scope',
      dataIndex: ['permission', 'scope'],
      key: 'scope',
      render: (code) => <code>{code}</code>,
    },
    {
      // 认证方式列
      title: '认证方式',
      dataIndex: 'authType',
      key: 'authType',
      // 映射认证类型到中文显示
      render: (type) => AUTH_TYPE[type] || '-',
    },
    {
      // API分类列
      title: '分类',
      dataIndex: ['category', 'nameCn'],
      key: 'category',
    },
    {
      // 订阅状态列
      title: '状态',
      dataIndex: 'status',
      key: 'status',
      // 状态码转标签显示
      render: getStatusTag,
    },
    {
      // 操作列
      title: '操作',
      key: 'action',
      render: (_, record) => (
        <div style={{ display: 'flex', gap: 8 }}>
          {/* 查看文档按钮 */}
          <Button type="link" size="small" onClick={() => handleOpenDoc(record.api?.docUrl)}>查看文档</Button>
          {/* 待审核状态显示复制审批地址和撤回按钮 */}
          {record.status === 0 && (
            <>
              <Button type="link" size="small" onClick={() => handleCopyApprovalAddress(record)}>复制审批地址</Button>
              <Button type="link" size="small" onClick={() => handleWithdraw(record)}>撤回审核</Button>
            </>
          )}
        </div>
      ),
    },
  ];

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

      {/* 权限开通抽屉弹窗组件 */}
      <ApiPermissionDrawer
        open={drawerOpen}
        onClose={() => setDrawerOpen(false)}
        onConfirm={handleConfirmPermission}
        appType={appType}
      />

      {/* 审批地址弹窗组件 */}
      <ApprovalAddressModal
        open={approvalModalOpen}
        onClose={() => setApprovalModalOpen(false)}
        approver={currentApprovalInfo.approver}
        approvalUrl={currentApprovalInfo.approvalUrl}
        onRemind={() => remindApproval(currentApprovalInfo.id)}
      />
    </div>
  );
}

export default ApiManagement;