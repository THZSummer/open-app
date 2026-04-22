import React, { useState, useEffect } from 'react';
import { Drawer, Table, Button, Pagination, Tag } from 'antd';
import { fetchAllEvents } from './thunk';
import './EventDrawer.m.less';

const PAGE_SIZE_OPTIONS = [10, 20, 50];

function EventDrawer({ open, onClose, onConfirm, selectedEvents = [] }) {
  const [selectedRowKeys, setSelectedRowKeys] = useState(
    selectedEvents.map(e => e.id)
  );
  const [currentPage, setCurrentPage] = useState(1);
  const [pageSize, setPageSize] = useState(10);
  const [allEvents, setAllEvents] = useState([]);
  const [loading, setLoading] = useState(false);

  useEffect(() => {
    const loadData = async () => {
      setLoading(true);
      const data = await fetchAllEvents();
      setAllEvents(data);
      setLoading(false);
    };
    if (open) {
      loadData();
    }
  }, [open]);

  const handlePageChange = (page, size) => {
    setCurrentPage(page);
    setPageSize(size);
  };

  const handleSelectChange = (keys) => {
    setSelectedRowKeys(keys);
  };

  const handleConfirm = () => {
    const selected = allEvents.filter(event => 
      selectedRowKeys.includes(event.id)
    );
    onConfirm(selected);
    setSelectedRowKeys([]);
    setCurrentPage(1);
    onClose();
  };

  const columns = [
    {
      title: '事件名称',
      dataIndex: 'nameCn',
      key: 'nameCn',
      render: (text, record) => {
        const name = record.nameCn || record.name || '-';
        return (
          <div>
            <div>{name}</div>
            <span style={{ fontSize: 12, color: '#8c8c8c' }}>{record.topic}</span>
          </div>
        );
      },
    },
    {
      title: '是否需要审核',
      dataIndex: 'needApproval',
      key: 'needApproval',
      render: (needApproval, record) => {
        const val = needApproval !== undefined ? needApproval : record.needReview;
        return val ? 
          <Tag color="orange">需要审核</Tag> : 
          <Tag color="green">无需审核</Tag>;
      },
    },
    {
      title: '操作',
      key: 'action',
      render: (_, record) => {
        const docUrl = record.event?.docUrl || record.docUrl;
        return (
          <Button type="link" size="small" onClick={() => window.open(docUrl, '_blank')}>
            查看文档
          </Button>
        );
      },
    },
  ];

  const rowSelection = {
    selectedRowKeys,
    onChange: handleSelectChange,
  };

  const paginatedData = allEvents.slice(
    (currentPage - 1) * pageSize,
    currentPage * pageSize
  );

  return (
    <Drawer
      title="添加事件"
      placement="right"
      width={600}
      onClose={onClose}
      open={open}
      className="event-drawer"
      footer={
        <div className="drawer-footer">
          <Button onClick={onClose}>取消</Button>
          <Button 
            type="primary" 
            disabled={selectedRowKeys.length === 0}
            onClick={handleConfirm}
          >
            确认添加
          </Button>
        </div>
      }
    >
      <Table
        rowSelection={rowSelection}
        columns={columns}
        dataSource={paginatedData}
        rowKey="id"
        pagination={false}
        loading={loading}
      />
      <div className="drawer-pagination">
        <span className="pagination-total">共 {allEvents.length} 条</span>
        <Pagination
          current={currentPage}
          pageSize={pageSize}
          total={allEvents.length}
          onChange={handlePageChange}
          showSizeChanger
          pageSizeOptions={PAGE_SIZE_OPTIONS}
          showQuickJumper
        />
      </div>
    </Drawer>
  );
}

export default EventDrawer;