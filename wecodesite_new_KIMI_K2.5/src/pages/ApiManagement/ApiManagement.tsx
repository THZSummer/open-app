import React, { useState, useEffect } from 'react';
import { Button, Table, Tag, message } from 'antd';
import { PlusOutlined, CopyOutlined, UndoOutlined } from '@ant-design/icons';
import ApiPermissionDrawer from './ApiPermissionDrawer';
import { fetchApiList } from './thunk';
import type { ApiPermission } from '../../types';
import styles from './ApiManagement.module.less';

const statusColors: Record<string, string> = {
  '已审核': 'success',
  '审核中': 'warning',
  '已中止': 'error',
};

const ApiManagement: React.FC = () => {
  const [apis, setApis] = useState<ApiPermission[]>([]);
  const [loading, setLoading] = useState(false);
  const [drawerVisible, setDrawerVisible] = useState(false);

  useEffect(() => {
    loadApis();
  }, []);

  const loadApis = async () => {
    setLoading(true);
    const data = await fetchApiList();
    setApis(data);
    setLoading(false);
  };

  const handleAddApi = () => {
    setDrawerVisible(true);
  };

  const handleConfirm = (selectedApis: string[]) => {
    message.success(`已选择 ${selectedApis.length} 个 API 权限`);
    setDrawerVisible(false);
  };

  const columns = [
    { title: '权限名称', dataIndex: 'name', key: 'name' },
    { 
      title: 'codeName', 
      dataIndex: 'codeName', 
      key: 'codeName',
      render: (code: string) => <code className={styles.code}>{code}</code>,
    },
    { title: '认证方式', dataIndex: 'authType', key: 'authType' },
    { title: '分类', dataIndex: 'category', key: 'category' },
    {
      title: '状态',
      dataIndex: 'status',
      key: 'status',
      render: (status: string) => (
        <Tag color={statusColors[status] || 'default'}>{status}</Tag>
      ),
    },
    {
      title: '操作',
      key: 'action',
      render: (_: any, record: ApiPermission) => (
        <div className={styles.actions}>
          {record.status === '已审核' && <Button type="link">查看文档</Button>}
          {record.status === '审核中' && (
            <>
              <Button type="link" icon={<CopyOutlined />}>复制</Button>
              <Button type="link" danger icon={<UndoOutlined />}>撤回</Button>
            </>
          )}
        </div>
      ),
    },
  ];

  return (
    <div className={styles.container}>
      <div className={styles.header}>
        <div>
          <h2 className={styles.title}>API管理</h2>
          <p className={styles.desc}>管理应用接口，配置API权限和调用参数</p>
        </div>
        <Button type="primary" icon={<PlusOutlined />} onClick={handleAddApi}>
          添加API
        </Button>
      </div>
      <Table 
        columns={columns} 
        dataSource={apis} 
        loading={loading} 
        rowKey="id"
        pagination={{
          pageSize: 10,
          showSizeChanger: true,
          showQuickJumper: true,
          showTotal: (total) => `共 ${total} 条`,
        }}
      />
      <ApiPermissionDrawer
        visible={drawerVisible}
        onClose={() => setDrawerVisible(false)}
        onConfirm={handleConfirm}
      />
    </div>
  );
};

export default ApiManagement;
