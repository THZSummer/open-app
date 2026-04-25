import React, { useState, useEffect } from 'react';
import {
  Button,
  Table,
  Input,
  Select,
  TreeSelect,
  Empty,
  Spin,
  Pagination,
  message,
} from 'antd';
import {
  PlusOutlined,
} from '@ant-design/icons';
import { fetchEventList, deleteEvent } from './thunk';
import { fetchCategoryTree } from '../Category/thunk';
import EventRegister from './EventRegister';
import { getEventListColumns } from './constants';
import { INIT_PAGECONFIG } from '../../../utils/constants';
import './EventList.m.less';

const { Search } = Input;

function EventList() {
  const [loading, setLoading] = useState(false);
  const [eventList, setEventList] = useState([]);
  const [pagination, setPagination] = useState(INIT_PAGECONFIG);
  const [keyword, setKeyword] = useState('');
  const [categoryId, setCategoryId] = useState(undefined);
  const [status, setStatus] = useState(undefined);
  const [modalVisible, setModalVisible] = useState(false);
  const [currentEvent, setCurrentEvent] = useState(null);
  const [mode, setMode] = useState('create');
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

    const result = await fetchEventList(filteredParams);
    if (result.code === '200') {
      setEventList(result.data);
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
    setCurrentEvent(null);
    setMode('create');
    setModalVisible(true);
  };

  const handleEdit = (record) => {
    setCurrentEvent({ id: record.id });
    setMode('edit');
    setModalVisible(true);
  };

  const handleView = (record) => {
    setCurrentEvent({ id: record.id });
    setMode('view');
    setModalVisible(true);
  };

  const handleSuccess = () => {
    setModalVisible(false);
    loadData();
  };

  const handleDelete = async (id) => {
    const res = await deleteEvent(id);
    if (res && res.code === '200') {
      message.success('删除成功');
      loadData();
    } else {
      message.error(res?.message || '删除失败');
    }
  };

  const columns = getEventListColumns({
    handleView,
    handleEdit,
    handleDelete,
  });

  return (
    <div className="event-list">
      <div className="page-header">
        <div className="page-header-left">
          <h4 className="page-title">事件管理</h4>
          <span className="page-desc">管理事件定义，配置事件订阅</span>
        </div>
        <Button type="primary" icon={<PlusOutlined />} onClick={handleAdd}>
          注册事件
        </Button>
      </div>

      <div className="toolbar">
        <Search
          placeholder="搜索事件名称"
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
        {eventList.length > 0 ? (
          <>
            <Table
              columns={columns}
              dataSource={eventList}
              rowKey="id"
              pagination={false}
            />
            <div style={{ marginTop: 16, textAlign: 'right' }}>
              <Pagination
                total={pagination.total}
                current={pagination.curPage}
                pageSize={pagination.pageSize}
                pageSizeOptions={[10, 20, 50]}
                showSizeChanger
                showQuickJumper
                showTotal={(total) => `共 ${pagination.total} 条`}
                onChange={handlePageChange}
              />
            </div>
          </>
        ) : (
          <Empty description="暂无事件数据" />
        )}
      </Spin>

      <EventRegister
        visible={modalVisible}
        event={currentEvent}
        mode={mode}
        onSuccess={handleSuccess}
        onCancel={() => setModalVisible(false)}
      />
    </div>
  );
}

export default EventList;
