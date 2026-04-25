import React, { useState, useEffect } from 'react';
import { Drawer, Table, Button, Pagination, Tag, message } from 'antd';
import { fetchAllCallbacks } from './thunk';
import { PAGE_SIZE_OPTIONS, INIT_PAGECONFIG } from '../../utils/constants';
import { openUrl } from '../../utils/common';
import { getCallbackDrawerColumns } from './constants';
import './CallbackDrawer.m.less';

function CallbackDrawer({ open, onClose, onConfirm, selectedCallbacks = [], subscribeLoading = false, appId }) {
  const [selectedRowKeys, setSelectedRowKeys] = useState(
    selectedCallbacks.map(c => c.id)
  );
  const [pagination, setPagination] = useState(INIT_PAGECONFIG);
  const [allCallbacks, setAllCallbacks] = useState([]);
  const [loading, setLoading] = useState(false);

  /**
   * 加载回调列表
   */
  const loadData = async (page = pagination.curPage, size = pagination.pageSize) => {
    setLoading(true);
    try {
      const result = await fetchAllCallbacks({ curPage: page, pageSize: size, appId });
      if (result && result.code === '200') {
        setAllCallbacks(result.data || []);
        setPagination(prev => ({
          ...prev,
          curPage: page,
          pageSize: size,
          total: result.page?.total || 0
        }));
      } else {
        message.error(result?.message || '加载回调列表失败');
      }
    } finally {
      setLoading(false);
    }
  };

  /**
   * 抽屉打开时加载数据
   */
  useEffect(() => {
    if (open) {
      loadData();
    }
  }, [open]);

  const handlePageChange = async (page, size) => {
    await loadData(page, size);
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
    setPagination(INIT_PAGECONFIG);
    onClose();
  };

  const renderCallbackName = (text, record) => {
    const name = record.nameCn || record.name || '-';
    return (
      <div>
        <div>{name}</div>
        <span style={{ fontSize: 12, color: '#8c8c8c' }}>{record.scope}</span>
      </div>
    );
  };

  const renderNeedApproval = (needApproval, record) => {
    const val = needApproval !== undefined ? needApproval : record.needReview;
    return val ?
      <Tag color="orange">需要审核</Tag> :
      <Tag color="green">无需审核</Tag>;
  };

  const renderIsSubScribed = (isSubscribed) => {
    if (isSubscribed === 1) {
      return <Tag color="success">已订阅</Tag>;
    }
    return <Tag color="default">未订阅</Tag>;
  }

  const renderAction = (_, record) => {
    const docUrl = record.callback?.docUrl || record.docUrl;
    return (
      <Button type="link" size="small" onClick={() => openUrl(docUrl)}>
        查看文档
      </Button>
    );
  };

  const columns = getCallbackDrawerColumns({
    renderCallbackName,
    renderNeedApproval,
    renderIsSubScribed,
    renderAction,
  });

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
        <span className="pagination-total">共 {pagination.total} 条</span>
        <Pagination
          current={pagination.curPage}
          pageSize={pagination.pageSize}
          total={pagination.total}
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
