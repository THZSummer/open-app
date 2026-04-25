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
import { fetchCallbackList, deleteCallback } from './thunk';
import { fetchCategoryTree } from '../Category/thunk';
import CallbackRegister from './CallbackRegister';
import { getCallbackListColumns } from './constants';
import { INIT_PAGECONFIG, PAGE_SIZE_OPTIONS } from '../../../utils/constants';
import './CallbackList.m.less';

const { Search } = Input;

function CallbackList() {
  const [loading, setLoading] = useState(false);
  const [callbackList, setCallbackList] = useState([]);
  const [pagination, setPagination] = useState(INIT_PAGECONFIG);
  const [keyword, setKeyword] = useState('');
  const [categoryId, setCategoryId] = useState(undefined);
  const [status, setStatus] = useState(undefined);
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

    const result = await fetchCallbackList(filteredParams);
    if (result.code === '200') {
      setCallbackList(result.data);
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
    const res = await deleteCallback(id);
    if (res && res.code === '200') {
      message.success('删除成功');
      loadData();
    } else {
      message.error(res?.message || '删除失败');
    }
  };

  const columns = getCallbackListColumns({
    handleView,
    handleEdit,
    handleDelete,
  });

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
          <Select.Option value={1}>待审</Select.Option>
          <Select.Option value={2}>已发布</Select.Option>
          <Select.Option value={3}>已下线</Select.Option>
        </Select>
      </div>

      <Spin spinning={loading}>
        {callbackList.length > 0 ? (
          <>
            <Table
              columns={columns}
              dataSource={callbackList}
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
