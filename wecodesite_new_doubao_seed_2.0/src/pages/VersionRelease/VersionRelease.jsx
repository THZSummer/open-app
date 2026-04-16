import React, { useState } from 'react';
import { Button, Table, Tag, message } from 'antd';
import { PlusOutlined, EditOutlined } from '@ant-design/icons';
import { useNavigate, useLocation } from 'react-router-dom';
import './VersionRelease.m.less';
import mockData from './mock';

const VersionRelease = () => {
  const navigate = useNavigate();
  const location = useLocation();
  const searchParams = new URLSearchParams(location.search);
  const appId = searchParams.get('appId');
  const [versions, setVersions] = useState(mockData.versions);

  const handleCreateVersion = () => {
    // 跳转到版本表单页面
    const params = new URLSearchParams();
    if (appId) params.append('appId', appId);
    navigate(`/version-release/form?${params.toString()}`);
  };

  const handleViewVersion = (version) => {
    // 跳转到版本表单页面，查看版本详情
    const params = new URLSearchParams();
    if (appId) params.append('appId', appId);
    params.append('versionId', version.id);
    navigate(`/version-release/form?${params.toString()}`);
  };

  const columns = [
    {
      title: '版本号',
      dataIndex: 'version',
      key: 'version'
    },
    {
      title: '状态',
      dataIndex: 'status',
      key: 'status',
      render: (status) => {
        let statusColor = '';
        switch (status) {
          case '已发布':
            statusColor = 'green';
            break;
          case '审核中':
            statusColor = 'orange';
            break;
          case '草稿':
            statusColor = 'blue';
            break;
          default:
            statusColor = 'default';
        }
        return (
          <Tag color={statusColor}>
            {status}
          </Tag>
        );
      }
    },
    {
      title: '创建时间',
      dataIndex: 'createdAt',
      key: 'createdAt'
    },
    {
      title: '发布时间',
      dataIndex: 'publishedAt',
      key: 'publishedAt'
    },
    {
      title: '操作',
      key: 'action',
      render: (_, record) => (
        <Button
          type="text"
          icon={<EditOutlined />}
          onClick={() => handleViewVersion(record)}
        >
          查看
        </Button>
      )
    }
  ];

  return (
    <div className="versionRelease">
      <div className="header">
        <h1 className="title">版本发布</h1>
        <p className="description">管理应用版本和发布流程</p>
        <Button type="primary" icon={<PlusOutlined />} onClick={handleCreateVersion}>
          新建版本
        </Button>
      </div>
      <div className="card">
        <Table
          columns={columns}
          dataSource={versions}
          rowKey="id"
          pagination={{ pageSize: 10 }}
        />
      </div>
    </div>
  );
};

export default VersionRelease;