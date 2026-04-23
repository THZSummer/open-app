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
import { fetchCallbackList, deleteCallback } from './thunk';
import { fetchCategoryTree } from '../Category/thunk';
import CallbackRegister from './CallbackRegister';
import './CallbackList.m.less';

const { Search } = Input;

const STATUS_MAP = {
  0: { text: '草稿', color: 'default' },
  2: { text: '已发布', color: 'green' },
};

function CallbackList() {
  const [loading, setLoading] = useState(false);
  const [callbackList, setCallbackList] = useState([]);
  const [total, setTotal] = useState(0);
  const [keyword, setKeyword] = useState('');
  const [categoryId, setCategoryId] = useState('');
  const [status, setStatus] = useState('');
  const [categories, setCategories] = useState([]);
  const [modalVisible, setModalVisible] = useState(false);
  const [currentCallback, setCurrentCallback] = useState(null);
  const [mode, setMode] = useState('create');

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
    // 使用 'key' in params 区分"传入 undefined 表示清除"和"没有传这个参数"
    const finalKeyword = 'keyword' in params ? params.keyword : keyword;
    const finalCategoryId = 'categoryId' in params ? params.categoryId : categoryId;
    const finalStatus = 'status' in params ? params.status : status;
    
    const requestParams = {
      keyword: finalKeyword,
      categoryId: finalCategoryId,
      status: finalStatus,
    };
    
    // 只有当 curPage 有值时才添加
    if (params.curPage !== undefined) {
      requestParams.curPage = params.curPage;
    }
    
    // 过滤掉值为 undefined 的参数
    const filteredParams = Object.fromEntries(
      Object.entries(requestParams).filter(([_, value]) => value !== undefined)
    );

    const result = await fetchCallbackList(filteredParams);
    if (result.code === '200') {
      setCallbackList(result.data);
      setTotal(result.page?.total || 0);
    }
    setLoading(false);
  };

  const handleSearch = () => {
    loadData();
  };

  const handleAdd = () => {
    setCurrentCallback(null);
    setMode('create');
    setModalVisible(true);
  };

  const handleEdit = (record) => {
    setCurrentCallback({ id: record.id });
    setMode('edit');
    setModalVisible(true);
  };

  const handleView = (record) => {
    setCurrentCallback({ id: record.id });
    setMode('view');
    setModalVisible(true);
  };

  const handleSuccess = () => {
    setModalVisible(false);
    loadData();
  };

  const handleDelete = async (id) => {
    await deleteCallback(id);
    loadData();
  };

  const columns = [
    {
      title: '回调名称',
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
            title="确定删除该回调吗？"
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
    <div className="callback-list">
      <div className="page-header">
        <div className="page-header-left">
          <h4 className="page-title">回调管理</h4>
          <span className="page-desc">管理回调接口，配置回调地址</span>
        </div>
        <Button type="primary" icon={<PlusOutlined />} onClick={handleAdd}>
          注册回调
        </Button>
      </div>

      <div className="toolbar">
        <Search
            placeholder="搜索回调名称"
            value={keyword}
            onChange={(e) => setKeyword(e.target.value)}
            style={{ width: 200 }}
            onSearch={handleSearch}
          />
          <TreeSelect
            placeholder="选择分类"
            value={categoryId}
            onChange={(value) => {
              setCategoryId(value);
              loadData({ categoryId: value });
            }}
            treeData={convertToTreeData(categories)}
            treeDefaultExpandAll
            allowClear
            style={{ width: 150 }}
            dropdownStyle={{ maxHeight: 400, overflow: 'auto' }}
          />
          <Select
            placeholder="选择状态"
            value={status}
            onChange={(value) => {
              setStatus(value);
              loadData({ status: value });
            }}
            style={{ width: 120 }}
            allowClear
          >
            <Select.Option value={0}>草稿</Select.Option>
            <Select.Option value={2}>已发布</Select.Option>
          </Select>
        </div>

        <Spin spinning={loading}>
          {callbackList.length > 0 ? (
            <Table
              columns={columns}
              dataSource={callbackList}
              rowKey="id"
              pagination={{
                total,
                pageSize: 20,
                onChange: (page) => loadData({ curPage: page }),
              }}
            />
          ) : (
            <Empty description="暂无回调数据" />
          )}
        </Spin>

      <CallbackRegister
        visible={modalVisible}
        callback={currentCallback}
        mode={mode}
        onSuccess={handleSuccess}
        onCancel={() => setModalVisible(false)}
      />
    </div>
  );
}

export default CallbackList;