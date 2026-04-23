import React, { useState, useEffect } from 'react';
import {
  Button,
  Table,
  Tag,
  Space,
  Input,
  Select,
  TreeSelect,
  Popconfirm,
  Empty,
  Spin,
} from 'antd';
import {
  PlusOutlined,
  EditOutlined,
  DeleteOutlined,
  EyeOutlined,
} from '@ant-design/icons';
import { fetchApiList, deleteApi } from './thunk';
import { fetchCategoryTree } from '../Category/thunk';
import ApiRegister from './ApiRegister';
import './ApiList.m.less';

const { Search } = Input;

const STATUS_MAP = {
  0: { text: '草稿', color: 'default' },
  1: { text: '待审', color: 'orange' },
  2: { text: '已发布', color: 'green' },
  3: { text: '已下线', color: 'red' },
};

function ApiList() {
  const [loading, setLoading] = useState(false);
  const [apiList, setApiList] = useState([]);
  const [total, setTotal] = useState(0);
  const [keyword, setKeyword] = useState('');
  const [categoryId, setCategoryId] = useState('');
  const [status, setStatus] = useState('');
  const [modalVisible, setModalVisible] = useState(false);
  const [currentApi, setCurrentApi] = useState(null);
  const [categories, setCategories] = useState([]);

  useEffect(() => {
    loadData();
    loadCategories();
  }, []);

  const loadCategories = async () => {
    const result = await fetchCategoryTree();
    if (result.code === '200') {
      setCategories(result.data || []);
    }
  };

  // 将后端返回的分类树数据转换为 TreeSelect 所需格式
  const convertToTreeData = (categories) => {
    if (!categories) return [];
    return categories.map(cat => ({
      value: cat.id,
      title: cat.nameCn,
      key: cat.id,
      children: cat.children ? convertToTreeData(cat.children) : undefined
    }));
  };

  const loadData = async (params = {}) => {
    setLoading(true);
    const result = await fetchApiList({
      keyword,
      categoryId,
      status,
      ...params,
    });
    if (result.code === '200') {
      setApiList(result.data);
      setTotal(result.page?.total || 0);
    }
    setLoading(false);
  };

  const handleSearch = () => {
    loadData();
  };

  const handleAdd = () => {
    setCurrentApi(null);
    setModalVisible(true);
  };

  const handleEdit = (record) => {
    setCurrentApi({ id: record.id });
    setModalVisible(true);
  };

  const handleView = (record) => {
    setCurrentApi({ id: record.id });
    setModalVisible(true);
  };

  const handleDelete = async (id) => {
    await deleteApi(id);
    loadData();
  };

  const handleSuccess = () => {
    setModalVisible(false);
    loadData();
  };

  const columns = [
    {
      title: 'API名称',
      dataIndex: 'nameCn',
      key: 'nameCn',
      render: (text, record) => (
        <div>
          <div>{text}</div>
          <div style={{ fontSize: 12, color: '#999' }}>{record.nameEn}</div>
        </div>
      ),
    },
    {
      title: '分类',
      dataIndex: 'categoryName',
      key: 'categoryName',
    },
    {
      title: '路径',
      dataIndex: 'path',
      key: 'path',
      render: (text) => <code>{text}</code>,
    },
    {
      title: '方法',
      dataIndex: 'method',
      key: 'method',
      render: (method) => <Tag color="blue">{method}</Tag>,
    },
    {
      title: '状态',
      dataIndex: 'status',
      key: 'status',
      render: (status) => {
        const { text, color } = STATUS_MAP[status] || STATUS_MAP[0];
        return <Tag color={color}>{text}</Tag>;
      },
    },
    {
      title: '操作',
      key: 'action',
      render: (_, record) => (
        <Space>
          <Button type="link" size="small" icon={<EyeOutlined />} onClick={() => handleView(record)}>
            详情
          </Button>
          <Button type="link" size="small" icon={<EditOutlined />} onClick={() => handleEdit(record)}>
            编辑
          </Button>
          <Popconfirm
            title="确定删除该API吗？"
            onConfirm={() => handleDelete(record.id)}
            okText="确定"
            cancelText="取消"
          >
            <Button type="link" size="small" danger icon={<DeleteOutlined />}>
              删除
            </Button>
          </Popconfirm>
        </Space>
      ),
    },
  ];

  return (
    <div className="api-list">
      <div className="page-header">
        <div className="page-header-left">
          <h4 className="page-title">API管理</h4>
          <span className="page-desc">管理API接口，配置API权限</span>
        </div>
        <Button type="primary" icon={<PlusOutlined />} onClick={handleAdd}>
          注册API
        </Button>
      </div>

      <div className="toolbar">
        <Search
            placeholder="搜索API名称"
            value={keyword}
            onChange={(e) => setKeyword(e.target.value)}
            style={{ width: 200 }}
            onSearch={handleSearch}
          />
          <TreeSelect
            placeholder="选择分类"
            value={categoryId}
            onChange={setCategoryId}
            treeData={convertToTreeData(categories)}
            treeDefaultExpandAll
            allowClear
            style={{ width: 150 }}
            dropdownStyle={{ maxHeight: 400, overflow: 'auto' }}
          />
          <Select
            placeholder="选择状态"
            value={status}
            onChange={setStatus}
            style={{ width: 120 }}
            allowClear
          >
            <Select.Option value={0}>草稿</Select.Option>
            <Select.Option value={2}>已发布</Select.Option>
            <Select.Option value={3}>已下线</Select.Option>
          </Select>
        </div>

        <Spin spinning={loading}>
          {apiList.length > 0 ? (
            <Table
              columns={columns}
              dataSource={apiList}
              rowKey="id"
              pagination={{
                total,
                pageSize: 20,
                onChange: (page) => loadData({ curPage: page }),
              }}
            />
          ) : (
            <Empty description="暂无API数据" />
          )}
        </Spin>

      <ApiRegister
        visible={modalVisible}
        api={currentApi}
        onSuccess={handleSuccess}
        onCancel={() => setModalVisible(false)}
      />
    </div>
  );
}

export default ApiList;