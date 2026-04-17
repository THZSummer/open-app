import React, { useState, useEffect } from 'react';
import { useNavigate } from 'react-router-dom';
import { Button, Table, Tag, message } from 'antd';
import { PlusOutlined, EyeOutlined, DeleteOutlined } from '@ant-design/icons';
import { fetchVersions, deleteVersion } from './thunk';
import type { Version } from '../../types';
import styles from './VersionRelease.module.less';

const statusColors: Record<string, string> = {
  '已发布': 'success',
  '审核中': 'blue',
  '审核未通过': 'error',
};

const VersionRelease: React.FC = () => {
  const navigate = useNavigate();
  const [versions, setVersions] = useState<Version[]>([]);
  const [loading, setLoading] = useState(false);

  useEffect(() => {
    loadVersions();
  }, []);

  const loadVersions = async () => {
    setLoading(true);
    const data = await fetchVersions();
    setVersions(data);
    setLoading(false);
  };

  const handleCreateVersion = () => {
    navigate('form');
  };

  const handleViewVersion = (version: Version) => {
    message.info(`查看版本: ${version.version}`);
  };

  const handleDeleteVersion = async (id: number) => {
    await deleteVersion(id);
    setVersions(versions.filter(v => v.id !== id));
    message.success('删除成功');
  };

  const columns = [
    { title: '版本号', dataIndex: 'version', key: 'version' },
    {
      title: '版本状态',
      dataIndex: 'status',
      key: 'status',
      render: (status: string) => (
        <Tag color={statusColors[status] || 'default'}>{status}</Tag>
      ),
    },
    { title: '发布人', dataIndex: 'publisher', key: 'publisher' },
    { title: '审核通过时间', dataIndex: 'approvedTime', key: 'approvedTime' },
    {
      title: '操作',
      key: 'action',
      render: (_: any, record: Version) => (
        <div className={styles.actions}>
          <Button 
            type="link" 
            icon={<EyeOutlined />}
            onClick={() => handleViewVersion(record)}
          >
            查看
          </Button>
          {record.status === '审核未通过' && (
            <Button 
              type="link" 
              danger
              icon={<DeleteOutlined />}
              onClick={() => handleDeleteVersion(record.id)}
            >
              删除
            </Button>
          )}
        </div>
      ),
    },
  ];

  return (
    <div className={styles.container}>
      <div className={styles.header}>
        <div>
          <h2 className={styles.title}>版本发布</h2>
          <p className={styles.desc}>管理应用版本和发布历史</p>
        </div>
        <Button type="primary" icon={<PlusOutlined />} onClick={handleCreateVersion}>
          创建新版本
        </Button>
      </div>
      <Table
        columns={columns}
        dataSource={versions}
        loading={loading}
        rowKey="id"
        pagination={{
          pageSize: 10,
          showSizeChanger: true,
          showQuickJumper: true,
          showTotal: (total) => `共 ${total} 条`,
        }}
      />
    </div>
  );
};

export default VersionRelease;
