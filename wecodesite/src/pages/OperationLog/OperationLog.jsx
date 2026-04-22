import React, { useState, useEffect } from 'react';
import { Form, Input, Select, DatePicker, Button, Table, Pagination } from 'antd';
import { SearchOutlined, ReloadOutlined } from '@ant-design/icons';
import { fetchOperationLogList, fetchOperationLogFilters } from './thunk';
import './OperationLog.m.less';
const { RangePicker } = DatePicker;
const { Option } = Select;

function OperationLog() {
  const [form] = Form.useForm();
  const [loading, setLoading] = useState(false);
  const [data, setData] = useState([]);
  const [currentPage, setCurrentPage] = useState(1);
  const [pageSize, setPageSize] = useState(10);
  const [total, setTotal] = useState(0);
  const [operationTypes, setOperationTypes] = useState([]);
  const [operationObjects, setOperationObjects] = useState([]);

  useEffect(() => {
    loadFilters();
    loadData();
  }, [currentPage, pageSize]);

  const loadFilters = async () => {
    const filters = await fetchOperationLogFilters();
    setOperationTypes(filters.operationTypes);
    setOperationObjects(filters.operationObjects);
  };

  const loadData = async (values = {}) => {
    setLoading(true);
    const result = await fetchOperationLogList({
      page: currentPage,
      pageSize,
      filters: values,
    });
    setData(result.data);
    setTotal(result.total);
    setLoading(false);
  };

  const handleSearch = async (values) => {
    setCurrentPage(1);
    await loadData(values);
  };

  const handleReset = () => {
    form.resetFields();
    setCurrentPage(1);
    loadData({});
  };

  const handlePageChange = (page, size) => {
    setCurrentPage(page);
    setPageSize(size);
  };

  const columns = [
    {
      title: '操作账号',
      dataIndex: 'account',
      key: 'account',
    },
    {
      title: '操作类型',
      dataIndex: 'operationType',
      key: 'operationType',
      render: (text) => {
        const type = operationTypes.find((t) => t.value === text);
        return type ? type.label : text;
      },
    },
    {
      title: '操作对象',
      dataIndex: 'operationObject',
      key: 'operationObject',
      render: (text) => {
        const obj = operationObjects.find((o) => o.value === text);
        return obj ? obj.label : text;
      },
    },
    {
      title: '操作描述',
      dataIndex: 'description',
      key: 'description',
    },
    {
      title: '操作IP',
      dataIndex: 'ip',
      key: 'ip',
    },
    {
      title: '操作时间',
      dataIndex: 'time',
      key: 'time',
    },
    {
      title: '操作结果',
      dataIndex: 'result',
      key: 'result',
      render: (text) => (
        <span className={text === '成功' ? 'result-success' : 'result-fail'}>{text}</span>
      ),
    },
  ];

  return (
    <div className="operation-log">
      <div className="page-header">
        <div className="page-header-left">
          <div className="page-title">操作日志</div>
          <span className="page-desc">查看用户操作记录</span>
        </div>
      </div>

      <div className="search-area">
        <Form form={form} layout="horizontal" onFinish={handleSearch}>
          <div className="search-form-row">
            <div className="search-form-left">
              <Form.Item name="account" label="操作账号" style={{ marginBottom: 8 }}>
                <Input placeholder="请输入操作账号" style={{ width: 140 }} />
              </Form.Item>
              <Form.Item name="operationObject" label="操作对象" style={{ marginBottom: 8 }}>
                <Select placeholder="请选择" style={{ width: 140 }} allowClear>
                  {operationObjects.map((obj) => (
                    <Option key={obj.value} value={obj.value}>{obj.label}</Option>
                  ))}
                </Select>
              </Form.Item>
              <Form.Item name="operationTime" label="操作时间" style={{ marginBottom: 8 }}>
                <RangePicker showTime format="YYYY-MM-DD HH:mm:ss" style={{ width: 240 }} />
              </Form.Item>
            </div>
            <div className="search-form-right">
              <Button icon={<ReloadOutlined />} onClick={handleReset}>
                重置
              </Button>
              <Button type="primary" icon={<SearchOutlined />} htmlType="submit">
                查询
              </Button>
            </div>
          </div>
        </Form>
      </div>

      <div className="table-area">
        <Table
          columns={columns}
          dataSource={data}
          rowKey="key"
          pagination={false}
          loading={loading}
        />
      </div>

      <div className="pagination-area">
        <div className="pagination-info">最近180日数据</div>
        <Pagination
          total={total}
          current={currentPage}
          pageSize={pageSize}
          pageSizeOptions={[10, 20, 50]}
          showSizeChanger
          showQuickJumper
          onChange={handlePageChange}
        />
      </div>
    </div>
  );
}

export default OperationLog;