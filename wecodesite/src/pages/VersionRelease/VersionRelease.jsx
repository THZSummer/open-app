import React, { useState } from 'react';
import { useSearchParams, useNavigate } from 'react-router-dom';
import { Typography, Table, Tag, Button, Space, Popconfirm } from 'antd';
import { PlusOutlined, EyeOutlined, DeleteOutlined } from '@ant-design/icons';
import { mockVersions } from './mock';
import './VersionRelease.m.less';

const { Title, Text } = Typography;

const statusColorMap = {
  '已发布': 'green',
  '审核中': 'blue',
  '审核未通过': 'red',
};

function VersionRelease() {
  const [searchParams] = useSearchParams();
  const navigate = useNavigate();
  const appId = searchParams.get('appId') || '1';
  const [versions, setVersions] = useState(mockVersions);

  const handleCreate = () => {
    navigate(`/version-release/form?appId=${appId}`);
  };

  const handleView = (record) => {
    navigate(`/version-release/form?appId=${appId}&versionId=${record.id}`);
  };

  const handleDelete = (record) => {
    setVersions((prev) => prev.filter((v) => v.id !== record.id));
  };

  const columns = [
    {
      title: '版本号',
      dataIndex: 'version',
      key: 'version',
      render: (text) => <Text strong>{text}</Text>,
    },
    {
      title: '版本状态',
      dataIndex: 'status',
      key: 'status',
      render: (status) => (
        <Tag color={statusColorMap[status]}>{status}</Tag>
      ),
    },
    {
      title: '发布人',
      dataIndex: 'publisher',
      key: 'publisher',
    },
    {
      title: '审核通过时间',
      dataIndex: 'approvedTime',
      key: 'approvedTime',
    },
    {
      title: '操作',
      key: 'actions',
      render: (_, record) => (
        <Space>
          <Button
            type="link"
            size="small"
            icon={<EyeOutlined />}
            onClick={() => handleView(record)}
          >
            查看
          </Button>
          {record.status === '审核未通过' && (
            <Popconfirm
              title="删除确认"
              description="确定要删除这条版本记录吗？"
              onConfirm={() => handleDelete(record)}
              okText="确定"
              cancelText="取消"
            >
              <Button
                type="link"
                size="small"
                danger
                icon={<DeleteOutlined />}
              >
                删除
              </Button>
            </Popconfirm>
          )}
        </Space>
      ),
    },
  ];

  return (
    <div className="version-release">
      <div className="page-header">
        <div className="page-header-left">
          <Title level={4} className="page-title">版本发布</Title>
          <Text type="secondary" className="page-desc">管理应用版本和发布历史</Text>
        </div>
        <Button
          type="primary"
          icon={<PlusOutlined />}
          className="btn-primary"
          onClick={handleCreate}
        >
          创建新版本
        </Button>
      </div>

      <Table
        dataSource={versions}
        columns={columns}
        rowKey="id"
        pagination={false}
        className="version-table"
      />
    </div>
  );
}

export default VersionRelease;