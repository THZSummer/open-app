import React, { useState, useEffect } from 'react';
import { useSearchParams } from 'react-router-dom';
import { Table, Button, Tag } from 'antd';
import { PlusOutlined } from '@ant-design/icons';
import { fetchApiList } from './thunk';
import ApiPermissionDrawer from './ApiPermissionDrawer';
import './ApiManagement.m.less';

const getStatusTag = (status) => {
  const colorMap = {
    '已审核': 'green',
    '审核中': 'orange',
    '已中止': 'red',
  };
  return <Tag color={colorMap[status]}>{status}</Tag>;
};

const columns = [
  {
    title: '权限名称',
    dataIndex: 'name',
    key: 'name',
  },
  {
    title: 'codeName',
    dataIndex: 'codeName',
    key: 'codeName',
    render: (code) => <code>{code}</code>,
  },
  {
    title: '认证方式',
    dataIndex: 'auth',
    key: 'auth',
  },
  {
    title: '分类',
    dataIndex: 'category',
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
        {record.status === '审核中' && (
          <>
            <Button type="link" size="small">复制审批地址</Button>
            <Button type="link" size="small">撤回审核</Button>
          </>
        )}
        <Button type="link" size="small">查看文档</Button>
      </div>
    ),
  },
];

function ApiManagement() {
  const [searchParams] = useSearchParams();
  const [apis, setApis] = useState([]);
  const [loading, setLoading] = useState(false);
  const [drawerOpen, setDrawerOpen] = useState(false);

  useEffect(() => {
    const loadData = async () => {
      setLoading(true);
      const data = await fetchApiList();
      setApis(data);
      setLoading(false);
    };
    loadData();
  }, []);

  const handleAddApi = () => {
    setDrawerOpen(true);
  };

  const handleConfirmPermission = (selectedApis) => {
    const newApis = selectedApis.map(api => ({
      ...api,
      id: Date.now() + api.id,
      status: api.needReview ? '审核中' : '已审核',
    }));
    setApis(prev => [...prev, ...newApis]);
  };

  return (
    <div className="api-management">
      <div className="page-header">
        <div className="page-header-left">
          <h4 className="page-title">API管理</h4>
          <span className="page-desc">管理应用接口，配置API权限和调用参数</span>
        </div>
        <Button 
          type="primary" 
          icon={<PlusOutlined />} 
          className="btn-primary"
          onClick={handleAddApi}
        >
          添加API
        </Button>
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
      />
    </div>
  );
}

export default ApiManagement;
