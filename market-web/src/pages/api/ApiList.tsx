import { useEffect, useState } from 'react';
import {
  Card,
  Button,
  Table,
  Space,
  Tag,
  Input,
  Select,
  Popconfirm,
  Badge,
} from 'antd';
import {
  PlusOutlined,
  EditOutlined,
  DeleteOutlined,
  EyeOutlined,
  UndoOutlined,
} from '@ant-design/icons';
import type { ColumnsType } from 'antd/es/table';
import { useApi, useCategory } from '@/hooks';
import { Api } from '@/services/api.service';
import ApiForm from './ApiForm';
import styles from './ApiList.module.less';

const { Search } = Input;

/**
 * API 状态映射
 */
const statusMap: Record<number, { color: string; text: string }> = {
  0: { color: 'default', text: '草稿' },
  1: { color: 'processing', text: '待审' },
  2: { color: 'success', text: '已发布' },
  3: { color: 'error', text: '已下线' },
};

/**
 * HTTP 方法颜色映射
 */
const methodColorMap: Record<string, string> = {
  GET: 'green',
  POST: 'blue',
  PUT: 'orange',
  DELETE: 'red',
  PATCH: 'cyan',
};

/**
 * API 管理列表页
 */
const ApiList: React.FC = () => {
  const {
    loading,
    apiList,
    total,
    fetchApiList,
    handleDeleteApi,
    handleWithdrawApi,
  } = useApi();
  const { categoryTree, fetchCategoryTree } = useCategory();

  const [formVisible, setFormVisible] = useState(false);
  const [currentApi, setCurrentApi] = useState<Api | null>(null);
  const [searchParams, setSearchParams] = useState({
    keyword: '',
    categoryId: '',
    status: undefined as number | undefined,
    curPage: 1,
    pageSize: 20,
  });

  useEffect(() => {
    fetchCategoryTree();
    fetchApiList(searchParams);
  }, []);

  /**
   * 刷新列表
   */
  const refreshList = () => {
    fetchApiList(searchParams);
  };

  /**
   * 打开新建表单
   */
  const handleAdd = () => {
    setCurrentApi(null);
    setFormVisible(true);
  };

  /**
   * 打开编辑表单
   */
  const handleEdit = (record: Api) => {
    setCurrentApi(record);
    setFormVisible(true);
  };

  /**
   * 查看详情
   */
  const handleView = (record: Api) => {
    // TODO: 跳转到详情页
    console.log('查看详情:', record);
  };

  /**
   * 删除 API
   */
  const handleDelete = async (id: string) => {
    const success = await handleDeleteApi(id);
    if (success) {
      refreshList();
    }
  };

  /**
   * 撤回 API
   */
  const handleWithdraw = async (id: string) => {
    const result = await handleWithdrawApi(id);
    if (result) {
      refreshList();
    }
  };

  /**
   * 表单提交成功
   */
  const handleFormSuccess = () => {
    setFormVisible(false);
    refreshList();
  };

  /**
   * 表格列定义
   */
  const columns: ColumnsType<Api> = [
    {
      title: 'API 名称',
      dataIndex: 'nameCn',
      key: 'nameCn',
      width: 200,
      render: (text: string, record: Api) => (
        <div>
          <div>{text}</div>
          <div style={{ color: '#999', fontSize: 12 }}>{record.nameEn}</div>
        </div>
      ),
    },
    {
      title: '路径',
      dataIndex: 'path',
      key: 'path',
      width: 250,
      render: (path: string, record: Api) => (
        <Space>
          <Tag color={methodColorMap[record.method]}>{record.method}</Tag>
          <span>{path}</span>
        </Space>
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
      render: (scope: string) => <Tag>{scope}</Tag>,
    },
    {
      title: '状态',
      dataIndex: 'status',
      key: 'status',
      width: 100,
      render: (status: number) => {
        const { color, text } = statusMap[status] || statusMap[0];
        return <Badge status={color as any} text={text} />;
      },
    },
    {
      title: '创建时间',
      dataIndex: 'createTime',
      key: 'createTime',
      width: 180,
      render: (time: string) => (time ? new Date(time).toLocaleString() : '-'),
    },
    {
      title: '操作',
      key: 'action',
      width: 200,
      fixed: 'right',
      render: (_, record: Api) => (
        <Space size="small">
          <Button
            type="link"
            size="small"
            icon={<EyeOutlined />}
            onClick={() => handleView(record)}
          >
            详情
          </Button>
          <Button
            type="link"
            size="small"
            icon={<EditOutlined />}
            onClick={() => handleEdit(record)}
          >
            编辑
          </Button>
          {record.status === 1 && (
            <Button
              type="link"
              size="small"
              icon={<UndoOutlined />}
              onClick={() => handleWithdraw(record.id)}
            >
              撤回
            </Button>
          )}
          {record.status !== 2 && (
            <Popconfirm
              title="确定删除该 API 吗？"
              onConfirm={() => handleDelete(record.id)}
              okText="确定"
              cancelText="取消"
            >
              <Button type="link" size="small" danger icon={<DeleteOutlined />}>
                删除
              </Button>
            </Popconfirm>
          )}
        </Space>
      ),
    },
  ];

  /**
   * 分类树扁平化（用于下拉选择）
   */
  const flattenCategories = (categories: any[], level = 0): any[] => {
    return categories.reduce((acc, cat) => {
      const prefix = '—'.repeat(level);
      acc.push({ label: `${prefix}${prefix ? ' ' : ''}${cat.nameCn}`, value: cat.id });
      if (cat.children && cat.children.length > 0) {
        acc.push(...flattenCategories(cat.children, level + 1));
      }
      return acc;
    }, []);
  };

  return (
    <div className={styles.container}>
      <Card
        title="API 管理"
        extra={
          <Button type="primary" icon={<PlusOutlined />} onClick={handleAdd}>
            注册 API
          </Button>
        }
      >
        <div className={styles.toolbar}>
          <Space size="middle">
            <Search
              placeholder="搜索 API 名称、Scope"
              value={searchParams.keyword}
              onChange={(e) =>
                setSearchParams({ ...searchParams, keyword: e.target.value })
              }
              onSearch={() => fetchApiList(searchParams)}
              style={{ width: 300 }}
              allowClear
            />
            <Select
              placeholder="选择分类"
              value={searchParams.categoryId}
              onChange={(value) =>
                setSearchParams({ ...searchParams, categoryId: value, curPage: 1 })
              }
              style={{ width: 200 }}
              allowClear
              options={flattenCategories(categoryTree)}
            />
            <Select
              placeholder="选择状态"
              value={searchParams.status}
              onChange={(value) =>
                setSearchParams({ ...searchParams, status: value, curPage: 1 })
              }
              style={{ width: 150 }}
              allowClear
            >
              <Select.Option value={0}>草稿</Select.Option>
              <Select.Option value={1}>待审</Select.Option>
              <Select.Option value={2}>已发布</Select.Option>
              <Select.Option value={3}>已下线</Select.Option>
            </Select>
            <Button type="primary" onClick={() => fetchApiList(searchParams)}>
              查询
            </Button>
          </Space>
        </div>

        <Table
          columns={columns}
          dataSource={apiList}
          rowKey="id"
          loading={loading}
          pagination={{
            current: searchParams.curPage,
            pageSize: searchParams.pageSize,
            total,
            showSizeChanger: true,
            showTotal: (total) => `共 ${total} 条`,
            onChange: (curPage, pageSize) => {
              setSearchParams({ ...searchParams, curPage, pageSize });
              fetchApiList({ ...searchParams, curPage, pageSize });
            },
          }}
          scroll={{ x: 1400 }}
        />
      </Card>

      {/* API 表单 */}
      <ApiForm
        visible={formVisible}
        api={currentApi}
        categoryTree={categoryTree}
        onSuccess={handleFormSuccess}
        onCancel={() => setFormVisible(false)}
      />
    </div>
  );
};

export default ApiList;
