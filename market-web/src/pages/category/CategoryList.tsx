import { useEffect, useState } from 'react';
import {
  Card,
  Button,
  Tree,
  Modal,
  Form,
  Input,
  InputNumber,
  Space,
  Tag,
  Table,
  Popconfirm,
  Empty,
  Spin,
} from 'antd';
import {
  PlusOutlined,
  EditOutlined,
  DeleteOutlined,
  UserOutlined,
  FolderOutlined,
  FileOutlined,
} from '@ant-design/icons';
import type { TreeProps, DataNode } from 'antd/es/tree';
import { useCategory } from '@/hooks';
import { Category, CategoryOwner } from '@/services/category.service';
import styles from './CategoryList.module.less';

const { Search } = Input;

/**
 * 分类管理页面
 */
const CategoryList: React.FC = () => {
  const [form] = Form.useForm();
  const [ownerForm] = Form.useForm();
  const {
    loading,
    categoryTree,
    owners,
    fetchCategoryTree,
    handleCreateCategory,
    handleUpdateCategory,
    handleDeleteCategory,
    fetchOwners,
    handleAddOwner,
    handleRemoveOwner: removeOwner,
  } = useCategory();

  const [searchValue, setSearchValue] = useState('');
  const [categoryModalVisible, setCategoryModalVisible] = useState(false);
  const [ownerModalVisible, setOwnerModalVisible] = useState(false);
  const [currentCategory, setCurrentCategory] = useState<Category | null>(null);
  const [selectedCategoryId, setSelectedCategoryId] = useState<string>('');
  const [expandedKeys, setExpandedKeys] = useState<React.Key[]>([]);

  useEffect(() => {
    fetchCategoryTree();
  }, [fetchCategoryTree]);

  /**
   * 将分类数据转换为树节点数据
   */
  const convertToTreeData = (categories: Category[]): DataNode[] => {
    return categories.map((category) => ({
      key: category.id,
      title: (
        <div className={styles.treeNode}>
          <span className={styles.treeNodeTitle}>
            {category.categoryAlias && <Tag color="blue">{category.categoryAlias}</Tag>}
            {category.nameCn}
          </span>
          <Space className={styles.treeNodeActions}>
            <Button
              type="link"
              size="small"
              icon={<PlusOutlined />}
              onClick={(e) => {
                e.stopPropagation();
                handleAddChild(category);
              }}
            >
              添加子分类
            </Button>
            <Button
              type="link"
              size="small"
              icon={<EditOutlined />}
              onClick={(e) => {
                e.stopPropagation();
                handleEdit(category);
              }}
            >
              编辑
            </Button>
            <Button
              type="link"
              size="small"
              icon={<UserOutlined />}
              onClick={(e) => {
                e.stopPropagation();
                handleManageOwners(category);
              }}
            >
              责任人
            </Button>
            <Popconfirm
              title="确定删除该分类吗？"
              onConfirm={(e) => {
                e?.stopPropagation();
                handleDelete(category.id);
              }}
              okText="确定"
              cancelText="取消"
            >
              <Button
                type="link"
                size="small"
                danger
                icon={<DeleteOutlined />}
                onClick={(e) => e.stopPropagation()}
              >
                删除
              </Button>
            </Popconfirm>
          </Space>
        </div>
      ),
      icon: category.children && category.children.length > 0 ? <FolderOutlined /> : <FileOutlined />,
      children: category.children ? convertToTreeData(category.children) : undefined,
    }));
  };

  /**
   * 打开添加分类模态框
   */
  const handleAddRoot = () => {
    setCurrentCategory(null);
    form.resetFields();
    setCategoryModalVisible(true);
  };

  /**
   * 打开添加子分类模态框
   */
  const handleAddChild = (parent: Category) => {
    setCurrentCategory(parent);
    form.resetFields();
    form.setFieldsValue({ parentId: parent.id });
    setCategoryModalVisible(true);
  };

  /**
   * 打开编辑分类模态框
   */
  const handleEdit = (category: Category) => {
    setCurrentCategory(category);
    form.setFieldsValue({
      categoryAlias: category.categoryAlias,
      nameCn: category.nameCn,
      nameEn: category.nameEn,
      sortOrder: category.sortOrder,
    });
    setCategoryModalVisible(true);
  };

  /**
   * 删除分类
   */
  const handleDelete = async (id: string) => {
    const success = await handleDeleteCategory(id);
    if (success) {
      fetchCategoryTree();
    }
  };

  /**
   * 提交分类表单
   */
  const handleCategorySubmit = async () => {
    try {
      const values = await form.validateFields();
      let result;
      if (currentCategory && currentCategory.id) {
        // 编辑
        result = await handleUpdateCategory(currentCategory.id, {
          nameCn: values.nameCn,
          nameEn: values.nameEn,
          sortOrder: values.sortOrder,
        });
      } else {
        // 新建
        result = await handleCreateCategory({
          categoryAlias: values.categoryAlias,
          nameCn: values.nameCn,
          nameEn: values.nameEn,
          parentId: values.parentId,
          sortOrder: values.sortOrder,
        });
      }
      if (result) {
        setCategoryModalVisible(false);
        fetchCategoryTree();
      }
    } catch (error) {
      console.error('表单验证失败:', error);
    }
  };

  /**
   * 打开责任人管理模态框
   */
  const handleManageOwners = async (category: Category) => {
    setSelectedCategoryId(category.id);
    await fetchOwners(category.id);
    ownerForm.resetFields();
    setOwnerModalVisible(true);
  };

  /**
   * 添加责任人
   */
  const handleAddOwnerSubmit = async () => {
    try {
      const values = await ownerForm.validateFields();
      const result = await handleAddOwner(selectedCategoryId, {
        userId: values.userId,
        userName: values.userName,
      });
      if (result) {
        ownerForm.resetFields();
        fetchOwners(selectedCategoryId);
      }
    } catch (error) {
      console.error('添加责任人失败:', error);
    }
  };

  /**
   * 移除责任人
   */
  const handleRemoveOwner = async (userId: string) => {
    const success = await removeOwner(selectedCategoryId, userId);
    if (success) {
      fetchOwners(selectedCategoryId);
    }
  };

  /**
   * 树节点选中事件
   */
  const handleTreeSelect: TreeProps['onSelect'] = (selectedKeys) => {
    if (selectedKeys.length > 0) {
      const categoryId = selectedKeys[0] as string;
      // 可以跳转到分类下的资源列表
      console.log('选中分类:', categoryId);
    }
  };

  /**
   * 搜索过滤
   */
  const filterTree = (data: Category[], search: string): Category[] => {
    if (!search) return data;
    return data
      .map((item) => {
        if (item.nameCn.includes(search) || item.nameEn.includes(search)) {
          return item;
        }
        if (item.children && item.children.length > 0) {
          const filteredChildren = filterTree(item.children, search);
          if (filteredChildren.length > 0) {
            return { ...item, children: filteredChildren };
          }
        }
        return null;
      })
      .filter(Boolean) as Category[];
  };

  const filteredTree = filterTree(categoryTree, searchValue);

  /**
   * 责任人表格列定义
   */
  const ownerColumns = [
    {
      title: '用户ID',
      dataIndex: 'userId',
      key: 'userId',
    },
    {
      title: '用户名称',
      dataIndex: 'userName',
      key: 'userName',
    },
    {
      title: '操作',
      key: 'action',
      render: (_: any, record: CategoryOwner) => (
        <Popconfirm
          title="确定移除该责任人吗？"
          onConfirm={() => handleRemoveOwner(record.userId)}
          okText="确定"
          cancelText="取消"
        >
          <Button type="link" danger size="small">
            移除
          </Button>
        </Popconfirm>
      ),
    },
  ];

  return (
    <div className={styles.container}>
      <Card
        title="分类管理"
        extra={
          <Button type="primary" icon={<PlusOutlined />} onClick={handleAddRoot}>
            新增一级分类
          </Button>
        }
      >
        <div className={styles.toolbar}>
          <Search
            placeholder="搜索分类名称"
            value={searchValue}
            onChange={(e) => setSearchValue(e.target.value)}
            style={{ width: 300 }}
            allowClear
          />
        </div>

        <Spin spinning={loading}>
          {filteredTree.length > 0 ? (
            <Tree
              showLine
              showIcon
              expandedKeys={expandedKeys}
              onExpand={(keys) => setExpandedKeys(keys)}
              selectedKeys={[]}
              treeData={convertToTreeData(filteredTree)}
              onSelect={handleTreeSelect}
              className={styles.tree}
            />
          ) : (
            <Empty description="暂无分类数据" />
          )}
        </Spin>
      </Card>

      {/* 分类表单模态框 */}
      <Modal
        title={currentCategory?.id ? '编辑分类' : '新增分类'}
        open={categoryModalVisible}
        onOk={handleCategorySubmit}
        onCancel={() => setCategoryModalVisible(false)}
        width={600}
        destroyOnClose
      >
        <Form form={form} layout="vertical">
          {!currentCategory?.id && (
            <Form.Item
              label="分类别名"
              name="categoryAlias"
              tooltip="一级分类需设置别名，用于区分不同权限树"
              rules={[
                {
                  required: !currentCategory,
                  message: '一级分类必须设置别名',
                },
              ]}
            >
              <Input placeholder="如：app_type_a" disabled={!!currentCategory?.parentId} />
            </Form.Item>
          )}

          <Form.Item label="父分类ID" name="parentId" hidden={!!currentCategory?.id}>
            <Input disabled />
          </Form.Item>

          <Form.Item
            label="中文名称"
            name="nameCn"
            rules={[{ required: true, message: '请输入中文名称' }]}
          >
            <Input placeholder="请输入中文名称" />
          </Form.Item>

          <Form.Item
            label="英文名称"
            name="nameEn"
            rules={[{ required: true, message: '请输入英文名称' }]}
          >
            <Input placeholder="请输入英文名称" />
          </Form.Item>

          <Form.Item label="排序" name="sortOrder" initialValue={0}>
            <InputNumber min={0} placeholder="排序序号，数字越小越靠前" style={{ width: '100%' }} />
          </Form.Item>
        </Form>
      </Modal>

      {/* 责任人管理模态框 */}
      <Modal
        title="管理责任人"
        open={ownerModalVisible}
        onCancel={() => setOwnerModalVisible(false)}
        footer={null}
        width={800}
      >
        <div className={styles.ownerSection}>
          <Card title="添加责任人" size="small" style={{ marginBottom: 16 }}>
            <Form form={ownerForm} layout="inline">
              <Form.Item
                name="userId"
                label="用户ID"
                rules={[{ required: true, message: '请输入用户ID' }]}
              >
                <Input placeholder="请输入用户ID" style={{ width: 200 }} />
              </Form.Item>
              <Form.Item name="userName" label="用户名称">
                <Input placeholder="请输入用户名称" style={{ width: 200 }} />
              </Form.Item>
              <Form.Item>
                <Button type="primary" onClick={handleAddOwnerSubmit}>
                  添加
                </Button>
              </Form.Item>
            </Form>
          </Card>

          <Card title="责任人列表" size="small">
            <Table
              dataSource={owners}
              columns={ownerColumns}
              rowKey="id"
              pagination={false}
              size="small"
            />
          </Card>
        </div>
      </Modal>
    </div>
  );
};

export default CategoryList;
