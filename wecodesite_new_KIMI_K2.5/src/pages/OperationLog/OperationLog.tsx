import React, { useState, useEffect } from 'react';
import { Card, Table, Input, Select, DatePicker, Button, Tag, Pagination } from 'antd';
import { SearchOutlined, ReloadOutlined } from '@ant-design/icons';
import { fetchOperationLogList, fetchOperationLogFilters } from './thunk';
import styles from './OperationLog.module.less';

const { RangePicker } = DatePicker;

const OperationLog: React.FC = () => {
  const [logs, setLogs] = useState<any[]>([]);
  const [loading, setLoading] = useState(false);
  const [currentPage, setCurrentPage] = useState(1);
  const [pageSize, setPageSize] = useState(10);
  const [total, setTotal] = useState(0);
  const [filters, setFilters] = useState({
    account: '',
    object: '全部',
    timeRange: null as any,
  });
  const [filterOptions, setFilterOptions] = useState({
    operationTypes: [] as string[],
    operationObjects: [] as string[],
  });

  useEffect(() => {
    loadLogs();
    loadFilterOptions();
  }, [currentPage, pageSize]);

  const loadLogs = async () => {
    setLoading(true);
    const result = await fetchOperationLogList({
      account: filters.account,
      object: filters.object,
      page: currentPage,
      pageSize,
    });
    setLogs(result.list);
    setTotal(result.total);
    setLoading(false);
  };

  const loadFilterOptions = async () => {
    const options = await fetchOperationLogFilters();
    setFilterOptions(options);
  };

  const handleSearch = () => {
    setCurrentPage(1);
    loadLogs();
  };

  const handleReset = () => {
    setFilters({ account: '', object: '全部', timeRange: null });
    setCurrentPage(1);
    loadLogs();
  };

  const columns = [
    { title: '操作账号', dataIndex: 'account', key: 'account' },
    { title: '操作类型', dataIndex: 'operationType', key: 'operationType' },
    { title: '操作对象', dataIndex: 'object', key: 'object' },
    { title: '操作描述', dataIndex: 'description', key: 'description' },
    { title: '操作IP', dataIndex: 'ip', key: 'ip' },
    {
      title: '结果',
      dataIndex: 'result',
      key: 'result',
      render: (result: string) => (
        <Tag color={result === '成功' ? 'success' : 'error'}>{result}</Tag>
      ),
    },
    { title: '操作时间', dataIndex: 'time', key: 'time' },
  ];

  return (
    <div className={styles.container}>
      <h2 className={styles.title}>操作日志</h2>
      <p className={styles.desc}>查看用户操作记录</p>

      <Card className={styles.filterCard}>
        <div className={styles.filterRow}>
          <Input
            placeholder="操作账号"
            value={filters.account}
            onChange={(e) => setFilters({ ...filters, account: e.target.value })}
            style={{ width: 150 }}
          />
          <Select
            placeholder="操作对象"
            value={filters.object}
            onChange={(value) => setFilters({ ...filters, object: value })}
            style={{ width: 150 }}
          >
            {filterOptions.operationObjects.map((obj) => (
              <Select.Option key={obj} value={obj}>{obj}</Select.Option>
            ))}
          </Select>
          <RangePicker
            placeholder={['开始时间', '结束时间']}
            value={filters.timeRange}
            onChange={(dates) => setFilters({ ...filters, timeRange: dates })}
          />
          <Button icon={<ReloadOutlined />} onClick={handleReset}>重置</Button>
          <Button type="primary" icon={<SearchOutlined />} onClick={handleSearch}>查询</Button>
        </div>
      </Card>

      <Card>
        <div className={styles.tableHint}>最近180日数据</div>
        <Table
          columns={columns}
          dataSource={logs}
          loading={loading}
          rowKey="id"
          pagination={false}
          size="small"
        />
        <div className={styles.pagination}>
          <Pagination
            total={total}
            current={currentPage}
            pageSize={pageSize}
            pageSizeOptions={[10, 20, 50]}
            showSizeChanger
            showQuickJumper
            showTotal={(t) => `共 ${t} 条`}
            onChange={(page, size) => {
              setCurrentPage(page);
              setPageSize(size || 10);
            }}
          />
        </div>
      </Card>
    </div>
  );
};

export default OperationLog;
