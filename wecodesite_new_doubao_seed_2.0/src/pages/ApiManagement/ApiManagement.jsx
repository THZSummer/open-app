import React, { useState } from 'react';
import { Button, Table, message } from 'antd';
import { PlusOutlined, FileTextOutlined, LinkOutlined, CloseCircleOutlined } from '@ant-design/icons';
import './ApiManagement.m.less';
import mockData from './mock';
import ApiPermissionDrawer from './ApiPermissionDrawer';

const ApiManagement = () => {
  const [apis, setApis] = useState(mockData.apis);
  const [isDrawerVisible, setIsDrawerVisible] = useState(false);

  const handleAddApi = () => {
    setIsDrawerVisible(true);
  };

  const handleCloseDrawer = () => {
    setIsDrawerVisible(false);
  };

  const handleConfirmAddApi = (selectedApis) => {
    // 处理添加 API 逻辑
    const newApis = selectedApis.map(api => ({
      id: Date.now().toString() + Math.random().toString(36).substr(2, 9),
      name: api.name,
      codeName: api.codeName,
      authType: api.authType,
      category: api.category,
      status: api.needApproval ? '审核中' : '已审核'
    }));
    setApis([...apis, ...newApis]);
    setIsDrawerVisible(false);
    message.success(`成功添加 ${newApis.length} 个 API`);
  };

  const handleCopyApprovalUrl = (api) => {
    // 模拟复制审批地址
    navigator.clipboard.writeText(`https://open.feishu.cn/approval/${api.id}`).then(() => {
      message.success('复制成功');
    });
  };

  const handleWithdrawApproval = (apiId) => {
    setApis(apis.map(api => 
      api.id === apiId ? { ...api, status: '已中止' } : api
    ));
    message.success('已撤回审核');
  };

  const handleViewDoc = (api) => {
    // 打开 API 文档
    window.open(`https://open.feishu.cn/document/${api.codeName}`, '_blank');
  };

  const columns = [
    {
      title: '权限名称',
      dataIndex: 'name',
      key: 'name'
    },
    {
      title: 'codeName',
      dataIndex: 'codeName',
      key: 'codeName',
      render: (codeName) => (
        <code className="codeName">{codeName}</code>
      )
    },
    {
      title: '认证方式',
      dataIndex: 'authType',
      key: 'authType'
    },
    {
      title: '分类',
      dataIndex: 'category',
      key: 'category'
    },
    {
      title: '状态',
      dataIndex: 'status',
      key: 'status',
      render: (status) => {
        let statusClass = '';
        switch (status) {
          case '已审核':
            statusClass = 'statusApproved';
            break;
          case '审核中':
            statusClass = 'statusPending';
            break;
          case '已中止':
            statusClass = 'statusCanceled';
            break;
          default:
            statusClass = '';
        }
        return (
          <span className={`statusTag ${statusClass}`}>
            {status}
          </span>
        );
      }
    },
    {
      title: '操作',
      key: 'action',
      render: (_, record) => {
        const actions = [];
        if (record.status === '审核中') {
          actions.push(
            <Button
              key="copy"
              type="text"
              icon={<LinkOutlined />}
              onClick={() => handleCopyApprovalUrl(record)}
            >
              复制审批地址
            </Button>
          );
          actions.push(
            <Button
              key="withdraw"
              type="text"
              danger
              icon={<CloseCircleOutlined />}
              onClick={() => handleWithdrawApproval(record.id)}
            >
              撤回审核
            </Button>
          );
        }
        actions.push(
          <Button
            key="doc"
            type="text"
            icon={<FileTextOutlined />}
            onClick={() => handleViewDoc(record)}
          >
            查看文档
          </Button>
        );
        return actions;
      }
    }
  ];

  return (
    <div className="apiManagement">
      <div className="header">
        <h1 className="title">API管理</h1>
        <p className="description">管理应用接口，配置API权限和调用参数</p>
        <Button type="primary" icon={<PlusOutlined />} onClick={handleAddApi}>
          添加API
        </Button>
      </div>
      <div className="card">
        <Table
          columns={columns}
          dataSource={apis}
          rowKey="id"
          pagination={{ pageSize: 10 }}
        />
      </div>

      {/* 开通权限 Drawer */}
      <ApiPermissionDrawer
        visible={isDrawerVisible}
        onClose={handleCloseDrawer}
        onConfirm={handleConfirmAddApi}
      />
    </div>
  );
};

export default ApiManagement;