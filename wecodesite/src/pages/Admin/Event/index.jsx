import React, { useEffect } from 'react';
import { Button, Table, Spin, Empty, Pagination } from 'antd';
import { useNavigate } from 'react-router-dom';
import { useAdminList } from '../../../hooks/useAdminList';
import AdminTableToolbar from '../../../components/AdminTableToolbar/AdminTableToolbar';
import { fetchEventList, deleteEvent } from './thunk';
import { fetchCategoryTree } from '../Category/thunk';
import EventRegister from './EventRegister';
import { getEventListColumns } from './constants';
import { PAGE_SIZE_OPTIONS } from '../../../utils/constants';
import { isInAdminWhitelist } from '../../../utils/common';
import SimpleSidebar from '../../../components/SimpleSidebar/SimpleSidebar';
import './EventList.m.less';

function EventList() {
  const navigate = useNavigate();

  useEffect(() => {
    if (!isInAdminWhitelist()) {
      navigate('/apps');
    }
  }, [navigate]);

  const {
    data: eventList,
    loading,
    pagination,
    keyword,
    categoryId,
    status,
    categories,
    modalVisible,
    currentItem,
    mode,
    setKeyword,
    loadData,
    loadCategories,
    handleSearch,
    handlePageChange,
    handleCategoryChange,
    handleStatusChange,
    handleAdd,
    handleEdit,
    handleView,
    handleDelete,
    handleSuccess,
    closeModal,
  } = useAdminList({
    fetchList: fetchEventList,
    fetchCategories: fetchCategoryTree,
    deleteItem: deleteEvent,
  });

  useEffect(() => {
    loadCategories();
    loadData();
  }, [loadCategories, loadData]);

  const columns = getEventListColumns({
    handleView,
    handleEdit,
    handleDelete,
  });

  return (
    <div style={{ display: 'flex', height: '100vh', overflow: 'hidden' }}>
      <SimpleSidebar />
      <div style={{ flex: 1, overflow: 'auto' }}>
        <div className="event-list">
          <div className="page-header">
            <div className="page-header-left">
              <h4 className="page-title">事件管理</h4>
              <span className="page-desc">管理事件定义，配置事件订阅</span>
            </div>
            <Button type="primary" onClick={handleAdd} style={{ justifyContent: 'center', borderRadius: 6 }}>
              注册事件
            </Button>
          </div>

          <AdminTableToolbar
            keyword={keyword}
            onKeywordChange={setKeyword}
            onSearch={handleSearch}
            placeholder="搜索事件名称"
            categoryId={categoryId}
            categories={categories}
            onCategoryChange={handleCategoryChange}
            status={status}
            onStatusChange={handleStatusChange}
          />

          <Spin spinning={loading}>
            {eventList.length > 0 ? (
              <>
                <div className="table-wrapper">
                  <Table
                    columns={columns}
                    dataSource={eventList}
                    rowKey="id"
                    pagination={false}
                  />
                </div>
                <div style={{ marginTop: 16, textAlign: 'right' }}>
                  <Pagination
                    total={pagination.total}
                    current={pagination.curPage}
                    pageSize={pagination.pageSize}
                    pageSizeOptions={PAGE_SIZE_OPTIONS}
                    showSizeChanger
                    showQuickJumper
                    showTotal={(total) => `共 ${total} 条`}
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
            event={currentItem}
            mode={mode}
            onSuccess={handleSuccess}
            onCancel={closeModal}
          />
        </div>
      </div>
    </div>
  );
}

export default EventList;
