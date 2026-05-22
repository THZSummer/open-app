import { useEffect, useState } from 'react';
import { Drawer, Tree, Table, Button, Space, Tag, message, Spin, Empty, Checkbox } from 'antd';
import type { TreeProps, DataNode } from 'antd/es/tree';
import type { ColumnsType } from 'antd/es/table';
import { CheckCircleOutlined } from '@ant-design/icons';
import { useCategory } from '@/hooks';
import { getCategoryCallbackPermissions, subscribeCallbackPermissions } from '@/services/permission.service';
import { CallbackPermission } from '@/services/permission.service';
import styles from './PermissionDrawer.module.less';

interface CallbackPermissionDrawerProps {
  visible: boolean;
  appId: string;
  onClose: () => void;
}

const CallbackPermissionDrawer: React.FC<CallbackPermissionDrawerProps> = ({
  visible,
  appId,
  onClose,
}) => {
  const { categoryTree, fetchCategoryTree } = useCategory();
  const [selectedCategory, setSelectedCategory] = useState<string>('');
  const [loading, setLoading] = useState(false);
  const [permissions, setPermissions] = useState<CallbackPermission[]>([]);
  const [selectedPermissionIds, setSelectedPermissionIds] = useState<string[]>([]);
  const [submitting, setSubmitting] = useState(false);

  useEffect(() => {
    if (visible) {
      fetchCategoryTree();
      setSelectedPermissionIds([]);
      setPermissions([]);
      setSelectedCategory('');
    }
  }, [visible, fetchCategoryTree]);

  const convertToTreeData = (categories: any[]): DataNode[] => {
    return categories.map((category) => ({
      key: category.id,
      title: category.nameCn,
      children: category.children ? convertToTreeData(category.children) : undefined,
    }));
  };

  const handleTreeSelect: TreeProps['onSelect'] = async (selectedKeys) => {
    if (selectedKeys.length > 0) {
      const categoryId = selectedKeys[0] as string;
      setSelectedCategory(categoryId);
      setLoading(true);
      try {
        const response = await getCategoryCallbackPermissions(categoryId, {
          includeChildren: true,
        });
        setPermissions(response.data.data || []);
      } catch (error) {
        console.error('获取权限列表失败:', error);
        setPermissions([]);
      } finally {
        setLoading(false);
      }
    }
  };

  const handleSelectPermission = (permissionId: string, checked: boolean) => {
    if (checked) {
      setSelectedPermissionIds([...selectedPermissionIds, permissionId]);
    } else {
      setSelectedPermissionIds(selectedPermissionIds.filter((id) => id !== permissionId));
    }
  };

  const handleSelectAll = (checked: boolean) => {
    if (checked) {
      setSelectedPermissionIds(permissions.map((p) => p.id));
    } else {
      setSelectedPermissionIds([]);
    }
  };

  const handleSubmit = async () => {
    if (selectedPermissionIds.length === 0) {
      message.warning('请至少选择一个权限');
      return;
    }

    setSubmitting(true);
    try {
      const response = await subscribeCallbackPermissions(appId, selectedPermissionIds);
      const { successCount, failedCount } = response.data.data;
      if (successCount > 0) {
        message.success(`权限申请已提交，成功 ${successCount} 条，等待审批`);
        onClose();
      }
      if (failedCount > 0) {
        message.warning(`${failedCount} 条权限申请失败`);
      }
    } catch (error) {
      console.error('提交权限申请失败:', error);
    } finally {
      setSubmitting(false);
    }
  };

  const columns: ColumnsType<CallbackPermission> = [
    {
      title: (
        <Checkbox
          checked={selectedPermissionIds.length === permissions.length && permissions.length > 0}
          indeterminate={selectedPermissionIds.length > 0 && selectedPermissionIds.length < permissions.length}
          onChange={(e) => handleSelectAll(e.target.checked)}
        >
          权限名称
        </Checkbox>
      ),
      dataIndex: 'nameCn',
      key: 'nameCn',
      width: 200,
      render: (text: string, record: CallbackPermission) => (
        <div>
          <Checkbox
            checked={selectedPermissionIds.includes(record.id)}
            onChange={(e) => handleSelectPermission(record.id, e.target.checked)}
          >
            {text}
          </Checkbox>
          <div style={{ color: '#999', fontSize: 12, marginLeft: 24 }}>{record.nameEn}</div>
        </div>
      ),
    },
    {
      title: 'Scope',
      dataIndex: 'scope',
      key: 'scope',
      width: 200,
      render: (scope: string) => <Tag>{scope}</Tag>,
    },
    {
      title: '需要审批',
      dataIndex: 'needApproval',
      key: 'needApproval',
      width: 100,
      render: (needApproval: number) =>
        needApproval === 1 ? <Tag color="orange">是</Tag> : <Tag color="green">否</Tag>,
    },
    {
      title: '已订阅',
      dataIndex: 'isSubscribed',
      key: 'isSubscribed',
      width: 100,
      render: (isSubscribed?: number) =>
        isSubscribed === 1 ? <CheckCircleOutlined style={{ color: '#52c41a' }} /> : '-',
    },
  ];

  return (
    <Drawer
      title="回调权限申请"
      placement="right"
      width={900}
      onClose={onClose}
      open={visible}
      footer={
        <Space>
          <Button onClick={onClose}>取消</Button>
          <Button
            type="primary"
            onClick={handleSubmit}
            loading={submitting}
            disabled={selectedPermissionIds.length === 0}
          >
            提交申请 ({selectedPermissionIds.length})
          </Button>
        </Space>
      }
    >
      <div className={styles.container}>
        <div className={styles.left}>
          <div className={styles.treeTitle}>选择分类</div>
          {categoryTree.length > 0 ? (
            <Tree
              showLine
              treeData={convertToTreeData(categoryTree)}
              onSelect={handleTreeSelect}
              selectedKeys={selectedCategory ? [selectedCategory] : []}
            />
          ) : (
            <Empty description="暂无分类数据" />
          )}
        </div>
        <div className={styles.right}>
          <div className={styles.tableTitle}>
            权限列表
            {selectedCategory && (
              <Tag color="blue" style={{ marginLeft: 8 }}>
                已选择 {selectedPermissionIds.length} 项
              </Tag>
            )}
          </div>
          <Spin spinning={loading}>
            {selectedCategory ? (
              <Table
                columns={columns}
                dataSource={permissions}
                rowKey="id"
                pagination={false}
                size="small"
                scroll={{ y: 600 }}
              />
            ) : (
              <Empty description="请选择分类查看权限列表" style={{ marginTop: 100 }} />
            )}
          </Spin>
        </div>
      </div>
    </Drawer>
  );
};

export default CallbackPermissionDrawer;
