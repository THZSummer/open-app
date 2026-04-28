import React, { useState, useEffect } from 'react';
import {
  Button,
  Tree,
  Form,
  Input,
  Space,
  Tag,
  Empty,
  Spin,
  message,
} from 'antd';
import {
  EditOutlined,
  DeleteOutlined,
  UserOutlined,
  FolderOutlined,
  FileOutlined,
  PlusOutlined,
} from '@ant-design/icons';
import { useNavigate } from 'react-router-dom';
import {
  fetchCategoryTree,
  fetchCategoryOwners,
  createCategory,
  updateCategory,
  deleteCategory,
  addCategoryOwner,
  removeCategoryOwner,
} from './thunk';
import CategoryFormModal from '../../../components/CategoryFormModal/CategoryFormModal';
import CategoryOwnerModal from '../../../components/CategoryOwnerModal/CategoryOwnerModal';
import DeleteConfirmModal from '../../../components/DeleteConfirmModal/DeleteConfirmModal';
import { isInAdminWhitelist } from '../../../utils/common';
import SimpleSidebar from '../../../components/SimpleSidebar/SimpleSidebar';
import './CategoryList.m.less';

function CategoryList() {
  const navigate = useNavigate();

  useEffect(() => {
    if (!isInAdminWhitelist()) {
      navigate('/apps');
    }
  }, [navigate]);

  const [loading, setLoading] = useState(false);
  const [categoryTree, setCategoryTree] = useState([]);
  const [searchValue, setSearchValue] = useState('');
  const [expandedKeys, setExpandedKeys] = useState([]);
  const [formModalVisible, setFormModalVisible] = useState(false);
  const [ownerModalVisible, setOwnerModalVisible] = useState(false);
  const [editingCategory, setEditingCategory] = useState(null);
  const [parentCategory, setParentCategory] = useState(null);
  const [ownerCategoryId, setOwnerCategoryId] = useState('');
  const [deleteModalVisible, setDeleteModalVisible] = useState(false);
  const [deleteCategoryId, setDeleteCategoryId] = useState('');
  const [deleteCategoryName, setDeleteCategoryName] = useState('');

  const { Search: AntSearch } = Input;

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

  const handleAddRoot = () => {
    setEditingCategory(null);
    setParentCategory(null);
    setFormModalVisible(true);
  };

  const handleAddChild = (parent) => {
    setEditingCategory(null);
    setParentCategory(parent);
    setFormModalVisible(true);
  };

  const handleEdit = (category) => {
    setEditingCategory(category);
    setParentCategory(null);
    setFormModalVisible(true);
  };

  const handleManageOwners = (category) => {
    setOwnerCategoryId(category.id);
    setOwnerModalVisible(true);
  };

  const handleDeleteClick = (category) => {
    setDeleteCategoryId(category.id);
    setDeleteCategoryName(category.nameCn);
    setDeleteModalVisible(true);
  };

  const handleConfirmDelete = async () => {
    const res = await deleteCategory(deleteCategoryId);
    if (res && res.code === '200') {
      message.success('删除成功');
      setDeleteModalVisible(false);
      loadData();
    } else {
      message.error(res?.message || '删除失败');
    }
  };

  const handleFormSubmit = async (data) => {
    let result;
    if (editingCategory) {
      result = await updateCategory(editingCategory.id, {
        nameCn: data.nameCn,
        nameEn: data.nameEn,
        sortOrder: data.sortOrder,
      });
      if (result && result.code === '200') {
        message.success('更新成功');
        setFormModalVisible(false);
        loadData();
      } else {
        message.error(result?.message || '更新失败');
      }
    } else {
      result = await createCategory({
        categoryAlias: data.categoryAlias,
        nameCn: data.nameCn,
        nameEn: data.nameEn,
        parentId: data.parentId,
        sortOrder: data.sortOrder,
      });
      if (result && result.code === '200') {
        message.success('创建成功');
        setFormModalVisible(false);
        loadData();
      } else {
        message.error(result?.message || '创建失败');
      }
    }
  };

  const handleFormClose = () => {
    setFormModalVisible(false);
    setEditingCategory(null);
    setParentCategory(null);
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
                handleDeleteClick(category);
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

  return (
    <div style={{ display: 'flex', height: '100vh', overflow: 'hidden' }}>
      <SimpleSidebar />
      <div style={{ flex: 1, overflow: 'auto' }}>
        <div className="category-list">
          <div className="page-header">
            <div className="page-header-left">
              <h4 className="page-title">分类管理</h4>
              <span className="page-desc">管理分类树结构，配置分类责任人</span>
            </div>
            <Button type="primary" onClick={handleAddRoot} style={{ justifyContent: 'center', borderRadius: 6 }}>
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

          <CategoryFormModal
            visible={formModalVisible}
            isEditing={!!editingCategory}
            parentCategory={parentCategory}
            initialValues={editingCategory}
            onClose={handleFormClose}
            onSubmit={handleFormSubmit}
          />

          <CategoryOwnerModal
            visible={ownerModalVisible}
            categoryId={ownerCategoryId}
            onClose={() => setOwnerModalVisible(false)}
            fetchOwners={fetchCategoryOwners}
            addOwner={addCategoryOwner}
            removeOwner={removeCategoryOwner}
          />

          <DeleteConfirmModal
            open={deleteModalVisible}
            onClose={() => setDeleteModalVisible(false)}
            onConfirm={handleConfirmDelete}
            requireConfirmText={deleteCategoryName}
            title="删除分类"
            content="此操作将永久删除该分类及其所有子分类，无法恢复！"
          />
        </div>
      </div>
    </div>
  );
}

export default CategoryList;
