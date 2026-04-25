import React, { useState, useEffect } from 'react';
import {
  Button,
  Table,
  Input,
  Select,
  TreeSelect,
  Empty,
  Spin,
  message,
  Pagination,
} from 'antd';
import {
  PlusOutlined,
} from '@ant-design/icons';
import { fetchApiList, deleteApi } from './thunk';
import { fetchCategoryTree } from '../Category/thunk';
import ApiRegister from './ApiRegister';
import { getApiListColumns } from './constants';
import { INIT_PAGECONFIG, PAGE_SIZE_OPTIONS } from '../../../utils/constants';
import './ApiList.m.less';

const { Search } = Input;

function ApiList() {
  const [loading, setLoading] = useState(false);
  const [apiList, setApiList] = useState([]);
  const [pagination, setPagination] = useState(INIT_PAGECONFIG);
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
    const finalKeyword = 'keyword' in params ? params.keyword : keyword;
    const finalCategoryId = 'categoryId' in params ? params.categoryId : categoryId;
    const finalStatus = 'status' in params ? params.status : status;
    const finalPage = 'curPage' in params ? params.curPage : pagination.curPage;
    const finalSize = 'pageSize' in params ? params.pageSize : pagination.pageSize;

    const requestParams = {
      keyword: finalKeyword,
      categoryId: finalCategoryId,
      status: finalStatus,
      curPage: finalPage,
      pageSize: finalSize,
    };

    const filteredParams = Object.fromEntries(
      Object.entries(requestParams).filter(([_, value]) => value !== undefined)
    );

    const result = await fetchApiList(filteredParams);
    if (result.code === '200') {
      setApiList(result.data);
      setPagination(prev => ({ ...prev, total: result.page?.total || 0, curPage: finalPage, pageSize: finalSize }));
    }
    setLoading(false);
  };

  const handleSearch = () => {
    loadData({ curPage: 1 });
  };

  const handlePageChange = (page, size) => {
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
    const res = await deleteApi(id);
    if (res && res.code === '200') {
      message.success('删除成功');
      loadData();
    } else {
      message.error(res?.message || '删除失败');
    }
  };

  const handleSuccess = () => {
    setModalVisible(false);
    loadData();
  };

  const columns = getApiListColumns({
    handleView,
    handleEdit,
    handleDelete,
  });

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
                total={pagination.total}
                current={pagination.curPage}
                pageSize={pagination.pageSize}
                pageSizeOptions={PAGE_SIZE_OPTIONS}
                showSizeChanger
                showQuickJumper
                showTotal={(total) => `共 ${pagination.total} 条`}
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
