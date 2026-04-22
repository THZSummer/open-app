import { useEffect, useState } from 'react';
import { Card, Button, Table, Space, Tag, Input, Select, Popconfirm, Badge } from 'antd';
import { PlusOutlined, EditOutlined, DeleteOutlined, EyeOutlined, UndoOutlined } from '@ant-design/icons';
import type { ColumnsType } from 'antd/es/table';
import { useCallbackManager, useCategory } from '@/hooks';
import { Callback } from '@/services/callback.service';
import CallbackForm from './CallbackForm';
import styles from './CallbackList.module.less';

const { Search } = Input;

const statusMap: Record<number, { color: string; text: string }> = {
  0: { color: 'default', text: '草稿' },
  1: { color: 'processing', text: '待审' },
  2: { color: 'success', text: '已发布' },
  3: { color: 'error', text: '已下线' },
};

const CallbackList: React.FC = () => {
  const { loading, callbackList, total, fetchCallbackList, handleDeleteCallback, handleWithdrawCallback } = useCallbackManager();
  const { categoryTree, fetchCategoryTree } = useCategory();

  const [formVisible, setFormVisible] = useState(false);
  const [currentCallback, setCurrentCallback] = useState<Callback | null>(null);
  const [searchParams, setSearchParams] = useState({
    keyword: '',
    categoryId: '',
    status: undefined as number | undefined,
    curPage: 1,
    pageSize: 20,
  });

  useEffect(() => {
    fetchCategoryTree();
    fetchCallbackList(searchParams);
  }, []);

  const refreshList = () => fetchCallbackList(searchParams);

  const columns: ColumnsType<Callback> = [
    {
      title: '回调名称',
      dataIndex: 'nameCn',
      key: 'nameCn',
      width: 200,
      render: (text, record) => (
        <div>
          <div>{text}</div>
          <div style={{ color: '#999', fontSize: 12 }}>{record.nameEn}</div>
        </div>
      ),
    },
    {
      title: '所属分类',
      dataIndex: 'categoryName',
      key: 'categoryName',
      width: 150,
    },
    {
      title: 'Scope',
      dataIndex: ['permission', 'scope'],
      key: 'scope',
      width: 200,
      render: (scope) => <Tag>{scope}</Tag>,
    },
    {
      title: '状态',
      dataIndex: 'status',
      key: 'status',
      width: 100,
      render: (status) => {
        const { color, text } = statusMap[status] || statusMap[0];
        return <Badge status={color as any} text={text} />;
      },
    },
    {
      title: '操作',
      key: 'action',
      width: 200,
      fixed: 'right',
      render: (_, record) => (
        <Space size="small">
          <Button type="link" size="small" icon={<EyeOutlined />} onClick={() => console.log('查看详情:', record)}>详情</Button>
          <Button type="link" size="small" icon={<EditOutlined />} onClick={() => { setCurrentCallback(record); setFormVisible(true); }}>编辑</Button>
          {record.status === 1 && (
            <Button type="link" size="small" icon={<UndoOutlined />} onClick={() => handleWithdrawCallback(record.id).then(() => refreshList())}>撤回</Button>
          )}
          {record.status !== 2 && (
            <Popconfirm title="确定删除该回调吗？" onConfirm={() => handleDeleteCallback(record.id).then(() => refreshList())} okText="确定" cancelText="取消">
              <Button type="link" size="small" danger icon={<DeleteOutlined />}>删除</Button>
            </Popconfirm>
          )}
        </Space>
      ),
    },
  ];

  const flattenCategories = (categories: any[], level = 0): any[] => {
    return categories.reduce((acc, cat) => {
      const prefix = '—'.repeat(level);
      acc.push({ label: `${prefix}${prefix ? ' ' : ''}${cat.nameCn}`, value: cat.id });
      if (cat.children && cat.children.length > 0) acc.push(...flattenCategories(cat.children, level + 1));
      return acc;
    }, []);
  };

  return (
    <div className={styles.container}>
      <Card title="回调管理" extra={<Button type="primary" icon={<PlusOutlined />} onClick={() => { setCurrentCallback(null); setFormVisible(true); }}>注册回调</Button>}>
        <div className={styles.toolbar}>
          <Space size="middle">
            <Search placeholder="搜索回调名称、Scope" value={searchParams.keyword} onChange={(e) => setSearchParams({ ...searchParams, keyword: e.target.value })} onSearch={() => fetchCallbackList(searchParams)} style={{ width: 300 }} allowClear />
            <Select placeholder="选择分类" value={searchParams.categoryId} onChange={(value) => setSearchParams({ ...searchParams, categoryId: value, curPage: 1 })} style={{ width: 200 }} allowClear options={flattenCategories(categoryTree)} />
            <Select placeholder="选择状态" value={searchParams.status} onChange={(value) => setSearchParams({ ...searchParams, status: value, curPage: 1 })} style={{ width: 150 }} allowClear>
              <Select.Option value={0}>草稿</Select.Option>
              <Select.Option value={1}>待审</Select.Option>
              <Select.Option value={2}>已发布</Select.Option>
              <Select.Option value={3}>已下线</Select.Option>
            </Select>
            <Button type="primary" onClick={() => fetchCallbackList(searchParams)}>查询</Button>
          </Space>
        </div>
        <Table columns={columns} dataSource={callbackList} rowKey="id" loading={loading} pagination={{ current: searchParams.curPage, pageSize: searchParams.pageSize, total, showSizeChanger: true, showTotal: (total) => `共 ${total} 条`, onChange: (curPage, pageSize) => { setSearchParams({ ...searchParams, curPage, pageSize }); fetchCallbackList({ ...searchParams, curPage, pageSize }); } }} scroll={{ x: 1000 }} />
      </Card>
      <CallbackForm visible={formVisible} callback={currentCallback} categoryTree={categoryTree} onSuccess={() => { setFormVisible(false); refreshList(); }} onCancel={() => setFormVisible(false)} />
    </div>
  );
};

export default CallbackList;
