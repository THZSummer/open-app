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
  Pagination,
} from 'antd';
import {
  PlusOutlined,
  EditOutlined,
  DeleteOutlined,
  EyeOutlined,
  FileTextOutlined,
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

const AUTH_TYPE_MAP = {
  0: 'Cookie',
  1: 'SOA',
  2: 'APIG',
  3: 'IAM',
  4: '免认证',
  5: 'AKSK',
  6: 'CLITOKEN',
};

function ApiList() {
  const [loading, setLoading] = useState(false);
  const [apiList, setApiList] = useState([]);
  const [total, setTotal] = useState(0);
  const [currentPage, setCurrentPage] = useState(1);
  const [pageSize, setPageSize] = useState(10);
  const [keyword, setKeyword] = useState('');
  const [categoryId, setCategoryId] = useState(undefined);
  const [status, setStatus] = useState(undefined);
  const [modalVisible, setModalVisible] = useState(false);
  const [currentApi, setCurrentApi] = useState(null);
  const [categories, setCategories] = useState([]);
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
    const finalPage = 'curPage' in params ? params.curPage : currentPage;
    const finalSize = 'pageSize' in params ? params.pageSize : pageSize;
    
    const requestParams = {
      keyword: finalKeyword,
      categoryId: finalCategoryId,
      status: finalStatus,
      curPage: finalPage,
      pageSize: finalSize,
    };
    
    // 过滤掉值为 undefined 的参数
    const filteredParams = Object.fromEntries(
      Object.entries(requestParams).filter(([_, value]) => value !== undefined)
    );

    const result = await fetchApiList(filteredParams);
    if (result.code === '200') {
      setApiList(result.data);
      setTotal(result.page?.total || 0);
    }
    setLoading(false);
  };

  const handleSearch = () => {
    setCurrentPage(1);
    loadData({ curPage: 1 });
  };

  const handlePageChange = (page, size) => {
    setCurrentPage(page);
    setPageSize(size);
    loadData({ curPage: page, pageSize: size });
  };

  const handleAdd = () => {
    setCurrentApi(null);
    setMode('create');
    setModalVisible(true);
  };

  const handleEdit = (record) => {
    setCurrentApi({ id: record.id });
    setMode('edit');
    setModalVisible(true);
  };

  const handleView = (record) => {
    setCurrentApi({ id: record.id });
    setMode('view');
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
      title: '认证方式',
      dataIndex: 'authType',
      key: 'authType',
      render: (authType) => {
        const label = AUTH_TYPE_MAP[authType] || 'SOA';
        return <Tag color="purple">{label}</Tag>;
      },
    },
    {
      title: 'Scope',
      dataIndex: 'permission',
      key: 'scope',
      render: (permission) => {
        const scope = permission?.scope || '-';
        return <Tag color="cyan">{scope}</Tag>;
      },
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
          {record.docUrl && (
            <Button 
              type="link" 
              size="small" 
              icon={<FileTextOutlined />}
              onClick={() => window.open(record.docUrl, '_blank')}
            >
              文档
            </Button>
          )}
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
            <Select.Option value={1}>待审</Select.Option>
            <Select.Option value={2}>已发布</Select.Option>
            <Select.Option value={3}>已下线</Select.Option>
          </Select>
        </div>

        <Spin spinning={loading}>
          {apiList.length > 0 ? (
            <>
              <Table
                columns={columns}
                dataSource={apiList}
                rowKey="id"
                pagination={false}
              />
              <div style={{ marginTop: 16, textAlign: 'right' }}>
                <Pagination
                  total={total}
                  current={currentPage}
                  pageSize={pageSize}
                  pageSizeOptions={[10, 20, 50]}
                  showSizeChanger
                  showQuickJumper
                  showTotal={(total) => `共 ${total} 条`}
                  onChange={handlePageChange}
                />
              </div>
            </>
          ) : (
            <Empty description="暂无API数据" />
          )}
        </Spin>

      <ApiRegister
        visible={modalVisible}
        api={currentApi}
        mode={mode}
        onSuccess={handleSuccess}
        onCancel={() => setModalVisible(false)}
      />
    </div>
  );
}

export default ApiList;