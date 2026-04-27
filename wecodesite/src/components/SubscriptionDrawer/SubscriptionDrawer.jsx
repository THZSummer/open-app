import React, { useState, useEffect } from 'react';
import { Drawer, Table, Button, Input } from 'antd';
import './SubscriptionDrawer.less';

const { Search } = Input;

const SubscriptionDrawer = ({
  open,
  onClose,
  onConfirm,
  title = '添加订阅',
  columns,
  dataSource,
  loading,
  selectedRowKeys = [],
  fetchData,
  confirmText = '确认订阅',
  confirmLoading = false,
}) => {
  const [keyword, setKeyword] = useState('');
  const [selectedKeys, setSelectedKeys] = useState(selectedRowKeys);

  useEffect(() => {
    if (open && fetchData) {
      fetchData(keyword);
    }
  }, [open, keyword, fetchData]);

  useEffect(() => {
    setSelectedKeys(selectedRowKeys);
  }, [selectedRowKeys]);

  const handleSearch = (value) => {
    setKeyword(value);
    if (fetchData) {
      fetchData(value);
    }
  };

  const handleConfirm = () => {
    const selectedItems = dataSource.filter(item => selectedKeys.includes(item.id));
    onConfirm(selectedItems);
  };

  const handleClose = () => {
    setKeyword('');
    setSelectedKeys([]);
    onClose();
  };

  return (
    <Drawer
      title={title}
      placement="right"
      width={600}
      open={open}
      onClose={handleClose}
      footer={
        <div className="drawer-footer">
          <Button onClick={handleClose}>取消</Button>
          <Button
            type="primary"
            onClick={handleConfirm}
            loading={confirmLoading}
            disabled={selectedKeys.length === 0}
          >
            {confirmText} ({selectedKeys.length})
          </Button>
        </div>
      }
    >
      <div className="drawer-content">
        <div className="search-bar">
          <Search
            placeholder="搜索名称或标识"
            onSearch={handleSearch}
            style={{ width: 200 }}
            allowClear
          />
        </div>

        <Table
          columns={columns}
          dataSource={dataSource}
          rowKey="id"
          pagination={{ pageSize: 10 }}
          loading={loading}
          rowSelection={{
            selectedRowKeys: selectedKeys,
            onChange: (keys) => {
              setSelectedKeys(keys);
            },
          }}
          size="small"
        />
      </div>
    </Drawer>
  );
};

export default SubscriptionDrawer;
