import React, { useState, useEffect, useCallback } from 'react';
import { Drawer, Table, Button, Pagination, Tag } from 'antd';
import { fetchAllCallbacks } from './thunk';
import './CallbackDrawer.m.less';

const PAGE_SIZE_OPTIONS = [10, 20, 50];

function CallbackDrawer({ open, onClose, onConfirm, selectedCallbacks = [], subscribeLoading = false, appId }) {
  const [selectedRowKeys, setSelectedRowKeys] = useState(
    selectedCallbacks.map(c => c.id)
  );
  const [currentPage, setCurrentPage] = useState(1);
  const [pageSize, setPageSize] = useState(10);
  const [allCallbacks, setAllCallbacks] = useState([]);
  const [total, setTotal] = useState(0);
  const [loading, setLoading] = useState(false);

  const loadData = useCallback(async (page = currentPage, size = pageSize) => {
    setLoading(true);
    try {
      const result = await fetchAllCallbacks({ curPage: page, pageSize: size, appId });
      setAllCallbacks(result.data || []);
      setTotal(result.page?.total || 0);
    } finally {
      setLoading(false);
    }
  }, [currentPage, pageSize, appId]);

  useEffect(() => {
    if (open) {
      loadData();
    }
  }, [open, loadData]);

  const handlePageChange = (page, size) => {
    setCurrentPage(page);
    setPageSize(size);
    loadData(page, size);
  };

  const handleSelectChange = (keys) => {
    setSelectedRowKeys(keys);
  };

  const handleConfirm = () => {
    const selected = allCallbacks.filter(callback => 
      selectedRowKeys.includes(callback.id)
    );
    onConfirm(selected);
    setSelectedRowKeys([]);
    setCurrentPage(1);
    onClose();
  };

  const columns = [
    {
      title: '回调名称',
      dataIndex: 'nameCn',
      key: 'nameCn',
      render: (text, record) => {
        const name = record.nameCn || record.name || '-';
        return (
          <div>
            <div>{name}</div>
            <span style={{ fontSize: 12, color: '#8c8c8c' }}>{record.scope}</span>
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
      title: '订阅状态',
      dataIndex: 'isSubscribed',
      key: 'isSubscribed',
      width: 100,
      render: (isSubscribed) => {
        if (isSubscribed === 1) {
          return <Tag color="success">已订阅</Tag>;
        }
        return <Tag color="default">未订阅</Tag>;
      },
    },
    {
      title: '操作',
      key: 'action',
      render: (_, record) => {
        const docUrl = record.callback?.docUrl || record.docUrl;
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
    getCheckboxProps: (record) => ({
      disabled: record.isSubscribed === 1,
    }),
  };

  return (
    <Drawer
      title="添加回调"
      placement="right"
      width={600}
      onClose={onClose}
      open={open}
      className="callback-drawer"
      footer={
        <div className="drawer-footer">
          <Button onClick={onClose}>取消</Button>
          <Button 
            type="primary" 
            disabled={selectedRowKeys.length === 0 || subscribeLoading}
            loading={subscribeLoading}
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
        dataSource={allCallbacks}
        rowKey="id"
        pagination={false}
        loading={loading}
      />
      <div className="drawer-pagination">
        <span className="pagination-total">共 {total} 条</span>
        <Pagination
          current={currentPage}
          pageSize={pageSize}
          total={total}
          onChange={handlePageChange}
          showSizeChanger
          pageSizeOptions={PAGE_SIZE_OPTIONS}
          showQuickJumper
        />
      </div>
    </Drawer>
  );
}

export default CallbackDrawer;
