import React, { useState, useEffect } from 'react';
import {
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
  message,
} from 'antd';
import {
  PlusOutlined,
  EditOutlined,
  DeleteOutlined,
  UserOutlined,
  FolderOutlined,
  FileOutlined,
} from '@ant-design/icons';
import {
  fetchCategoryTree,
  fetchCategoryOwners,
  createCategory,
  updateCategory,
  deleteCategory,
  addCategoryOwner,
  removeCategoryOwner,
} from './thunk';
import './CategoryList.m.less';

const { Search: AntSearch } = Input;

function CategoryList() {
  const [form] = Form.useForm();
  const [ownerForm] = Form.useForm();
  const [loading, setLoading] = useState(false);
  const [categoryTree, setCategoryTree] = useState([]);
  const [owners, setOwners] = useState([]);
  const [searchValue, setSearchValue] = useState('');
  const [modalVisible, setModalVisible] = useState(false);
  const [ownerModalVisible, setOwnerModalVisible] = useState(false);
  const [currentCategory, setCurrentCategory] = useState(null);
  const [selectedCategoryId, setSelectedCategoryId] = useState('');
  const [expandedKeys, setExpandedKeys] = useState([]);
  const [isEditing, setIsEditing] = useState(false);
  const [deleteModalVisible, setDeleteModalVisible] = useState(false);
  const [deleteCategoryData, setDeleteCategoryData] = useState(null);
  const [deleteConfirmAlias, setDeleteConfirmAlias] = useState('');

  useEffect(() => {
    loadData();
  }, []);

  const loadData = async () => {
    setLoading(true);
    const result = await fetchCategoryTree();
    if (result.code === '200') {
      setCategoryTree(result.data);
    }
    setLoading(false);
  };

  const convertToTreeData = (categories) => {
    return categories.map((category) => ({
      key: category.id,
      title: (
        <div className="tree-node">
          <span className="tree-node-title">
            {category.categoryAlias && <Tag color="blue">{category.categoryAlias}</Tag>}
            {category.nameCn}
          </span>
          <Space className="tree-node-actions">
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
            <Button
              type="link"
              size="small"
              danger
              icon={<DeleteOutlined />}
              onClick={(e) => {
                e.stopPropagation();
                setDeleteCategoryData(category);
                setDeleteConfirmAlias('');
                setDeleteModalVisible(true);
              }}
            >
              删除
            </Button>
          </Space>
        </div>
      ),
      icon: category.children && category.children.length > 0 ? <FolderOutlined /> : <FileOutlined />,
      children: category.children ? convertToTreeData(category.children) : undefined,
    }));
  };

  const handleAddRoot = () => {
    setCurrentCategory(null);
    setIsEditing(false);
    form.resetFields();
    setModalVisible(true);
  };

  const handleAddChild = (parent) => {
    setCurrentCategory(parent);
    setIsEditing(false);
    form.resetFields();
    form.setFieldsValue({ parentId: parent.id });
    setModalVisible(true);
  };

  const handleEdit = (category) => {
    setCurrentCategory(category);
    setIsEditing(true);
    form.setFieldsValue({
      categoryAlias: category.categoryAlias,
      nameCn: category.nameCn,
      nameEn: category.nameEn,
      sortOrder: category.sortOrder,
    });
    setModalVisible(true);
  };

  const handleDelete = async (id) => {
    const res = await deleteCategory(id);
    if (res && res.code === '200') {
      message.success('删除成功');
      loadData();
    } else {
      message.error(res?.message || '删除失败');
    }
  };

  const handleSubmit = async () => {
    const values = await form.validateFields();
    let result;
    if (isEditing) {
      result = await updateCategory(currentCategory.id, {
        nameCn: values.nameCn,
        nameEn: values.nameEn,
        sortOrder: values.sortOrder,
      });
      if (result && result.code === '200') {
        message.success('更新成功');
        setModalVisible(false);
        loadData();
      } else {
        message.error(result?.message || '更新失败');
      }
    } else {
      result = await createCategory({
        categoryAlias: values.categoryAlias,
        nameCn: values.nameCn,
        nameEn: values.nameEn,
        parentId: values.parentId,
        sortOrder: values.sortOrder,
      });
      if (result && result.code === '200') {
        message.success('创建成功');
        setModalVisible(false);
        loadData();
      } else {
        message.error(result?.message || '创建失败');
      }
    }
  };

  const handleManageOwners = async (category) => {
    setSelectedCategoryId(category.id);
    const result = await fetchCategoryOwners(category.id);
    if (result.code === '200') {
      setOwners(result.data);
    }
    ownerForm.resetFields();
    setOwnerModalVisible(true);
  };

  const handleAddOwner = async () => {
    const values = await ownerForm.validateFields();
    const res = await addCategoryOwner(selectedCategoryId, {
      userId: values.userId,
      userName: values.userName,
    });
    if (res && res.code === '200') {
      message.success('添加成功');
      ownerForm.resetFields();
      const result = await fetchCategoryOwners(selectedCategoryId);
      if (result.code === '200') {
        setOwners(result.data);
      }
    } else {
      message.error(res?.message || '添加失败');
    }
  };

  const handleRemoveOwner = async (userId) => {
    const res = await removeCategoryOwner(selectedCategoryId, userId);
    if (res && res.code === '200') {
      message.success('移除成功');
      const result = await fetchCategoryOwners(selectedCategoryId);
      if (result.code === '200') {
        setOwners(result.data);
      }
    } else {
      message.error(res?.message || '移除失败');
    }
  };

  const filterTree = (data, search) => {
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
      .filter(Boolean);
  };

  const filteredTree = filterTree(categoryTree, searchValue);

  const ownerColumns = [
    { title: '用户ID', dataIndex: 'userId', key: 'userId' },
    { title: '用户名称', dataIndex: 'userName', key: 'userName' },
    {
      title: '操作',
      key: 'action',
      render: (_, record) => (
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
    <div className="category-list">
      <div className="page-header">
        <div className="page-header-left">
          <h4 className="page-title">分类管理</h4>
          <span className="page-desc">管理分类树结构，配置分类责任人</span>
        </div>
        <Button type="primary" icon={<PlusOutlined />} onClick={handleAddRoot}>
          新增一级分类
        </Button>
      </div>

      <div className="toolbar">
        <AntSearch
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
            className="category-tree"
          />
        ) : (
          <Empty description="暂无分类数据" />
        )}
      </Spin>

      <Modal
        title={isEditing ? '编辑分类' : (currentCategory ? '新增子分类' : '新增一级分类')}
        open={modalVisible}
        onOk={handleSubmit}
        onCancel={() => setModalVisible(false)}
        width={600}
        destroyOnClose
      >
        <Form form={form} layout="vertical">
          {!currentCategory?.id && (
            <Form.Item
              label="分类别名"
              name="categoryAlias"
              tooltip="一级分类需设置别名，用于区分不同权限树"
              rules={[{ required: true, message: '一级分类必须设置别名' }]}
            >
              <Input placeholder="如：app_type_a" disabled={!!currentCategory?.parentId} />
            </Form.Item>
          )}

          <Form.Item label="父分类ID" name="parentId" hidden>
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
            <InputNumber min={0} placeholder="排序序号" style={{ width: '100%' }} />
          </Form.Item>
        </Form>
      </Modal>

      <Modal
        title="管理责任人"
        open={ownerModalVisible}
        onCancel={() => setOwnerModalVisible(false)}
        footer={null}
        width={800}
      >
        <div className="owner-section">
          <div className="section-header" style={{ marginBottom: 16 }}>
            <h4>添加责任人</h4>
          </div>
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
              <Button type="primary" onClick={handleAddOwner}>
                添加
              </Button>
            </Form.Item>
          </Form>

          <div className="section-header" style={{ marginTop: 16, marginBottom: 16 }}>
            <h4>责任人列表</h4>
          </div>
          <Table
            dataSource={owners}
            columns={ownerColumns}
            rowKey="id"
            pagination={false}
            size="small"
          />
        </div>
      </Modal>

      <Modal
        title="删除确认"
        open={deleteModalVisible}
        onOk={() => {
          if (deleteConfirmAlias === deleteCategoryData?.nameCn) {
            handleDelete(deleteCategoryData.id);
            setDeleteModalVisible(false);
          }
        }}
        onCancel={() => setDeleteModalVisible(false)}
        okText="确认删除"
        cancelText="取消"
        okButtonProps={{
          danger: true,
          disabled: deleteConfirmAlias !== deleteCategoryData?.nameCn
        }}
      >
        <div style={{ marginBottom: 16 }}>
          确定要删除分类 <strong>"{deleteCategoryData?.nameCn}"</strong> 吗？
        </div>
        <div style={{ marginBottom: 8 }}>
          请输入分类名称 <strong>"{deleteCategoryData?.nameCn}"</strong> 以确认删除：
        </div>
        <Input
          placeholder={`请输入 ${deleteCategoryData?.nameCn}`}
          value={deleteConfirmAlias}
          onChange={(e) => setDeleteConfirmAlias(e.target.value)}
        />
        {deleteConfirmAlias && deleteConfirmAlias !== deleteCategoryData?.nameCn && (
          <div style={{ color: 'red', marginTop: 8 }}>名称输入错误，请重新输入</div>
        )}
      </Modal>
    </div>
  );
}

export default CategoryList;
