import React, { useState, useEffect } from 'react';
import {
  Button,
  Table,
  Tag,
  Space,
  Input,
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

  useEffect(() => {
    loadData();
  }, []);

  const loadData = async () => {
    setLoading(true);
    const result = await fetchCallbackList({ keyword });
    if (result.code === '200') {
      setCallbackList(result.data);
      setTotal(result.page?.total || 0);
    }
    setLoading(false);
  };

  const handleSearch = () => {
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
      title: '回调路径',
      dataIndex: 'path',
      key: 'path',
      render: (text) => <code>{text}</code>,
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
          <Button type="link" size="small" icon={<EyeOutlined />}>
            详情
          </Button>
          <Button type="link" size="small" icon={<EditOutlined />}>
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
        <Button type="primary" icon={<PlusOutlined />}>
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
    </div>
  );
}

export default CallbackList;