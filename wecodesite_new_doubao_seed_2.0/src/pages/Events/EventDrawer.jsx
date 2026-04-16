import React, { useState } from 'react';
import { Drawer, Checkbox, Button, Table, Pagination, message } from 'antd';
import './EventDrawer.m.less';
import mockData from './mock';

const EventDrawer = ({ visible, onClose, onConfirm }) => {
  const [selectedEvents, setSelectedEvents] = useState([]);
  const [currentPage, setCurrentPage] = useState(1);
  const [pageSize, setPageSize] = useState(10);

  const handleEventCheck = (eventId, checked) => {
    if (checked) {
      setSelectedEvents([...selectedEvents, eventId]);
    } else {
      setSelectedEvents(selectedEvents.filter(id => id !== eventId));
    }
  };

  const handleConfirm = () => {
    if (selectedEvents.length === 0) {
      message.error('请选择要添加的事件');
      return;
    }
    // 获取选中的事件详情
    const selectedEventDetails = mockData.availableEvents.filter(event => 
      selectedEvents.includes(event.id)
    );
    // 为每个事件生成新的ID
    const newEvents = selectedEventDetails.map(event => ({
      ...event,
      id: Date.now().toString() + Math.random().toString(36).substr(2, 9)
    }));
    onConfirm(newEvents);
  };

  // 分页处理
  const startIndex = (currentPage - 1) * pageSize;
  const endIndex = startIndex + pageSize;
  const paginatedEvents = mockData.availableEvents.slice(startIndex, endIndex);

  const columns = [
    {
      title: (
        <Checkbox
          indeterminate={selectedEvents.length > 0 && selectedEvents.length < mockData.availableEvents.length}
          checked={selectedEvents.length === mockData.availableEvents.length && mockData.availableEvents.length > 0}
          onChange={(e) => {
            if (e.target.checked) {
              setSelectedEvents(mockData.availableEvents.map(event => event.id));
            } else {
              setSelectedEvents([]);
            }
          }}
        />
      ),
      dataIndex: 'checkbox',
      key: 'checkbox',
      render: (_, record) => (
        <Checkbox
          checked={selectedEvents.includes(record.id)}
          onChange={(e) => handleEventCheck(record.id, e.target.checked)}
        />
      )
    },
    {
      title: '事件名称',
      dataIndex: 'name',
      key: 'name'
    },
    {
      title: '事件类型',
      dataIndex: 'type',
      key: 'type'
    },
    {
      title: '所需权限',
      dataIndex: 'permission',
      key: 'permission'
    }
  ];

  return (
    <Drawer
      title="添加事件"
      placement="right"
      width={800}
      open={visible}
      onClose={onClose}
      footer={(
        <div className="drawerFooter">
          <Button onClick={onClose}>取消</Button>
          <Button type="primary" onClick={handleConfirm}>
            确认添加
          </Button>
        </div>
      )}
    >
      <div className="drawerContent">
        <Table
          columns={columns}
          dataSource={paginatedEvents}
          rowKey="id"
          pagination={false}
          size="small"
        />
        <div className="pagination">
          <span className="total">
            共 {mockData.availableEvents.length} 条
          </span>
          <Pagination
            current={currentPage}
            pageSize={pageSize}
            total={mockData.availableEvents.length}
            onChange={(page, size) => {
              setCurrentPage(page);
              setPageSize(size);
            }}
            showSizeChanger
            pageSizeOptions={['10', '20', '50']}
          />
        </div>
      </div>
    </Drawer>
  );
};

export default EventDrawer;