import React, { useState, useEffect } from 'react';
import { useSearchParams } from 'react-router-dom';
import { Table, Button, Tag } from 'antd';
import { fetchApiList } from './thunk';
import { fetchAppInfo } from '../BasicInfo/thunk';
import ApiPermissionDrawer from './ApiPermissionDrawer';
import ApprovalAddressModal from '../../components/ApprovalAddressModal/ApprovalAddressModal';
import { remindApproval, deleteApi, withdrawApproval } from './thunk';
import { SUBSCRIPTION_STATUS, AUTH_TYPE } from '../../utils/constants';
import './ApiManagement.m.less';

function getStatusTag(status) {
  const { text, color } = SUBSCRIPTION_STATUS[status] || { text: '未知', color: 'default' };
  return <Tag color={color}>{text}</Tag>;
}

function ApiManagement() {
  const [searchParams] = useSearchParams();
  const [apis, setApis] = useState([]);
  const [loading, setLoading] = useState(false);
  const [drawerOpen, setDrawerOpen] = useState(false);
  const [appType, setAppType] = useState('business');
  const [approvalModalOpen, setApprovalModalOpen] = useState(false);
  const [currentApprovalInfo, setCurrentApprovalInfo] = useState({});
  const [deleteModalOpen, setDeleteModalOpen] = useState(false);
  const [currentDeleteId, setCurrentDeleteId] = useState(null);
  const [deleteLoading, setDeleteLoading] = useState(false);

  useEffect(() => {
    const loadData = async () => {
      setLoading(true);
      const data = await fetchApiList();
      setApis(data);
      setLoading(false);
    };
    loadData();
  }, []);

  useEffect(() => {
    const loadAppInfo = async () => {
      const appId = searchParams.get('appId') || '1';
      const appInfo = await fetchAppInfo(appId);
      if (appInfo && appInfo.eamap) {
        setAppType('business');
      } else {
        setAppType('personal');
      }
    };
    loadAppInfo();
  }, [searchParams]);

  const handleAddApi = () => {
    setDrawerOpen(true);
  };

  const handleConfirmPermission = (selectedApis) => {
    const newApis = selectedApis.map(api => ({
      ...api,
      id: String(Date.now() + api.id),
      status: api.needReview ? 0 : 1,
      approver: api.needReview ? {
        userId: 'pending',
        userName: '待分配'
      } : null,
      approvalUrl: api.needReview ? 'https://approval.example.com/api/' + (Date.now() + api.id) : ''
    }));
    setApis(prev => [...prev, ...newApis]);
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
    const data = await fetchApiList();
    setApis(data);
  };

  const handleOpenDoc = (url) => {
    window.open(url, '_blank');
  };

  const handleDeleteClick = (id) => {
    setCurrentDeleteId(id);
    setDeleteModalOpen(true);
  };

  const columns = [
    {
      title: '权限名称',
      dataIndex: ['permission', 'nameCn'],
      key: 'nameCn',
    },
    {
      title: 'scope',
      dataIndex: ['permission', 'scope'],
      key: 'scope',
      render: (code) => <code>{code}</code>,
    },
    {
      title: '认证方式',
      dataIndex: 'authType',
      key: 'authType',
      render: (type) => AUTH_TYPE[type] || '-',
    },
    {
      title: '分类',
      dataIndex: ['category', 'nameCn'],
      key: 'category',
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
        <div style={{ display: 'flex', gap: 8 }}>
          <Button type="link" size="small" onClick={() => handleOpenDoc(record.api?.docUrl)}>查看文档</Button>
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
      <div className="page-header">
        <div className="page-header-left">
          <h4 className="page-title">API管理</h4>
          <span className="page-desc">管理应用接口，配置API权限和调用参数</span>
        </div>
        <Button type="primary" onClick={handleAddApi} style={{ justifyContent: 'center', borderRadius: 6 }}>添加API</Button>
      </div>
      <Table 
        columns={columns} 
        dataSource={apis} 
        rowKey="id" 
        pagination={false}
        loading={loading}
      />
      <ApiPermissionDrawer
        open={drawerOpen}
        onClose={() => setDrawerOpen(false)}
        onConfirm={handleConfirmPermission}
        appType={appType}
      />
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